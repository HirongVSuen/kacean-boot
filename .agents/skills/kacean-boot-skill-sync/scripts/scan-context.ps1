param(
    [switch]$Verbose
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# 只读扫描基础架构、Skill 路由与文档引用，供 Windows / DSH 的 PowerShell 环境使用。
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../../..')).Path

function Section([string]$Name) {
    Write-Output ''
    Write-Output "===== $Name ====="
}

function Get-MainJavaFiles([string]$RootPath) {
    if (-not (Test-Path $RootPath -PathType Container)) {
        return
    }

    Get-ChildItem -Path $RootPath -Recurse -File -Filter '*.java' |
        Where-Object {
            $_.FullName -match '[\\/]src[\\/]main[\\/]java[\\/]' -and
            $_.FullName -notmatch '[\\/]target[\\/]'
        } |
        ForEach-Object {
            $_.FullName.Substring($ProjectRoot.Length + 1).Replace('\', '/')
        } |
        Sort-Object
}

Push-Location $ProjectRoot
try {
    Section '1. 未提交变更'
    $Git = Get-Command git -ErrorAction SilentlyContinue
    if ($null -ne $Git -and (Test-Path '.git')) {
        $GitStatus = @(git status --short)
        if ($LASTEXITCODE -ne 0) {
            throw 'git status 执行失败。'
        }
        Write-Output "[GIT] 未提交路径 $($GitStatus.Count) 个。"
        if ($Verbose) {
            $GitStatus
        }
    } else {
        Write-Output '[GIT] 当前目录不是 Git 仓库或未安装 Git，跳过。'
    }

    Section '2. 通用组件主源码'
    foreach ($Module in @('common/common-web', 'common/common-app', 'common/common-infra', 'common/common-util', 'share')) {
        $Files = @(Get-MainJavaFiles $Module)
        Write-Output "[COMMON] ${Module}: $($Files.Count) 个主源码文件。"
        if ($Verbose) {
            $Files
        }
    }

    Section '3. ArchUnit 全局规则'
    $ArchRuleFile = Get-ChildItem 'common/common-test' -Recurse -File -Filter 'GlobalArchitectureTest.java' -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' } |
        Select-Object -First 1
    if ($null -ne $ArchRuleFile) {
        $RuleCount = @(Select-String -Path $ArchRuleFile.FullName -Pattern '^[ \t]*@ArchTest[ \t]*$').Count
        $RuleFileName = $ArchRuleFile.FullName.Substring($ProjectRoot.Length + 1).Replace('\', '/')
        Write-Output "[ARCH] $RuleFileName"
        Write-Output "[ARCH] @ArchTest 规则数：$RuleCount（构建验证时报告条数应与此一致）。"
    } else {
        Write-Output '[ARCH] 未找到 GlobalArchitectureTest.java，请人工核对 common-test 结构。'
    }

    Section '4. 业务域主源码'
    if (Test-Path 'business' -PathType Container) {
        foreach ($DomainDir in @(Get-ChildItem 'business' -Directory | Sort-Object Name)) {
            $Files = @(Get-MainJavaFiles $DomainDir.FullName)
            $DomainName = $DomainDir.FullName.Substring($ProjectRoot.Length + 1).Replace('\', '/')
            Write-Output "[BUSINESS] ${DomainName}: $($Files.Count) 个主源码文件。"
            if ($Verbose -and $Files.Count -gt 0) {
                $Files
            }
        }
    } else {
        Write-Output '[BUSINESS] business 目录不存在。'
    }

    Section '5. Skill 引用、路由与链接校验'
    $MissingClasses = 0
    $UnregisteredSkills = 0
    $BrokenRoutes = 0
    $BrokenLinks = 0
    $SkillRoot = '.agents/skills'
    $MarkdownFiles = @()
    if (Test-Path $SkillRoot -PathType Container) {
        $SkillBundles = @(Get-ChildItem $SkillRoot -Directory |
            Where-Object { Test-Path (Join-Path $_.FullName 'SKILL.md') -PathType Leaf } |
            Sort-Object Name)
        $SkillNames = @($SkillBundles | ForEach-Object { $_.Name })
        $SkillFiles = @(Get-ChildItem $SkillRoot -Recurse -File -Filter '*.md')
        $MarkdownFiles = @($SkillFiles)

        if (Test-Path 'AGENTS.md' -PathType Leaf) {
            $MarkdownFiles += Get-Item 'AGENTS.md'
            $RouteNames = @(
                Select-String -Path 'AGENTS.md' -Pattern 'kacean-boot-[a-z0-9-]+' -AllMatches |
                    ForEach-Object { $_.Matches } |
                    ForEach-Object { $_.Value } |
                    Sort-Object -Unique
            )
            foreach ($RouteName in $RouteNames) {
                if ($SkillNames -notcontains $RouteName) {
                    Write-Output "[失效路由] $RouteName"
                    $BrokenRoutes++
                }
            }
            foreach ($SkillName in $SkillNames) {
                if ($RouteNames -notcontains $SkillName) {
                    Write-Output "[未注册 Skill] $SkillName"
                    $UnregisteredSkills++
                }
            }
        } else {
            Write-Output '[失效路由] 根 AGENTS.md 不存在。'
            $BrokenRoutes++
        }

        if ($SkillFiles.Count -gt 0) {
            $Pattern = '(com\.kacean\.boot\.[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)*|`(common\.(app|infra|web|util)|infra|web|util)(\.[A-Za-z0-9_]+)+`)'
            $References = @(
                Select-String -Path $SkillFiles.FullName -Pattern $Pattern -AllMatches |
                    ForEach-Object { $_.Matches } |
                    ForEach-Object { $_.Value.Trim([char]96) } |
                    Where-Object { $_ -cmatch '\.[A-Z][A-Za-z0-9_]*$' } |
                    Sort-Object -Unique
            )
            $SourceFiles = @(
                foreach ($TopLevel in @('common', 'business', 'share', 'runner')) {
                    Get-MainJavaFiles $TopLevel
                }
            )

            foreach ($Reference in $References) {
                if ($Reference.StartsWith('com.kacean.boot.')) {
                    $Fqn = $Reference
                } elseif ($Reference -match '^common\.(app|infra|web|util)\.') {
                    $Fqn = "com.kacean.boot.$Reference"
                } elseif ($Reference -match '^(infra|web|util)\.') {
                    $Fqn = "com.kacean.boot.common.$Reference"
                } else {
                    continue
                }

                $RelativeClass = $Fqn.Replace('.', '/') + '.java'
                $Found = $false
                foreach ($SourceFile in $SourceFiles) {
                    if ($SourceFile.EndsWith("/src/main/java/$RelativeClass")) {
                        $Found = $true
                        break
                    }
                }
                if (-not $Found) {
                    Write-Output "[缺失类] $Fqn"
                    $MissingClasses++
                }
            }
        }
    }

    foreach ($MarkdownFile in $MarkdownFiles) {
        $LinkMatches = Select-String -Path $MarkdownFile.FullName -Pattern '\[[^\]]+\]\(([^)]+)\)' -AllMatches
        foreach ($MatchInfo in $LinkMatches) {
            foreach ($Match in $MatchInfo.Matches) {
                $Link = $Match.Groups[1].Value.Trim()
                if ($Link.StartsWith('<') -and $Link.EndsWith('>')) {
                    $Link = $Link.Substring(1, $Link.Length - 2)
                } else {
                    $Link = ($Link -split '\s+')[0]
                }
                if ($Link.StartsWith('#') -or $Link -match '^[A-Za-z][A-Za-z0-9+.-]*:') {
                    continue
                }
                $Target = $Link.Split('#')[0]
                if ($Target -eq '') {
                    continue
                }
                $TargetPath = if ($Target.StartsWith('/')) {
                    $Target
                } else {
                    Join-Path (Split-Path -Parent $MarkdownFile.FullName) $Target
                }
                if (-not (Test-Path $TargetPath)) {
                    $RelativeMarkdown = $MarkdownFile.FullName.Substring($ProjectRoot.Length + 1).Replace('\', '/')
                    Write-Output "[失效链接] $RelativeMarkdown -> $Link"
                    $BrokenLinks++
                }
            }
        }
    }

    $IssueTotal = $MissingClasses + $UnregisteredSkills + $BrokenRoutes + $BrokenLinks
    Write-Output "[CHECK] 缺失类=$MissingClasses；未注册 Skill=$UnregisteredSkills；失效路由=$BrokenRoutes；失效链接=$BrokenLinks。"
    if ($IssueTotal -eq 0) {
        Write-Output '[CHECK] 引用、路由与链接全部通过。'
    } else {
        Write-Output '[CHECK] 请按 kacean-boot-skill-sync 处理文档漂移；代码缺陷只报告，不擅自修改。'
    }

    Section '6. 下一步'
    $DeployModules = @()
    if (Test-Path 'business/deploy' -PathType Container) {
        $DeployModules = @(Get-ChildItem 'business/deploy' -Directory -Filter '*-deploy' | Sort-Object Name)
    }
    if ($DeployModules.Count -gt 0) {
        $DeployTargets = ($DeployModules | ForEach-Object { "business/deploy/$($_.Name)" }) -join ','
        Write-Output "[TARGET] 部署模块：$(($DeployModules | ForEach-Object { $_.Name }) -join '、')。"
        Write-Output "[VERIFY] mvn test -pl $DeployTargets -am test"
    } else {
        Write-Output '[TARGET] 未发现 business/deploy/*-deploy，请人工核对模块结构。'
    }
    Write-Output '[LOAD] 业务任务加载 kacean-boot-business；按任务补充 app / share / naming 等协作 Skill。'
    Write-Output '[SYNC] 路由、链接或公共事实漂移时加载 kacean-boot-skill-sync。'
    Write-Output '[DETAIL] 使用 -Verbose 查看未提交路径与全部主源码清单。'
} finally {
    Pop-Location
}
