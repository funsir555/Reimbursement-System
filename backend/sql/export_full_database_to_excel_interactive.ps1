$ErrorActionPreference = 'Stop'

function Get-PlainTextFromSecureString {
    param([Security.SecureString]$SecureString)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function New-Text {
    param([int[]]$Codes)
    return (-join ($Codes | ForEach-Object { [char]$_ }))
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$pythonExe = Join-Path $projectRoot '.venv\Scripts\python.exe'
$exportScript = Join-Path $scriptDir 'export_full_database_to_excel.py'
$defaultOutputDir = Join-Path $scriptDir 'templates'
$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$defaultOutputPath = Join-Path $defaultOutputDir ("finex_db_full_export_{0}.xlsx" -f $timestamp)

if (-not (Test-Path $pythonExe)) {
    throw "Virtual environment python not found: $pythonExe"
}
if (-not (Test-Path $exportScript)) {
    throw "Export script not found: $exportScript"
}

try {
    & chcp.com 65001 > $null
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [Console]::InputEncoding = $utf8NoBom
    [Console]::OutputEncoding = $utf8NoBom
    $OutputEncoding = $utf8NoBom
}
catch {
}

Write-Host (New-Text @(0x5C06,0x5BFC,0x51FA,0x5F53,0x524D,0x6570,0x636E,0x5E93,0x5168,0x90E8,0x57FA,0x7840,0x8868,0x5230,0x4E00,0x4E2A,0x20,0x45,0x78,0x63,0x65,0x6C,0x20,0x6587,0x4EF6,0x3002))
Write-Host (New-Text @(0x5982,0x5DF2,0x5728,0x73AF,0x5883,0x53D8,0x91CF,0x4E2D,0x914D,0x7F6E,0x5BC6,0x7801,0xFF0C,0x53EF,0x76F4,0x63A5,0x56DE,0x8F66,0x3002))
Write-Host ((New-Text @(0x9ED8,0x8BA4,0x4FDD,0x5B58,0x8DEF,0x5F84,0xFF1A)) + ' ' + $defaultOutputDir)
Write-Host ''

$securePassword = Read-Host (New-Text @(0x8BF7,0x8F93,0x5165,0x6570,0x636E,0x5E93,0x5BC6,0x7801,0xFF08,0x53EF,0x56DE,0x8F66,0xFF09)) -AsSecureString
$password = Get-PlainTextFromSecureString -SecureString $securePassword
$tablesInput = Read-Host (New-Text @(0x8BF7,0x8F93,0x5165,0x8981,0x5BFC,0x51FA,0x7684,0x8868,0x540D,0xFF0C,0x591A,0x4E2A,0x8868,0x8BF7,0x7528,0x9017,0x53F7,0x5206,0x9694,0xFF1B,0x76F4,0x63A5,0x56DE,0x8F66,0x5BFC,0x51FA,0x5168,0x90E8,0x8868)))

$hadPassword = Test-Path Env:FINEX_DB_PASSWORD
$oldPassword = $env:FINEX_DB_PASSWORD

try {
    if (-not [string]::IsNullOrWhiteSpace($password)) {
        $env:FINEX_DB_PASSWORD = $password
    }
    $env:PYTHONUTF8 = '1'
    $env:PYTHONIOENCODING = 'utf-8'

    $arguments = @(
        $exportScript,
        '--output',
        $defaultOutputPath
    )
    if (-not [string]::IsNullOrWhiteSpace($tablesInput)) {
        $arguments += '--tables'
        $arguments += $tablesInput
    }

    Write-Host ''
    Write-Host ((New-Text @(0x5BFC,0x51FA,0x6587,0x4EF6,0xFF1A)) + ' ' + $defaultOutputPath)
    Write-Host ''

    & $pythonExe @arguments
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        exit $exitCode
    }
}
finally {
    if ($hadPassword) {
        $env:FINEX_DB_PASSWORD = $oldPassword
    }
    else {
        Remove-Item Env:FINEX_DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
