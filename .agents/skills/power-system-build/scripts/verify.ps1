param(
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Windows / DSH：全量构建 + 分发包冒烟验证 + 自动清理。
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path
$DistDir = Join-Path $ProjectRoot 'runner/standalone-runner/target/dist-verify'
$AppDir = Join-Path $DistDir 'standalone-runner-1.0-SNAPSHOT'
$ZipFile = Join-Path $ProjectRoot 'runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT.zip'
$script:Started = $false
$PreviousServerPort = $env:SERVER_PORT

$Maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -ne $Maven) {
    $MavenCommand = $Maven.Source
} else {
    $MavenWrapper = Join-Path $ProjectRoot 'mvnw.cmd'
    if (-not (Test-Path $MavenWrapper -PathType Leaf)) {
        throw '未找到 mvn 或 mvnw.cmd，无法执行全量构建。'
    }
    $MavenCommand = $MavenWrapper
}

function Remove-VerifyDirectory {
    if (Test-Path $DistDir -PathType Container) {
        $ResolvedDistDir = (Resolve-Path $DistDir).Path
        $ExpectedDistDir = Join-Path $ProjectRoot 'runner/standalone-runner/target/dist-verify'
        if ($ResolvedDistDir -ne $ExpectedDistDir) {
            throw "拒绝清理非预期目录：$ResolvedDistDir"
        }
        Remove-Item $ResolvedDistDir -Recurse -Force
    }
}

function Invoke-VerifyBatch([string]$BatchFile, [string]$Name) {
    $StdoutFile = Join-Path $DistDir "$Name.stdout.log"
    $StderrFile = Join-Path $DistDir "$Name.stderr.log"
    $Process = Start-Process -FilePath $BatchFile -WorkingDirectory $AppDir -PassThru -NoNewWindow `
        -RedirectStandardOutput $StdoutFile -RedirectStandardError $StderrFile
    $BatchDeadline = (Get-Date).AddSeconds(30)
    while (-not $Process.HasExited) {
        if ((Get-Date) -ge $BatchDeadline) {
            throw "批处理 30 秒内未退出：$BatchFile"
        }
        Start-Sleep -Milliseconds 200
    }
    $Process.Refresh()
    if (Test-Path $StdoutFile -PathType Leaf) {
        Get-Content $StdoutFile | Out-Host
    }
    if (Test-Path $StderrFile -PathType Leaf) {
        Get-Content $StderrFile | Out-Host
    }
    # Windows PowerShell 5.1 可能不返回 .bat 的 ExitCode；结果由后续日志、HTTP 与清理断言判定。
}

function Stop-VerifyApplication {
    if ($script:Started) {
        $StopScript = Join-Path $AppDir 'bin/stop.bat'
        if (Test-Path $StopScript -PathType Leaf) {
            Invoke-VerifyBatch $StopScript 'stop'
        }
        $script:Started = $false
    }
}

Push-Location $ProjectRoot
try {
    Write-Output '==> 1/4 全量构建'
    & $MavenCommand clean package -q
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 构建失败，退出码：$LASTEXITCODE"
    }

    Write-Output '==> 2/4 解压分发包'
    Remove-VerifyDirectory
    Expand-Archive -LiteralPath $ZipFile -DestinationPath $DistDir

    Write-Output '==> 3/4 启动并断言'
    $env:SERVER_PORT = "$Port"
    $StartupScript = Join-Path $AppDir 'bin/startup.bat'
    Invoke-VerifyBatch $StartupScript 'startup'
    $script:Started = Test-Path (Join-Path $AppDir 'run/standalone-runner.pid') -PathType Leaf
    Write-Output '启动脚本已返回，等待启动日志。'

    $ApplicationLog = Join-Path $AppDir '.logs/power-system.log'
    $Deadline = (Get-Date).AddSeconds(60)
    $StartedLogFound = $false
    do {
        Start-Sleep -Seconds 1
        if ((Test-Path $ApplicationLog -PathType Leaf) -and
            (Select-String -Path $ApplicationLog -Pattern 'Started PowerSystemApplication' -Quiet)) {
            $StartedLogFound = $true
            break
        }
    } while ((Get-Date) -lt $Deadline)
    if (-not $StartedLogFound) {
        throw "60 秒内未在 $ApplicationLog 中发现 Started PowerSystemApplication。"
    }
    Write-Output '启动日志断言通过。'

    foreach ($Path in @('/', '/index.html', '/swagger-ui/index.html', '/v3/api-docs')) {
        $Uri = "http://localhost:$Port$Path"
        Write-Output "检查 HTTP $Path"
        try {
            $Response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 15
            $StatusCode = [int]$Response.StatusCode
        } catch {
            if ($null -eq $_.Exception.Response) {
                throw "[失败] $Path 请求失败：$($_.Exception.Message)"
            }
            $StatusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($StatusCode -ne 200) {
            throw "[失败] $Path 返回 $StatusCode"
        }
        Write-Output "$Path -> $StatusCode"
    }
    Write-Output '断言通过：启动日志正常，首页、Swagger UI 与 OpenAPI JSON 均可访问，.logs/power-system.log 已生成'

    Write-Output '==> 4/4 停止并清理'
    Stop-VerifyApplication
    Remove-VerifyDirectory
    Write-Output '验证通过'
} finally {
    Stop-VerifyApplication
    Remove-VerifyDirectory
    if ($null -eq $PreviousServerPort) {
        Remove-Item Env:SERVER_PORT -ErrorAction SilentlyContinue
    } else {
        $env:SERVER_PORT = $PreviousServerPort
    }
    Pop-Location
}
