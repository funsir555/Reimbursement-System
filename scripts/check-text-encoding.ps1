$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot
try {
    $changed = @()
    if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_BASE_REF)) {
        $changed += & git diff --name-only --diff-filter=ACMR "origin/$($env:GITHUB_BASE_REF)...HEAD"
    } elseif (-not [string]::IsNullOrWhiteSpace($env:GITHUB_EVENT_BEFORE) -and
              $env:GITHUB_EVENT_BEFORE -notmatch '^0+$') {
        $changed += & git diff --name-only --diff-filter=ACMR "$($env:GITHUB_EVENT_BEFORE)...HEAD"
    } else {
        $changed += & git diff --name-only --diff-filter=ACMR
        $changed += & git diff --cached --name-only --diff-filter=ACMR
        $changed += & git ls-files --others --exclude-standard
    }
    $files = $changed | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Sort-Object -Unique
    $textExtensions = @('.java', '.js', '.json', '.md', '.properties', '.ps1', '.sql', '.ts', '.tsx', '.vue', '.xml', '.yaml', '.yml')
    $utf8 = [System.Text.UTF8Encoding]::new($false, $true)
    $findings = [System.Collections.Generic.List[string]]::new()

    foreach ($relativePath in $files) {
        if ($textExtensions -notcontains [System.IO.Path]::GetExtension($relativePath).ToLowerInvariant()) {
            continue
        }
        $absolutePath = Join-Path $repositoryRoot $relativePath
        if (-not (Test-Path -LiteralPath $absolutePath)) {
            continue
        }
        try {
            $content = $utf8.GetString([System.IO.File]::ReadAllBytes($absolutePath))
            if ($content.Contains([char]0xFFFD)) {
                $findings.Add("replacement character detected: $relativePath")
            }
        } catch {
            $findings.Add("invalid UTF-8: $relativePath")
        }
    }

    if ($findings.Count -gt 0) {
        $findings | ForEach-Object { Write-Error $_ }
        exit 1
    }
    Write-Output "UTF-8 check passed for $($files.Count) changed files."
} finally {
    Pop-Location
}
