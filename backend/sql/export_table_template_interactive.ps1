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

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$pythonExe = Join-Path $projectRoot '.venv\Scripts\python.exe'
$exportScript = Join-Path $scriptDir 'export_table_template.py'

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

Write-Host '如遇执行策略限制，可使用：'
Write-Host "powershell -ExecutionPolicy Bypass -File `"$($MyInvocation.MyCommand.Path)`""
Write-Host ''

$tableName = Read-Host '请输入要导出的表名'
if ([string]::IsNullOrWhiteSpace($tableName)) {
    throw '表名不能为空。'
}

$securePassword = Read-Host '请输入数据库密码' -AsSecureString
$password = Get-PlainTextFromSecureString -SecureString $securePassword
$defaultOutputPath = Join-Path $scriptDir ("templates\{0}.xlsx" -f $tableName)

$hadPassword = Test-Path Env:FINEX_DB_PASSWORD
$oldPassword = $env:FINEX_DB_PASSWORD

try {
    $env:FINEX_DB_PASSWORD = $password
    & $pythonExe $exportScript '--table' $tableName
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        exit $exitCode
    }
    Write-Host ''
    Write-Host "导出完成：$defaultOutputPath"
}
finally {
    if ($hadPassword) {
        $env:FINEX_DB_PASSWORD = $oldPassword
    }
    else {
        Remove-Item Env:FINEX_DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
