param(
    [string]$SourceSchema = 'finex_db',
    [string]$MySqlUser = 'root'
)

$ErrorActionPreference = 'Stop'
if ($SourceSchema -notmatch '^[A-Za-z0-9_]+$') { throw '源库名称只允许字母、数字和下划线' }
$secret = [Environment]::GetEnvironmentVariable('finex_db_password')
if ([string]::IsNullOrWhiteSpace($secret)) { throw '缺少 finex_db_password 环境变量' }
$oldMySqlPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD')
$env:MYSQL_PWD = $secret
$schemaName = 'finex_agent_verify_' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$created = $false

function Invoke-MySql([string]$sql, [string]$schema = '') {
    $arguments = @('--host=localhost', "--user=$MySqlUser", '--batch', '--skip-column-names',
        '--default-character-set=utf8mb4')
    if ($schema) { $arguments += "--database=$schema" }
    $result = & mysql @arguments "--execute=$sql" 2>&1
    if ($LASTEXITCODE -ne 0) { throw "MySQL 验证失败：$result" }
    return ($result | Out-String).Trim()
}

function Assert-Count([string]$sql, [int]$expected) {
    $actual = [int](Invoke-MySql $sql $schemaName)
    if ($actual -ne $expected) { throw "数量不符：预期 $expected，实际 $actual" }
}

