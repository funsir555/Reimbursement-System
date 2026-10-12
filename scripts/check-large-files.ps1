param([string]$BaselinePath = (Join-Path $PSScriptRoot 'p3-large-file-baseline.json'))

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$baseline = Get-Content -LiteralPath $BaselinePath -Raw -Encoding utf8 | ConvertFrom-Json -AsHashtable
$violations = [System.Collections.Generic.List[string]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)

Push-Location $root
try {
    $files = @(
        rg --files 'backend' -g '*.java'
        rg --files 'frontend/admin-web/src' -g '*.vue' -g '*.ts'
    )
    foreach ($candidate in $files) {
        $path = $candidate.Replace('\', '/')
        if ($path.EndsWith('.java') -and $path -notmatch '/src/main/java/') { continue }
        if ($path -match '/__tests__/|\.test\.ts$') { continue }
        [void]$seen.Add($path)
        $limit = if ($path.EndsWith('.java')) { 1000 } else { 800 }
        $lineCount = [IO.File]::ReadAllLines((Join-Path $root $path)).Length
        if ($baseline.ContainsKey($path)) {
            $legacyLimit = [int]$baseline[$path]
            if ($legacyLimit -le $limit) {
                $violations.Add("Invalid legacy baseline ($legacyLimit <= $limit): $path")
            } elseif ($lineCount -le $limit) {
                $violations.Add("Remove recovered file from legacy baseline ($lineCount <= $limit): $path")
            } elseif ($lineCount -gt $legacyLimit) {
                $violations.Add("Legacy file grew from $legacyLimit to $lineCount lines: $path")
            } elseif ($lineCount -lt $legacyLimit) {
                $violations.Add("Lower legacy limit from $legacyLimit to $lineCount lines: $path")
            }
        } elseif ($lineCount -gt $limit) {
            $violations.Add("New oversized source file ($lineCount > $limit): $path")
        }
    }
    foreach ($path in $baseline.Keys) {
        if (-not $seen.Contains($path)) {
            $violations.Add("Remove missing file from legacy baseline: $path")
        }
    }
    $baseRef = if ($env:GITHUB_BASE_REF) {
        "origin/$($env:GITHUB_BASE_REF)"
    } elseif ($env:GITHUB_EVENT_NAME -eq 'push') {
        'HEAD^'
    } else {
        $null
    }
    if ($baseRef) {
        $previousJson = git -C $root show "$($baseRef):scripts/p3-large-file-baseline.json" 2>$null
        if ($LASTEXITCODE -eq 0) {
            $previous = ($previousJson -join "`n") | ConvertFrom-Json -AsHashtable
            foreach ($path in $baseline.Keys) {
                if (-not $previous.ContainsKey($path)) {
                    $violations.Add("Legacy exception was added instead of shrinking the baseline: $path")
                } elseif ([int]$baseline[$path] -gt [int]$previous[$path]) {
                    $violations.Add("Legacy limit was raised from $($previous[$path]) to $($baseline[$path]): $path")
                }
            }
        }
    }
    if ($violations.Count -gt 0) {
        throw ($violations -join "`n")
    }
    Write-Output "Large-file gate passed: $($seen.Count) production files; $($baseline.Count) frozen legacy exceptions."
} finally {
    Pop-Location
}
