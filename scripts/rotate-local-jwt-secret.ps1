param(
    [string]$EnvironmentFile = ""
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $repositoryRoot 'backend/.env.local.cmd'
}

$resolvedPath = [System.IO.Path]::GetFullPath($EnvironmentFile)
if (-not (Test-Path -LiteralPath $resolvedPath)) {
    throw "Local environment file does not exist: $resolvedPath"
}

$randomBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
$secret = [Convert]::ToBase64String($randomBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

$utf8 = [System.Text.UTF8Encoding]::new($false)
$lines = [System.IO.File]::ReadAllLines($resolvedPath, $utf8)
$replacement = "set FINEX_JWT_SECRET=$secret"
$replaced = $false
$nextLines = foreach ($line in $lines) {
    if ($line -match '^\s*(?:set\s+)?FINEX_JWT_SECRET=') {
        $replaced = $true
        $replacement
    } else {
        $line
    }
}
if (-not $replaced) {
    $nextLines = @($nextLines) + $replacement
}

$temporaryPath = "$resolvedPath.codex-rotate.tmp"
try {
    [System.IO.File]::WriteAllLines($temporaryPath, $nextLines, $utf8)
    [System.IO.File]::Move($temporaryPath, $resolvedPath, $true)
} finally {
    if (Test-Path -LiteralPath $temporaryPath) {
        Remove-Item -LiteralPath $temporaryPath -Force
    }
}

Write-Output 'Local JWT signing secret rotated without displaying the secret.'