try {
    Invoke-MySql "CREATE DATABASE $schemaName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" | Out-Null
    $created = $true
    foreach ($table in @('sys_company', 'sys_permission', 'sys_role', 'sys_role_permission',
            'sys_user', 'sys_user_role', 'sys_department', 'gl_period_close', 'gl_accvouch',
            'fin_account_set', 'fin_account_subject', 'fin_project_class', 'fin_project_archive',
            'gl_Customer', 'gl_Vender', 'pm_document_instance', 'ea_agent_run')) {
        Invoke-MySql "CREATE TABLE $schemaName.$table LIKE $SourceSchema.$table" | Out-Null
    }
    $initializationPath = Join-Path $PSScriptRoot '..\backend\sql\ea_agent_platform_init.sql'
    $initializationSql = Get-Content -LiteralPath $initializationPath -Raw -Encoding utf8
    Invoke-MySql $initializationSql $schemaName | Out-Null
    Invoke-MySql $initializationSql $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME='ea_agent_run' AND COLUMN_NAME='execution_context'" 1
    Assert-Count "SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA='$schemaName' AND EVENT_NAME='clean_expired_mappings'" 1
    Invoke-MySql @'
INSERT INTO sys_permission (permission_code, permission_name, permission_type, sort_order, status)
VALUES ('settings:roles:view','角色查看','MENU',1,1),
       ('agent:chat:view','FinWork','MENU',2,1),
       ('agent:digital_employee:view','数字员工','MENU',3,1),
       ('agent:workbench:view','工作台','MENU',4,1),
       ('finance:general_ledger:menu','总账','MENU',5,1);
INSERT INTO sys_permission
  (permission_code,permission_name,permission_type,parent_id,module_code,route_path,sort_order,status)
SELECT 'finance:general_ledger:balance_sheet:view','余额表','MENU',id,
       'finance','/finance/general-ledger/balance-sheet',4014,1
FROM sys_permission WHERE permission_code='finance:general_ledger:menu';
INSERT INTO sys_permission
  (permission_code,permission_name,permission_type,parent_id,module_code,route_path,sort_order,status)
SELECT 'finance:general_ledger:supplier_detail_ledger:view','供应商明细账','MENU',id,
       'finance','/finance/general-ledger/supplier-detail-ledger',4018,1
FROM sys_permission WHERE permission_code='finance:general_ledger:menu';
INSERT INTO sys_role (role_code, role_name, status)
VALUES ('SUPER_ADMIN','超级管理员',1),('finance_reader','财务查询员',1);
INSERT INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r,sys_permission p
WHERE r.role_code='SUPER_ADMIN'
  AND p.permission_code IN ('finance:general_ledger:balance_sheet:view',
    'finance:general_ledger:supplier_detail_ledger:view');
INSERT INTO sys_user (username,password,company_id,status,sync_managed)
VALUES ('verify_owner','unused','A',1,0);
INSERT INTO ea_digital_employee
(tenant_id,company_id,name,type,responsible_user_id,authority_level,status,created_by)
VALUES ('default','A','旧员工','咨询型',1,0,'active',1)
'@ $schemaName | Out-Null

    $migrationPath = Join-Path $PSScriptRoot '..\backend\sql\migrate_digital_employee_query_20261001.sql'
    $migrationSql = Get-Content -LiteralPath $migrationPath -Raw -Encoding utf8
    Invoke-MySql $migrationSql $schemaName | Out-Null
    Invoke-MySql $migrationSql $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM ea_digital_employee_service WHERE employee_id=1 AND lifecycle_status='SUSPENDED'" 1
    Assert-Count "SELECT COUNT(*) FROM sys_query_capability WHERE status=1" 2
    Assert-Count "SELECT COUNT(*) FROM sys_permission WHERE permission_code='finance:period_close:query'" 1
    Assert-Count "SELECT COUNT(*) FROM sys_role_permission rp JOIN sys_role r ON r.id=rp.role_id JOIN sys_permission p ON p.id=rp.permission_id WHERE r.role_code='SUPER_ADMIN' AND p.permission_code IN ('settings:roles:assign_data_scope','expense:agent:query','finance:period_close:query','agent:digital_employee:create','agent:digital_employee:edit','agent:digital_employee:assign','agent:digital_employee:status','agent:digital_employee:plan','agent:digital_employee:logs','agent:workbench:share_submit','agent:workbench:share_approve')" 11
    Assert-Count "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME='pm_document_instance' AND COLUMN_NAME='company_id'" 1
    Assert-Count "SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME='pm_document_instance' AND INDEX_NAME='idx_pm_document_instance_company_created_at'" 1

    $phaseOnePath = Join-Path $PSScriptRoot '..\backend\sql\migrate_finwork_phase1_query_20261002.sql'
    $phaseOneSql = Get-Content -LiteralPath $phaseOnePath -Raw -Encoding utf8
    Invoke-MySql $phaseOneSql $schemaName | Out-Null
    Invoke-MySql $phaseOneSql $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM sys_query_capability WHERE status=1" 16
    Assert-Count "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME='ea_agent_message_query_origin_item' AND COLUMN_NAME='origin_count'" 1
    Assert-Count "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME IN ('ea_agent_chat_pending_plan','ea_agent_message_query_origin_item','sys_download_query_origin')" 3

    $controlledToolsPath = Join-Path $PSScriptRoot '..\backend\sql\migrate_agent_controlled_tools_20261009.sql'
    $controlledToolsSql = Get-Content -LiteralPath $controlledToolsPath -Raw -Encoding utf8
    if ($controlledToolsSql -match '(?im)^\s*USE\s+') {
        throw '受控工具迁移包含 USE，隔离验证不能保证目标库安全'
    }
    Invoke-MySql $controlledToolsSql $schemaName | Out-Null
    Invoke-MySql $controlledToolsSql $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM sys_query_capability WHERE status=1" 23
    Assert-Count "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME IN ('ea_workflow_share_capability','ea_controlled_result_origin')" 2
    Assert-Count "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$schemaName' AND TABLE_NAME='ea_digital_employee_delivery' AND COLUMN_NAME='origin_required'" 1
    Assert-Count "SELECT COUNT(*) FROM sys_permission WHERE permission_code IN ('finance:agent:account_subjects:query','finance:agent:project_classes:query','finance:agent:projects:query','finance:agent:customers:query','finance:agent:vendors:query','system:agent:departments:query','system:agent:people:query')" 7

    # Reproduce the target database drift: capability exists, but the reused
    # sequence ledger permission and its super-admin grant are absent.
    Assert-Count "SELECT COUNT(*) FROM sys_query_capability WHERE permission_code='finance:general_ledger:sequence_ledger:view' AND status=1" 1
    Assert-Count "SELECT COUNT(*) FROM sys_permission WHERE permission_code LIKE 'finance:general_ledger:sequence_ledger:%'" 0
    $sequenceRepairPath = Join-Path $PSScriptRoot '..\backend\sql\migrate_agent_sequence_permission_20261010.sql'
    $sequenceRepairSql = Get-Content -LiteralPath $sequenceRepairPath -Raw -Encoding utf8
    if ($sequenceRepairSql -match '(?im)^\s*USE\s+') { throw '序时账权限补齐脚本不能指定目标库' }
    Invoke-MySql $sequenceRepairSql $schemaName | Out-Null
    Invoke-MySql $sequenceRepairSql $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM sys_permission WHERE permission_code IN ('finance:general_ledger:sequence_ledger:view','finance:general_ledger:sequence_ledger:export') AND status=1" 2
    Assert-Count "SELECT COUNT(*) FROM sys_role_permission rp JOIN sys_role r ON r.id=rp.role_id JOIN sys_permission p ON p.id=rp.permission_id WHERE r.role_code='SUPER_ADMIN' AND p.permission_code IN ('finance:general_ledger:sequence_ledger:view','finance:general_ledger:sequence_ledger:export')" 2
    Assert-Count "SELECT COUNT(*) FROM sys_role_permission rp JOIN sys_role r ON r.id=rp.role_id JOIN sys_permission p ON p.id=rp.permission_id WHERE r.role_code='finance_reader' AND p.permission_code LIKE 'finance:general_ledger:sequence_ledger:%'" 0
    $controlledPermissionCodes = @(
        'expense:agent:query','finance:period_close:query',
        'finance:general_ledger:balance_sheet:view','finance:general_ledger:sequence_ledger:view',
        'finance:general_ledger:supplier_detail_ledger:view',
        'finance:agent:account_subjects:query','finance:agent:project_classes:query',
        'finance:agent:projects:query','finance:agent:customers:query',
        'finance:agent:vendors:query','system:agent:departments:query',
        'system:agent:people:query'
    )
    $catalogPath = Join-Path $PSScriptRoot '..\backend\auth-service\src\main\java\com\finex\auth\agent\workflow\ControlledQueryCatalog.java'
    $catalogText = Get-Content -LiteralPath $catalogPath -Raw -Encoding utf8
    $catalogToolCount = ([regex]::Matches($catalogText, 'new Definition\(')).Count
    if ($catalogToolCount -ne $controlledPermissionCodes.Count) {
        throw "受控工具目录有 $catalogToolCount 项，权限验证清单有 $($controlledPermissionCodes.Count) 项，请同步维护"
    }
    $quotedCodes = ($controlledPermissionCodes | ForEach-Object { "'$_'" }) -join ','
    Assert-Count "SELECT COUNT(DISTINCT c.permission_code) FROM sys_query_capability c JOIN sys_permission p ON p.permission_code=c.permission_code AND p.status=1 JOIN sys_role_permission rp ON rp.permission_id=p.id JOIN sys_role r ON r.id=rp.role_id AND r.role_code='SUPER_ADMIN' AND r.status=1 WHERE c.status=1 AND c.permission_code IN ($quotedCodes)" $controlledPermissionCodes.Count

    Invoke-MySql @'
INSERT INTO sys_company (company_id,company_code,company_name,status)
VALUES ('A','A','甲公司',1),('B','B','乙公司',1),('C','C','丙公司',1);
INSERT INTO fin_account_set (company_id,status,enabled_year,enabled_period)
VALUES ('A','ACTIVE',2026,9),('B','ACTIVE',2026,9),('C','INITIALIZING',2026,9);
INSERT INTO gl_period_close (company_id,iyear,iperiod,iyperiod,status)
VALUES ('A',2026,9,202609,'CLOSED');
INSERT INTO gl_Vender (cVenCode,cVenName,company_id)
VALUES ('VA','云之智','A'),('VB','云之智','B');
INSERT INTO gl_accvouch (company_id,iyear,iyperiod,iperiod,ibook,void_flag,bdelete,iflag,cdigest,ccode,csup_id,md,mc)
VALUES ('A',2026,202609,9,1,0,0,0,'甲凭证','1001','VA',10,0),
       ('B',2026,202609,9,1,0,0,0,'乙凭证','1001','VB',20,0);
INSERT INTO sys_user (username,password,company_id,status,sync_managed)
VALUES ('verify_reader','unused','B',1,0);
INSERT INTO sys_user_role (user_id,role_id)
SELECT u.id,r.id FROM sys_user u,sys_role r
WHERE u.username='verify_reader' AND r.role_code='finance_reader';
INSERT INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r,sys_permission p
WHERE r.role_code='finance_reader' AND p.permission_code='finance:period_close:query';
INSERT INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r,sys_permission p
WHERE r.role_code='finance_reader'
  AND p.permission_code='finance:general_ledger:sequence_ledger:view';
INSERT INTO sys_role_query_scope (role_id,permission_code,scope_type,updated_by)
SELECT id,'finance:period_close:query','SELECTED_COMPANIES',1
FROM sys_role WHERE role_code='finance_reader';
INSERT INTO sys_role_query_scope_company (scope_id,company_id)
SELECT id,'B' FROM sys_role_query_scope WHERE permission_code='finance:period_close:query';
'@ $schemaName | Out-Null

    Assert-Count "SELECT COUNT(*) FROM fin_account_set a JOIN sys_company c ON c.company_id=a.company_id WHERE a.company_id='A' AND a.status='ACTIVE' AND c.status=1" 1
    Assert-Count "SELECT COUNT(*) FROM fin_account_set WHERE company_id='C' AND status='ACTIVE'" 0
    Invoke-MySql "UPDATE fin_account_set SET status='FAILED' WHERE company_id='C'" $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) FROM fin_account_set WHERE company_id='C' AND status='ACTIVE'" 0

    $sequenceScopeJoin = @'
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id=u.id
JOIN sys_role r ON r.id=ur.role_id AND r.status=1
JOIN sys_role_permission rp ON rp.role_id=r.id
JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1
JOIN sys_role_query_scope s ON s.role_id=r.id AND s.permission_code=p.permission_code
JOIN sys_role_query_scope_company sc ON sc.scope_id=s.id
JOIN sys_company c ON c.company_id=sc.company_id AND c.status=1
WHERE u.username='verify_reader'
  AND p.permission_code='finance:general_ledger:sequence_ledger:view'
