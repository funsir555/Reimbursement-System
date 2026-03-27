$ErrorActionPreference = 'Stop'

function New-Text {
    param([int[]]$Codes)
    return (-join ($Codes | ForEach-Object { [char]$_ }))
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$configPath = Join-Path $projectRoot 'backend\auth-service\src\main\resources\application.yml'
$outputPath = Join-Path $PSScriptRoot 'finex_db_schema.csv'

if (-not (Test-Path $configPath)) {
    throw "Config file not found: $configPath"
}

$configContent = Get-Content $configPath -Raw -Encoding UTF8

$urlMatch = [regex]::Match($configContent, '(?m)^\s*url:\s*jdbc:mysql://(?<host>[^:/?#]+)(:(?<port>\d+))?/(?<db>[^? \r\n]+)')
$userMatch = [regex]::Match($configContent, '(?m)^\s*username:\s*(?<value>.+?)\s*$')
$passwordMatch = [regex]::Match($configContent, '(?m)^\s*password:\s*(?<value>.+?)\s*$')

if (-not $urlMatch.Success -or -not $userMatch.Success -or -not $passwordMatch.Success) {
    throw 'Failed to parse datasource settings from application.yml.'
}

$dbHost = $urlMatch.Groups['host'].Value
$dbPort = if ($urlMatch.Groups['port'].Success) { $urlMatch.Groups['port'].Value } else { '3306' }
$dbName = $urlMatch.Groups['db'].Value
$dbUser = $userMatch.Groups['value'].Value.Trim()
$dbPassword = $passwordMatch.Groups['value'].Value.Trim()

$mysqlCommand = Get-Command mysql -ErrorAction Stop
$sql = @"
SELECT
    t.table_name,
    IFNULL(t.table_comment, ''),
    c.column_name,
    IFNULL(c.column_comment, ''),
    CASE
        WHEN c.column_key = 'PRI' THEN 'key'
        ELSE UPPER(c.data_type)
    END AS field_attr,
    CASE
        WHEN LOCATE('(', c.column_type) > 0 THEN SUBSTRING_INDEX(SUBSTRING_INDEX(c.column_type, '(', -1), ')', 1)
        ELSE ''
    END AS field_length
FROM information_schema.columns c
INNER JOIN information_schema.tables t
    ON t.table_schema = c.table_schema
   AND t.table_name = c.table_name
WHERE c.table_schema = '$dbName'
ORDER BY c.table_name, c.ordinal_position;
"@

$env:MYSQL_PWD = $dbPassword
try {
    $rows = & $mysqlCommand.Source `
        --host=$dbHost `
        --port=$dbPort `
        --user=$dbUser `
        --default-character-set=utf8mb4 `
        --batch `
        --raw `
        --skip-column-names `
        --execute=$sql 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw ($rows -join [Environment]::NewLine)
    }
}
finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

$headerTableEn = New-Text @(0x82F1,0x6587,0x8868,0x540D)
$headerTableZh = New-Text @(0x4E2D,0x6587,0x8868,0x540D)
$headerColumnEn = New-Text @(0x82F1,0x6587,0x5B57,0x6BB5,0x540D)
$headerColumnZh = New-Text @(0x4E2D,0x6587,0x5B57,0x6BB5,0x540D)
$headerAttr = New-Text @(0x5B57,0x6BB5,0x5C5E,0x6027)
$headerLength = New-Text @(0x957F,0x5EA6)
$headers = @(
    $headerTableEn,
    $headerTableZh,
    $headerColumnEn,
    $headerColumnZh,
    $headerAttr,
    $headerLength
)

$records = foreach ($row in $rows) {
    if ([string]::IsNullOrWhiteSpace($row)) {
        continue
    }

    $parts = $row -split "`t", 6
    $record = [ordered]@{}
    $record[$headerTableEn] = if ($parts.Count -gt 0) { $parts[0] } else { '' }
    $record[$headerTableZh] = if ($parts.Count -gt 1) { $parts[1] } else { '' }
    $record[$headerColumnEn] = if ($parts.Count -gt 2) { $parts[2] } else { '' }
    $record[$headerColumnZh] = if ($parts.Count -gt 3) { $parts[3] } else { '' }
    $record[$headerAttr] = if ($parts.Count -gt 4) { $parts[4] } else { '' }
    $record[$headerLength] = if ($parts.Count -gt 5) { $parts[5] } else { '' }
    [pscustomobject]$record
}

$records |
    Select-Object $headers |
    Export-Csv -Path $outputPath -NoTypeInformation -Encoding UTF8

Write-Host "Schema CSV generated: $outputPath"
