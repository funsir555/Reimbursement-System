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

function Invoke-PythonJson {
    param(
        [string]$PythonExe,
        [string]$ScriptPath,
        [string[]]$Arguments
    )

    $output = & $PythonExe $ScriptPath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        if ($output) {
            $output | ForEach-Object { Write-Host $_ }
        }
        throw "命令执行失败：$ScriptPath $($Arguments -join ' ')"
    }
    return ($output -join [Environment]::NewLine | ConvertFrom-Json)
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$pythonExe = Join-Path $projectRoot '.venv\Scripts\python.exe'
$importScript = Join-Path $scriptDir 'import_excel_to_mysql.py'
$helperScript = Join-Path $scriptDir 'excel_tool_runtime_helper.py'

if (-not (Test-Path $pythonExe)) {
    throw "Virtual environment python not found: $pythonExe"
}
if (-not (Test-Path $importScript)) {
    throw "Import script not found: $importScript"
}
if (-not (Test-Path $helperScript)) {
    throw "Runtime helper script not found: $helperScript"
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

$excelPathInput = Read-Host '请输入 Excel 完整路径(.xlsx)'
if ([string]::IsNullOrWhiteSpace($excelPathInput)) {
    throw 'Excel 路径不能为空。'
}

$excelPath = [System.IO.Path]::GetFullPath($excelPathInput)
if (-not (Test-Path $excelPath)) {
    throw "Excel 文件不存在：$excelPath"
}
if ([System.IO.Path]::GetExtension($excelPath).ToLowerInvariant() -ne '.xlsx') {
    throw '当前仅支持 .xlsx 文件导入。'
}

$tableName = Read-Host '请输入目标表名'
if ([string]::IsNullOrWhiteSpace($tableName)) {
    throw '目标表名不能为空。'
}

$securePassword = Read-Host '请输入数据库密码' -AsSecureString
$password = Get-PlainTextFromSecureString -SecureString $securePassword

$hadPassword = Test-Path Env:FINEX_DB_PASSWORD
$oldPassword = $env:FINEX_DB_PASSWORD

try {
    $env:FINEX_DB_PASSWORD = $password

    $modeInfo = Invoke-PythonJson -PythonExe $pythonExe -ScriptPath $helperScript -Arguments @(
        'detect-template',
        '--file', $excelPath
    )
    $headerRow = [int]$modeInfo.header_row
    $startRow = [int]$modeInfo.start_row
    Write-Host "已识别导入模式：$($modeInfo.mode)，header-row=$headerRow，start-row=$startRow"
    Write-Host "识别说明：$($modeInfo.reason)"
    Write-Host ''

    $keyInfo = Invoke-PythonJson -PythonExe $pythonExe -ScriptPath $helperScript -Arguments @(
        'resolve-keys',
        '--table', $tableName
    )

    if ($keyInfo.status -eq 'selected') {
        $keyColumns = ($keyInfo.key_columns -join ',')
        Write-Host "自动采用重复校验键：$keyColumns（来源：$($keyInfo.source)）"
    }
    elseif ($keyInfo.status -eq 'ambiguous') {
        Write-Host '检测到多个唯一键候选，请手工输入 key-columns。'
        foreach ($candidate in $keyInfo.candidates) {
            Write-Host ("- {0}: {1}" -f $candidate.index_name, ($candidate.columns -join ','))
        }
        $keyColumns = Read-Host '请输入 key-columns（逗号分隔）'
        if ([string]::IsNullOrWhiteSpace($keyColumns)) {
            throw '未提供 key-columns，已停止导入。'
        }
    }
    else {
        throw '当前表无法自动确定主键或唯一键。请先补充唯一约束后再导入。'
    }

    Write-Host ''
    Write-Host '开始执行 dry-run 校验...'
    & $pythonExe $importScript `
        '--file' $excelPath `
        '--table' $tableName `
        '--key-columns' $keyColumns `
        '--header-row' $headerRow `
        '--start-row' $startRow `
        '--dry-run'
    $dryRunExitCode = $LASTEXITCODE
    if ($dryRunExitCode -ne 0) {
        exit $dryRunExitCode
    }

    Write-Host ''
    $confirm = Read-Host 'dry-run 已通过，是否正式导入？输入 Y 继续'
    if ($confirm -notmatch '^(?i)y(es)?$') {
        Write-Host '已取消正式导入。'
        exit 0
    }

    Write-Host ''
    Write-Host '开始正式导入...'
    & $pythonExe $importScript `
        '--file' $excelPath `
        '--table' $tableName `
        '--key-columns' $keyColumns `
        '--header-row' $headerRow `
        '--start-row' $startRow
    exit $LASTEXITCODE
}
finally {
    if ($hadPassword) {
        $env:FINEX_DB_PASSWORD = $oldPassword
    }
    else {
        Remove-Item Env:FINEX_DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