'@
    Assert-Count "SELECT COUNT(*) $sequenceScopeJoin" 0
    Invoke-MySql @'
INSERT INTO sys_role_query_scope (role_id,permission_code,scope_type,updated_by)
SELECT id,'finance:general_ledger:sequence_ledger:view','SELECTED_COMPANIES',1
FROM sys_role WHERE role_code='finance_reader';
INSERT INTO sys_role_query_scope_company (scope_id,company_id)
SELECT id,'B' FROM sys_role_query_scope
WHERE permission_code='finance:general_ledger:sequence_ledger:view';
'@ $schemaName | Out-Null
    Assert-Count "SELECT COUNT(*) $sequenceScopeJoin AND c.company_id='B'" 1
    Assert-Count "SELECT COUNT(*) $sequenceScopeJoin AND c.company_id='A'" 0

    Assert-Count @'
SELECT COUNT(*) FROM sys_company c
WHERE c.status=1 AND c.company_id IN (
  SELECT sc.company_id FROM sys_user u
  JOIN sys_user_role ur ON ur.user_id=u.id
  JOIN sys_role r ON r.id=ur.role_id AND r.status=1
  JOIN sys_role_permission rp ON rp.role_id=r.id
  JOIN sys_permission p ON p.id=rp.permission_id AND p.status=1
  JOIN sys_role_query_scope s ON s.role_id=r.id AND s.permission_code=p.permission_code
  JOIN sys_role_query_scope_company sc ON sc.scope_id=s.id
  WHERE u.username='verify_reader' AND p.permission_code='finance:period_close:query'
) AND NOT EXISTS (
  SELECT 1 FROM gl_period_close g WHERE g.company_id=c.company_id
    AND g.iyear=2026 AND g.iperiod=9 AND g.status='CLOSED'
)
'@ 1
    Assert-Count @'
