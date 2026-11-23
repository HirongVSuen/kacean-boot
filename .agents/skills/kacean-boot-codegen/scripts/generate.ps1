$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir '../../../..')).Path
$ModuleDir = Join-Path $ProjectRoot 'common/common-codegen'
$ClasspathFile = Join-Path $ModuleDir 'target/codegen-classpath.txt'

$Maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -ne $Maven) {
    $MavenCommand = $Maven.Source
} else {
    $MavenWrapper = Join-Path $ProjectRoot 'mvnw.cmd'
    if (-not (Test-Path $MavenWrapper -PathType Leaf)) {
        throw '未找到 mvn 或 mvnw.cmd，无法编译 common-codegen。'
    }
    $MavenCommand = $MavenWrapper
}

$Java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $Java) {
    throw '未找到 java，请使用项目要求的 JDK 21。'
}

& $MavenCommand -q -f (Join-Path $ModuleDir 'pom.xml') compile `
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath `
    "-Dmdep.outputFile=$ClasspathFile" '-DincludeScope=runtime'
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$Dependencies = (Get-Content -Raw $ClasspathFile).Trim()
$Classpath = (Join-Path $ModuleDir 'target/classes') + ';' + $Dependencies
Push-Location $ProjectRoot
try {
    & $Java.Source '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp $Classpath `
        com.kacean.boot.codegen.CodeGeneratorApplication @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
