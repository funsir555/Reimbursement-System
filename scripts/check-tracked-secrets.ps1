$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot
try {
    $trackedOutput = & git -c core.quotePath=false ls-files
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to enumerate tracked files.'
    }
    $trackedFiles = @($trackedOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $trackedFiles += & git -c core.quotePath=false ls-files --others --exclude-standard
    $trackedFiles = $trackedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Sort-Object -Unique
    $textExtensions = @('.bat', '.cmd', '.conf', '.env', '.java', '.js', '.json', '.md', '.properties', '.ps1', '.sql', '.ts', '.tsx', '.vue', '.xml', '.yaml', '.yml')
    $findings = [System.Collections.Generic.List[string]]::new()
    $utf8 = [System.Text.UTF8Encoding]::new($false, $true)
    $providerKeyPattern = '(?<![A-Za-z0-9_-])' + [Regex]::Escape(('s' + 'k-')) + '[A-Za-z0-9_-]{20,}'

    foreach ($relativePath in $trackedFiles) {
        $normalizedPath = $relativePath.Replace('\', '/')
        if ($normalizedPath -match '(^|/)tmp-[^/]*-token\.txt$') {
            $findings.Add("high-risk token filename: $normalizedPath")
            continue
        }
        $extension = [System.IO.Path]::GetExtension($relativePath).ToLowerInvariant()
        if ($textExtensions -notcontains $extension) {
            continue
        }
        $absolutePath = Join-Path $repositoryRoot $relativePath
        if (-not (Test-Path -LiteralPath $absolutePath)) {
            continue
        }
        $fileInfo = Get-Item -LiteralPath $absolutePath
        if ($fileInfo.Length -gt 2MB) {
            continue
        }
        try {
            $content = $utf8.GetString([System.IO.File]::ReadAllBytes($absolutePath))
        } catch {
            continue
        }
        if ($content -match 'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}') {
            $findings.Add("JWT-shaped value: $normalizedPath")
        }
        if ($content -match $providerKeyPattern) {
            $findings.Add("provider API key-shaped value: $normalizedPath")
        }
        if ($content -match '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----') {
            $findings.Add("private key material: $normalizedPath")
        }
        $lineNumber = 0
        foreach ($line in ($content -split "`r?`n")) {
            $lineNumber += 1
            if ($line -notmatch '(?i)^\s*(?:set\s+|export\s+)?(?:FINEX_JWT_SECRET|API_KEY|ACCESS_TOKEN|SECRET_KEY)\s*[:=]\s*["'']?([^"''#\s]+)') {
                continue
            }
            $value = $Matches[1]
            if ($value -match '^(?:\$\{|replace-|your-|example|placeholder|dummy|test|local-|<|\*)' -or $value -eq '') {
                continue
            }
            if ($normalizedPath -match '(^|/)(?:src/test|docs|执行记录)/') {
                continue
            }
            $findings.Add("possible hard-coded secret: ${normalizedPath}:$lineNumber")
        }
    }

    if ($findings.Count -gt 0) {
        $findings | Sort-Object -Unique | ForEach-Object { Write-Error $_ }
        exit 1
    }
    Write-Output "Secret scan passed for $($trackedFiles.Count) current-tree files."
} finally {
    Pop-Location
}
