param([string]$SourceSchema = 'finex_db', [string]$MySqlUser = 'root')

$ErrorActionPreference = 'Stop'
if ($SourceSchema -notmatch '^[A-Za-z0-9_]+$') { throw '源库名称无效' }
$secret = [Environment]::GetEnvironmentVariable('finex_db_password')
if ([string]::IsNullOrWhiteSpace($secret)) { throw '缺少 finex_db_password 环境变量' }
$previousPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD')
$env:MYSQL_PWD = $secret
$schema = 'finex_conversation_verify_' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$created = $false

function Invoke-MySql([string]$sql, [string]$database = '') {
    $arguments = @('--host=localhost', "--user=$MySqlUser", '--batch', '--skip-column-names',
        '--default-character-set=utf8mb4')
    if ($database) { $arguments += "--database=$database" }
    $output = & mysql @arguments "--execute=$sql" 2>&1
    if ($LASTEXITCODE -ne 0) { throw "隔离库验证失败：$output" }
    return ($output | Out-String).Trim()
}

try {
    Invoke-MySql "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" | Out-Null
    $created = $true
    Invoke-MySql "CREATE TABLE $schema.ea_agent_conversation LIKE $SourceSchema.ea_agent_conversation" | Out-Null
    Invoke-MySql "CREATE TABLE $schema.ea_agent_message LIKE $SourceSchema.ea_agent_message" | Out-Null
    foreach ($column in @('custom_title', 'is_pinned')) {
        $exists = [int](Invoke-MySql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$schema' AND table_name='ea_agent_conversation' AND column_name='$column'")
        if ($exists -eq 1) { Invoke-MySql "ALTER TABLE $schema.ea_agent_conversation DROP COLUMN $column" | Out-Null }
    }
    Invoke-MySql "INSERT INTO $schema.ea_agent_conversation (tenant_id,company_id,session_id,user_id) VALUES ('tenant-A','COMPANY_A','legacy-session',7)" | Out-Null
    $path = Join-Path $PSScriptRoot '..\backend\sql\migrate_finwork_conversation_management_20261014.sql'
    $migration = Get-Content -LiteralPath $path -Raw -Encoding utf8
    Invoke-MySql $migration $schema | Out-Null
    Invoke-MySql $migration $schema | Out-Null
    $columns = [int](Invoke-MySql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$schema' AND table_name='ea_agent_conversation' AND column_name IN ('custom_title','is_pinned')")
    if ($columns -ne 2) { throw "迁移后字段数量异常：$columns" }
    $default = Invoke-MySql "SELECT column_default FROM information_schema.columns WHERE table_schema='$schema' AND table_name='ea_agent_conversation' AND column_name='is_pinned'"
    if ($default -ne '0') { throw "置顶默认值异常：$default" }
    $legacy = Invoke-MySql "SELECT CONCAT(is_pinned,':',COALESCE(custom_title,'NULL')) FROM $schema.ea_agent_conversation WHERE session_id='legacy-session'"
    if ($legacy -ne '0:NULL') { throw "旧会话迁移结果异常：$legacy" }
    Invoke-MySql "INSERT INTO $schema.ea_agent_message (tenant_id,company_id,session_id,role,content) VALUES ('tenant-A','COMPANY_A','legacy-session','user','原始问题')" | Out-Null
    Invoke-MySql "UPDATE $schema.ea_agent_conversation SET custom_title='新名称',is_pinned=1 WHERE session_id='legacy-session'" | Out-Null
    $visible = Invoke-MySql @'
SELECT CONCAT(COALESCE(c.custom_title,(SELECT LEFT(m.content,48) FROM ea_agent_message m
  WHERE m.tenant_id=c.tenant_id AND m.company_id=c.company_id AND m.session_id=c.session_id
    AND m.role='user' ORDER BY m.created_at,m.id LIMIT 1)),':',c.is_pinned)
FROM ea_agent_conversation c WHERE c.tenant_id='tenant-A' AND c.user_id=7 AND c.deleted=0
  AND c.is_pinned=1 AND EXISTS (SELECT 1 FROM ea_agent_message m
    WHERE m.tenant_id=c.tenant_id AND m.company_id=c.company_id AND m.session_id=c.session_id)
'@ $schema
    if ($visible -ne '新名称:1') { throw "置顶标题查询异常：$visible" }
    Write-Output 'FinWork 会话管理迁移重复执行通过；旧会话默认未置顶。'
} finally {
    if ($created -and $schema -match '^finex_conversation_verify_[0-9a-f]{8}$') {
        Invoke-MySql "DROP DATABASE $schema" | Out-Null
    }
    if ($null -eq $previousPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $previousPassword }
}
