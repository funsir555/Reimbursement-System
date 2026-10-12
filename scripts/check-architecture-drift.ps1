param([switch]$Quarterly)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pomText = Get-Content -LiteralPath (Join-Path $root 'backend/pom.xml') -Raw -Encoding utf8
$package = Get-Content -LiteralPath (Join-Path $root 'frontend/admin-web/package.json') -Raw -Encoding utf8 | ConvertFrom-Json
$spec = Get-Content -LiteralPath (Join-Path $root 'docs/architecture/系统最终目标与开发架构规范.md') -Raw -Encoding utf8

function Assert-DocumentedVersion([string]$name, [string]$actual, [string]$documented) {
    $pattern = '(?m)^\|\s*' + [regex]::Escape($name) + '\s*\|\s*' +
        [regex]::Escape($documented) + '\s*\|'
    if ($spec -notmatch $pattern) {
        throw "$name version drift: manifest=$actual; expected architecture table cell=$documented"
    }
    Write-Output "$name $actual : documented"
}

function Get-PomProperty([string]$name) {
    $match = [regex]::Match($pomText, '<' + [regex]::Escape($name) + '>([^<]+)</' + [regex]::Escape($name) + '>')
    if (-not $match.Success) { throw "Missing POM property: $name" }
    return $match.Groups[1].Value
}

function Get-PackageVersion([string]$name) {
    $value = $package.dependencies.$name
    if (-not $value) { $value = $package.devDependencies.$name }
    if (-not $value) { throw "Missing package: $name" }
    return [string]$value -replace '^[\^~]', ''
}

$java = Get-PomProperty 'maven.compiler.source'
Assert-DocumentedVersion 'Java' $java $java
foreach ($entry in @(
    @('Spring Boot', 'spring-boot.version'),
    @('Spring Cloud', 'spring-cloud.version'),
    @('MyBatis-Plus', 'mybatis-plus.version'),
    @('Lombok', 'lombok.version'),
    @('Hutool', 'hutool.version')
)) {
    $value = Get-PomProperty $entry[1]
    Assert-DocumentedVersion $entry[0] $value $value
}
foreach ($entry in @(
    @('Vue', 'vue', 2),
    @('TypeScript', 'typescript', 2),
    @('Pinia', 'pinia', 1),
    @('Vue Router', 'vue-router', 1),
    @('Element Plus', 'element-plus', 2),
    @('Vite', 'vite', 1),
    @('Vitest', 'vitest', 1)
)) {
    $value = Get-PackageVersion $entry[1]
    $parts = $value.Split('.')
    $documented = ($parts | Select-Object -First $entry[2]) -join '.'
    Assert-DocumentedVersion $entry[0] $value "$documented.x"
}

if ($Quarterly) {
    $reportDir = Join-Path $root 'backend/auth-service/target/surefire-reports'
    $reports = @(Get-ChildItem -LiteralPath $reportDir -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue)
    if ($reports.Count -eq 0) { throw 'Run backend verify before a quarterly test-count review.' }
    $count = 0
    foreach ($file in $reports) {
        $suite = [xml](Get-Content -LiteralPath $file.FullName -Raw)
        $count += [int]$suite.testsuite.tests
    }
    if ($spec -notmatch ('(?m)^\|\s*后端全量测试\s*\|\s*' + $count + '\s*个；')) {
        throw "Architecture current-fact test count has drifted: actual=$count"
    }
    Write-Output "Backend test count $count : documented; review hotspots and governance decisions manually."
}
