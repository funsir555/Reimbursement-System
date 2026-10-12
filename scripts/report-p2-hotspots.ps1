param([int]$Top = 15)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $root
$changes = @{}
git -C $root log --since=90.days --format= --name-only -- backend/auth-service/src/main/java frontend/admin-web/src |
    ForEach-Object {
        $path = $_.Trim().Replace('\', '/')
        if ($path) { $changes[$path] = 1 + [int]$changes[$path] }
    }
$workingChanges = @{}
@(
    git -c core.safecrlf=false -C $root diff --name-only -- backend/auth-service/src/main/java frontend/admin-web/src
    git -C $root ls-files --others --exclude-standard -- backend/auth-service/src/main/java frontend/admin-web/src
) | ForEach-Object {
    $path = $_.Trim().Replace('\', '/')
    if ($path) { $workingChanges[$path] = $true }
}

$items = foreach ($candidate in (rg --files 'backend/auth-service/src/main/java' 'frontend/admin-web/src')) {
    $path = $candidate.Replace('\', '/')
    if ($path -notmatch '\.(java|vue|ts)$' -or $path -match '/__tests__/|\.test\.ts$') { continue }
    $fullPath = Join-Path $root $path
    $content = Get-Content -LiteralPath $fullPath -Raw -Encoding utf8
    $lineCount = ($content -split "`n").Count
    $dependencyAreas = if ($path.EndsWith('.java')) {
        [regex]::Matches($content, 'import com\.finex\.auth\.(controller|service|mapper|support|agent|config|security|entity|dto)\.') |
            ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
    } else {
        [regex]::Matches($content, 'from [''\"]@/(api|stores|composables|components|router|views)/') |
            ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
    }
    $churn = [Math]::Min(10, [int]$changes[$path] + [int]$workingChanges.ContainsKey($path))
    [pscustomobject]@{
        Score = [Math]::Ceiling($lineCount / 250) + 2 * $churn + $dependencyAreas.Count
        Lines = $lineCount
        Changes90d = $churn
        DependencyAreas = $dependencyAreas.Count
        Path = $path
    }
}

$items | Sort-Object Score, Lines -Descending | Select-Object -First $Top |
    Format-Table Score, Lines, Changes90d, DependencyAreas, Path -Wrap
Pop-Location