SELECT COUNT(*) FROM sys_company c WHERE c.status=1 AND NOT EXISTS (
  SELECT 1 FROM gl_period_close g WHERE g.company_id=c.company_id
    AND g.iyear=2026 AND g.iperiod=9 AND g.status='CLOSED'
)
'@ 2
    foreach ($projection in @(
        'SELECT subject_code,subject_name,parent_subject_code,subject_level,balance_direction,subject_category,bperson,bcus,bsup,bdept,bitem,status FROM fin_account_subject LIMIT 0',
        'SELECT project_class_code,project_class_name,status FROM fin_project_class LIMIT 0',
        'SELECT citemcode,citemname,citemccode,status,bclose FROM fin_project_archive LIMIT 0',
        'SELECT cCusCode,cCusName,cCusAbbName,cCCCode,bCusState,dEndDate FROM gl_Customer LIMIT 0',
        'SELECT cVenCode,cVenName,cVenAbbName,cVCCode,dEndDate FROM gl_Vender LIMIT 0',
        'SELECT id,dept_code,dept_name,parent_id,status FROM sys_department LIMIT 0',
        'SELECT id,username,position,labor_relation_belong FROM sys_user LIMIT 0',
        'SELECT i_id,iyperiod,ino_id,dbill_date,cbill,maker_user_id,ccheck,cbook,ibook,iflag,cdigest,ccode,ccode_name,cexch_name,md,mc,cdept_id,cperson_id,ccus_id,csup_id,citem_id,citem_class,cash_flow_item_id,cash_flow_item_name,cname,company_id FROM gl_accvouch LIMIT 0'
    )) { Invoke-MySql $projection $schemaName | Out-Null }
    Assert-Count "SELECT COUNT(*) FROM gl_accvouch WHERE company_id='A' AND iyear=2026 AND iperiod=9 AND COALESCE(void_flag,0)=0 AND voided_at IS NULL AND COALESCE(iflag,0)=0 AND COALESCE(bdelete,0)=0 AND (ibook=1 OR posted_at IS NOT NULL)" 1
    Assert-Count "SELECT COUNT(*) FROM gl_Vender WHERE company_id='A' AND cVenName='云之智' AND cVenCode='VA'" 1
    Assert-Count "SELECT COUNT(*) FROM gl_accvouch WHERE company_id='A' AND csup_id='VA'" 1
    Assert-Count "SELECT COUNT(*) FROM gl_accvouch WHERE company_id='A' AND csup_id='VB'" 0
    Assert-Count "SELECT COUNT(*) FROM fin_account_set a JOIN sys_company c ON c.company_id=a.company_id WHERE a.company_id='A' AND a.enabled_year=2026 AND a.enabled_period=9 AND a.status='ACTIVE' AND c.status=1" 1
    Assert-Count "SELECT COUNT(*) FROM gl_accvouch WHERE company_id='C' AND iyear=2026 AND iperiod=9" 0
    Write-Output 'PASS: Agent initialization and four query migrations each repeated twice, all 12 controlled tool permissions granted to SUPER_ADMIN, ACTIVE account sets accepted, incomplete/failed account sets excluded, fixed SQL projections, multi-company voucher filtering, and scoped statistics.'
}
finally {
    if ($created -and $schemaName.StartsWith('finex_agent_verify_')) {
        Invoke-MySql "DROP DATABASE $schemaName" | Out-Null
    }
    if ($null -eq $oldMySqlPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $oldMySqlPassword }
}
