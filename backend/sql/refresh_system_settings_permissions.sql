USE finex_db;

SET NAMES utf8mb4;

/*
鐢ㄩ€?
1. 鎶婃潈闄愮洰褰曞埛鏂颁负鍜屽墠绔綋鍓嶅睍绀轰竴鑷寸殑涓枃缁撴瀯
2. 鍏煎娴佺▼绠＄悊閲嶆瀯鍚庣殑鑿滃崟灞傜骇
3. 纭繚 SUPER_ADMIN 鎷ユ湁鍏ㄩ儴鍚敤鏉冮檺
4. 纭繚 admin 缁戝畾 SUPER_ADMIN

璇存槑:
- 娴佺▼绠＄悊椤佃櫧鐒跺唴閮ㄩ噸鏋勪负鈥滃崟鎹笌娴佺▼ / 鑷畾涔夋。妗?/ 璐圭敤绫诲瀷鈥濓紝
  褰撳墠鍓嶅悗绔疄闄呴壌鏉冧粛浣跨敤 expense:process_management:* 杩欎竴缁勬潈闄愮爜銆?
- 鏈剼鏈负骞傜瓑鑴氭湰锛屽彲閲嶅鎵ц銆?
*/

DROP TEMPORARY TABLE IF EXISTS tmp_permission_seed;

CREATE TEMPORARY TABLE tmp_permission_seed (
    permission_code VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
    permission_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    permission_type VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    parent_code VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    module_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    route_path VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    sort_order INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_permission_seed (
    permission_code,
    permission_name,
    permission_type,
    parent_code,
    module_code,
    route_path,
    sort_order,
    status
) VALUES
    ('dashboard:menu', '棣栭〉', 'MENU', NULL, 'dashboard', '/dashboard', 10, 1),
    ('dashboard:view', '棣栭〉', 'MENU', 'dashboard:menu', 'dashboard', '/dashboard', 101, 1),

    ('profile:menu', '涓汉涓績', 'MENU', NULL, 'profile', '/profile', 20, 1),
    ('profile:view', '涓汉涓績', 'MENU', 'profile:menu', 'profile', '/profile', 201, 1),
    ('profile:password:update', '淇敼瀵嗙爜', 'BUTTON', 'profile:view', 'profile', NULL, 2011, 1),
    ('profile:downloads:view', '涓嬭浇涓績', 'BUTTON', 'profile:view', 'profile', NULL, 2012, 1),

    ('expense:menu', '鎶ラ攢绠＄悊', 'MENU', NULL, 'expense', '/expense', 30, 1),
    ('expense:create:view', '鏂板缓鎶ラ攢', 'MENU', 'expense:menu', 'expense', '/expense/create', 301, 1),
    ('expense:create:create', '鍒涘缓鎶ラ攢鍗?', 'BUTTON', 'expense:create:view', 'expense', NULL, 3011, 1),
    ('expense:create:submit', '鎻愪氦鎶ラ攢鍗?', 'BUTTON', 'expense:create:view', 'expense', NULL, 3012, 1),
    ('expense:create:save_draft', '淇濆瓨鑽夌', 'BUTTON', 'expense:create:view', 'expense', NULL, 3013, 1),
    ('expense:list:view', '鎴戠殑鎶ラ攢', 'MENU', 'expense:menu', 'expense', '/expense/list', 302, 1),
    ('expense:list:edit', '缂栬緫鎶ラ攢鍗?', 'BUTTON', 'expense:list:view', 'expense', NULL, 3021, 1),
    ('expense:list:delete', '鍒犻櫎鎶ラ攢鍗?', 'BUTTON', 'expense:list:view', 'expense', NULL, 3022, 1),
    ('expense:list:submit', '閲嶆柊鎻愪氦', 'BUTTON', 'expense:list:view', 'expense', NULL, 3023, 1),
    ('expense:approval:view', '寰呮垜瀹℃壒', 'MENU', 'expense:menu', 'expense', '/expense/approval', 303, 1),
    ('expense:approval:approve', '瀹℃壒閫氳繃', 'BUTTON', 'expense:approval:view', 'expense', NULL, 3031, 1),
    ('expense:approval:reject', '瀹℃壒椹冲洖', 'BUTTON', 'expense:approval:view', 'expense', NULL, 3032, 1),
    ('expense:payment:menu', '鏀粯', 'MENU', 'expense:menu', 'expense-payment', '/expense/payment', 304, 1),
    ('expense:payment:bank_link:view', '閾朵紒鐩磋繛', 'MENU', 'expense:payment:menu', 'expense', '/expense/payment/bank-link', 3041, 1),
    ('expense:payment:bank_link:edit', '缂栬緫鐩磋繛閰嶇疆', 'BUTTON', 'expense:payment:bank_link:view', 'expense', NULL, 30411, 1),
    ('expense:payment:bank_link:pay', '鍙戣捣鏀粯', 'BUTTON', 'expense:payment:bank_link:view', 'expense', NULL, 30412, 1),
    ('expense:payment:payment_order:view', '浠樻鍗?', 'MENU', 'expense:payment:menu', 'expense', '/expense/payment/orders', 3042, 1),
    ('expense:payment:payment_order:execute', '鎵ц浠樻', 'BUTTON', 'expense:payment:payment_order:view', 'expense', NULL, 30421, 1),
        ('expense:documents:view', '鍗曟嵁鏌ヨ', 'MENU', 'expense:menu', 'expense', '/expense/documents', 305, 1),
    ('expense:voucher_generation:view', CONVERT(0xe587ade8af81e7949fe68890 USING utf8mb4), 'MENU', 'expense:menu', 'expense', '/expense/workbench/process-management', 306, 1),
    ('expense:voucher_generation:generate', '鐢熸垚鍑瘉', 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3061, 1),
    ('expense:voucher_generation:mapping:view', CONVERT(0xe587ade8af81e7a791e79baee698a0e5b084 USING utf8mb4), 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3062, 1),
    ('expense:voucher_generation:mapping:edit', CONVERT(0xe7bc96e8be91e587ade8af81e7a791e79baee698a0e5b084 USING utf8mb4), 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3063, 1),
    ('expense:voucher_generation:push:view', CONVERT(0xe68ea8e98081e587ade8af81 USING utf8mb4), 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3064, 1),
    ('expense:voucher_generation:push:execute', CONVERT(0xe689a7e8a18ce587ade8af81e68ea8e98081 USING utf8mb4), 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3065, 1),
    ('expense:voucher_generation:query:view', CONVERT(0xe587ade8af81e69fa5e8afa2 USING utf8mb4), 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3066, 1),
    ('expense:workbench:menu', '绠＄悊宸ヤ綔鍙?', 'MENU', 'expense:menu', 'expense-workbench', '/expense/workbench', 307, 1),
    ('expense:process_management:view', '娴佺▼绠＄悊', 'MENU', 'expense:workbench:menu', 'expense', '/expense/workbench/process-management', 3071, 1),
    ('expense:process_management:create', '鏂板娴佺▼閰嶇疆', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30711, 1),
    ('expense:process_management:edit', '缂栬緫娴佺▼閰嶇疆', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30712, 1),
    ('expense:process_management:publish', '鍙戝竷娴佺▼閰嶇疆', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30713, 1),
    ('expense:process_management:disable', '鍋滅敤娴佺▼閰嶇疆', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30714, 1),
    ('expense:budget_management:view', '棰勭畻绠＄悊', 'MENU', 'expense:workbench:menu', 'expense', '/expense/workbench/budget-management', 3072, 1),

    ('finance:menu', '璐㈠姟绠＄悊', 'MENU', NULL, 'finance', '/finance', 40, 1),
    ('finance:general_ledger:menu', '鎬昏处', 'MENU', 'finance:menu', 'finance-general-ledger', '/finance/general-ledger', 401, 1),
    ('finance:general_ledger:new_voucher:view', '鏂板缓鍑瘉', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/new-voucher', 4011, 1),
    ('finance:general_ledger:new_voucher:create', '鏂板鍑瘉', 'BUTTON', 'finance:general_ledger:new_voucher:view', 'finance', NULL, 40111, 1),
    ('finance:general_ledger:query_voucher:view', '鏌ヨ鍑瘉', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/query-voucher', 4012, 1),
    ('finance:general_ledger:query_voucher:export', '瀵煎嚭鍑瘉', 'BUTTON', 'finance:general_ledger:query_voucher:view', 'finance', NULL, 40121, 1),
    ('finance:general_ledger:query_voucher:edit', '淇敼鍑瘉', 'BUTTON', 'finance:general_ledger:query_voucher:view', 'finance', NULL, 40122, 1),
    ('finance:general_ledger:review_voucher:view', '瀹℃牳鍑瘉', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/review-voucher', 4013, 1),
    ('finance:general_ledger:review_voucher:review', '瀹℃牳閫氳繃', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40131, 1),
    ('finance:general_ledger:review_voucher:unreview', '鍙栨秷瀹℃牳', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40132, 1),
    ('finance:general_ledger:balance_sheet:view', '鎬昏处浣欓琛?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/balance-sheet', 4014, 1),
    ('finance:general_ledger:balance_sheet:export', '瀵煎嚭鎬昏处浣欓琛?', 'BUTTON', 'finance:general_ledger:balance_sheet:view', 'finance', NULL, 40141, 1),
    ('finance:general_ledger:detail_ledger:view', '鏄庣粏璐?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/detail-ledger', 4015, 1),
    ('finance:general_ledger:general_ledger:view', '鎬诲垎绫昏处', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/general-ledger', 4016, 1),
    ('finance:general_ledger:project_detail_ledger:view', '椤圭洰鏄庣粏璐?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/project-detail-ledger', 4017, 1),
    ('finance:general_ledger:supplier_detail_ledger:view', '渚涘簲鍟嗘槑缁嗚处', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/supplier-detail-ledger', 4018, 1),
    ('finance:general_ledger:customer_detail_ledger:view', '瀹㈡埛鏄庣粏璐?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/customer-detail-ledger', 4019, 1),
    ('finance:general_ledger:personal_detail_ledger:view', '涓汉鏄庣粏璐?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/personal-detail-ledger', 4020, 1),
    ('finance:general_ledger:quantity_amount_detail_ledger:view', '鏁伴噺閲戦鏄庣粏璐?', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/quantity-amount-detail-ledger', 4021, 1),
    ('finance:fixed_assets:view', '鍥哄畾璧勪骇', 'MENU', 'finance:menu', 'finance', '/finance/fixed-assets', 402, 1),
('finance:fixed_assets:create', '鏂板鍥哄畾璧勪骇', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4021, 1),
('finance:fixed_assets:edit', '缂栬緫鍥哄畾璧勪骇', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4022, 1),
('finance:fixed_assets:delete', '鍒犻櫎鍥哄畾璧勪骇', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4023, 1),
('finance:fixed_assets:import', '鍥哄畾璧勪骇鏈熷垵瀵煎叆', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4024, 1),
('finance:fixed_assets:change', '鍥哄畾璧勪骇鍙樺姩', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4025, 1),
('finance:fixed_assets:depreciate', '鍥哄畾璧勪骇鎶樻棫', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4026, 1),
('finance:fixed_assets:dispose', '鍥哄畾璧勪骇澶勭疆', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4027, 1),
('finance:fixed_assets:close_period', '鍥哄畾璧勪骇鏈熼棿缁撹处', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4028, 1),
('finance:fixed_assets:view_voucher_link', '鍥哄畾璧勪骇鍑瘉鑱旀煡', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4029, 1),
    ('finance:reports:menu', '璐㈠姟鎶ヨ〃', 'MENU', 'finance:menu', 'finance-reports', '/finance/reports', 403, 1),
    ('finance:reports:balance_sheet:view', '璧勪骇璐熷€鸿〃', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/balance-sheet', 4031, 1),
    ('finance:reports:balance_sheet:export', '瀵煎嚭璧勪骇璐熷€鸿〃', 'BUTTON', 'finance:reports:balance_sheet:view', 'finance', NULL, 40311, 1),
    ('finance:reports:income_statement:view', '鍒╂鼎琛?', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/income-statement', 4032, 1),
    ('finance:reports:income_statement:export', '瀵煎嚭鍒╂鼎琛?', 'BUTTON', 'finance:reports:income_statement:view', 'finance', NULL, 40321, 1),
    ('finance:reports:cash_flow:view', '鐜伴噾娴侀噺琛?', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/cash-flow', 4033, 1),
    ('finance:reports:cash_flow:export', '瀵煎嚭鐜伴噾娴侀噺琛?', 'BUTTON', 'finance:reports:cash_flow:view', 'finance', NULL, 40331, 1),
    ('finance:archives:menu', '浼氳妗ｆ', 'MENU', 'finance:menu', 'finance-archives', '/finance/archives', 404, 1),
    ('finance:archives:customers:view', '瀹㈡埛妗ｆ', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/customers', 4041, 1),
    ('finance:archives:customers:create', '鏂板瀹㈡埛妗ｆ', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40411, 1),
    ('finance:archives:customers:edit', '缂栬緫瀹㈡埛妗ｆ', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40412, 1),
    ('finance:archives:customers:delete', '鍒犻櫎瀹㈡埛妗ｆ', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40413, 1),
    ('finance:archives:customers:import', '瀵煎叆瀹㈡埛妗ｆ', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40414, 1),
    ('finance:archives:customers:export', '瀵煎嚭瀹㈡埛妗ｆ', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40415, 1),
    ('finance:archives:suppliers:view', '渚涘簲鍟嗘。妗?', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/suppliers', 4042, 1),
    ('finance:archives:suppliers:create', '鏂板渚涘簲鍟嗘。妗?', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40421, 1),
    ('finance:archives:suppliers:edit', '缂栬緫渚涘簲鍟嗘。妗?', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40422, 1),
    ('finance:archives:suppliers:delete', '鍒犻櫎渚涘簲鍟嗘。妗?', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40423, 1),
    ('finance:archives:suppliers:import', '瀵煎叆渚涘簲鍟嗘。妗?', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40424, 1),
    ('finance:archives:suppliers:export', '瀵煎嚭渚涘簲鍟嗘。妗?', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40425, 1),
    ('finance:archives:employees:view', '鍛樺伐妗ｆ', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/employees', 4043, 1),
    ('finance:archives:employees:create', '鏂板鍛樺伐妗ｆ', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40431, 1),
    ('finance:archives:employees:edit', '缂栬緫鍛樺伐妗ｆ', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40432, 1),
    ('finance:archives:employees:delete', '鍒犻櫎鍛樺伐妗ｆ', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40433, 1),
    ('finance:archives:employees:import', '瀵煎叆鍛樺伐妗ｆ', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40434, 1),
    ('finance:archives:employees:export', '瀵煎嚭鍛樺伐妗ｆ', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40435, 1),
    ('finance:archives:departments:view', '閮ㄩ棬妗ｆ', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/departments', 4044, 1),
    ('finance:archives:departments:create', '鏂板閮ㄩ棬妗ｆ', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40441, 1),
    ('finance:archives:departments:edit', '缂栬緫閮ㄩ棬妗ｆ', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40442, 1),
    ('finance:archives:departments:delete', '鍒犻櫎閮ㄩ棬妗ｆ', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40443, 1),
    ('finance:archives:departments:import', '瀵煎叆閮ㄩ棬妗ｆ', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40444, 1),
    ('finance:archives:departments:export', '瀵煎嚭閮ㄩ棬妗ｆ', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40445, 1),
    ('finance:archives:account_subjects:view', '浼氳绉戠洰', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/account-subjects', 4045, 1),
    ('finance:archives:account_subjects:create', '鏂板浼氳绉戠洰', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40451, 1),
    ('finance:archives:account_subjects:edit', '缂栬緫浼氳绉戠洰', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40452, 1),
    ('finance:archives:account_subjects:disable', '鍚仠浼氳绉戠洰', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40453, 1),
    ('finance:archives:account_subjects:close', '灏佸瓨浼氳绉戠洰', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40454, 1),
    ('finance:archives:projects:view', '椤圭洰妗ｆ', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/projects', 4046, 1),
    ('finance:archives:projects:create', '??????', 'BUTTON', 'finance:archives:projects:view', 'finance', NULL, 40461, 1),
    ('finance:archives:projects:edit', '??????', 'BUTTON', 'finance:archives:projects:view', 'finance', NULL, 40462, 1),
    ('finance:archives:projects:disable', '??????', 'BUTTON', 'finance:archives:projects:view', 'finance', NULL, 40463, 1),
    ('finance:archives:projects:close', '??????', 'BUTTON', 'finance:archives:projects:view', 'finance', NULL, 40464, 1),
    ('finance:system_management:view', '璐㈠姟绯荤粺绠＄悊', 'MENU', 'finance:menu', 'finance', '/finance/system-management', 405, 1),
    ('finance:system_management:create', '鏂板缓璐﹀', 'BUTTON', 'finance:system_management:view', 'finance', NULL, 4051, 1),
    ('finance:system_management:task:view', '鏌ョ湅浠诲姟', 'BUTTON', 'finance:system_management:view', 'finance', NULL, 4052, 1),

    ('archives:menu', '鐢靛瓙妗ｆ', 'MENU', NULL, 'archives', '/archives', 50, 1),
    ('archives:invoices:view', '鍙戠エ绠＄悊', 'MENU', 'archives:menu', 'archives', '/archives/invoices', 501, 1),
    ('archives:invoices:upload', '涓婁紶鍙戠エ', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5011, 1),
    ('archives:invoices:export', '瀵煎嚭鍙戠エ', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5012, 1),
    ('archives:invoices:verify', '鍙戠エ楠岀湡', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5013, 1),
    ('archives:invoices:ocr', '鍙戠エ璇嗗埆', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5014, 1),
    ('archives:invoices:delete', '鍒犻櫎鍙戠エ', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5015, 1),
    ('archives:account_books:view', '璐﹀绠＄悊', 'MENU', 'archives:menu', 'archives', '/archives/account-books', 502, 1),
    ('archives:account_books:create', '鏂板璐﹀', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5021, 1),
    ('archives:account_books:edit', '缂栬緫璐﹀', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5022, 1),
    ('archives:account_books:delete', '鍒犻櫎璐﹀', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5023, 1),

    ('agents:menu', 'Agent', 'MENU', NULL, 'agents', '/archives/agents', 55, 1),
    ('agents:view', CONVERT(0x4167656e74e5b7a5e4bd9ce58fb0 USING utf8mb4), 'MENU', 'agents:menu', 'agents', '/archives/agents', 551, 1),
    ('agents:create', CONVERT(0xe696b0e5bbba204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5511, 1),
    ('agents:edit', CONVERT(0xe7bc96e8be91204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5512, 1),
    ('agents:delete', CONVERT(0xe588a0e999a4204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5513, 1),
    ('agents:run', CONVERT(0xe8bf90e8a18c204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5514, 1),
    ('agents:publish', CONVERT(0xe58f91e5b883204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5515, 1),
    ('agents:view_logs', CONVERT(0xe69fa5e79c8be8bf90e8a18ce697a5e5bf97 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5516, 1),

    ('settings:menu', '绯荤粺璁剧疆', 'MENU', NULL, 'settings', '/settings', 60, 1),
    ('settings:organization:view', '缁勭粐鏋舵瀯', 'MENU', 'settings:menu', 'organization', '/settings?tab=organization', 601, 1),
    ('settings:organization:create', '鏂板閮ㄩ棬', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6011, 1),
    ('settings:organization:edit', '缂栬緫閮ㄩ棬', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6012, 1),
    ('settings:organization:delete', '鍒犻櫎閮ㄩ棬', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6013, 1),
    ('settings:organization:sync_config', '閰嶇疆鍚屾', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6014, 1),
    ('settings:organization:run_sync', '鎵嬪姩鍚屾', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6015, 1),
    ('settings:employees:view', '鍛樺伐绠＄悊', 'MENU', 'settings:menu', 'employees', '/settings?tab=employees', 602, 1),
    ('settings:employees:create', '鏂板鍛樺伐', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6021, 1),
    ('settings:employees:edit', '缂栬緫鍛樺伐', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6022, 1),
    ('settings:employees:delete', '鍒犻櫎鍛樺伐', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6023, 1),
    ('settings:roles:view', '鏉冮檺绠＄悊', 'MENU', 'settings:menu', 'roles', '/settings?tab=roles', 603, 1),
    ('settings:roles:create', '鏂板瑙掕壊', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6031, 1),
    ('settings:roles:edit', '缂栬緫瑙掕壊', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6032, 1),
    ('settings:roles:delete', '鍒犻櫎瑙掕壊', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6033, 1),
    ('settings:roles:assign_permissions', '鍒嗛厤鏉冮檺', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6034, 1),
    ('settings:roles:assign_users', '鍒嗛厤鐢ㄦ埛', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6035, 1),
    ('settings:companies:view', CONVERT(0xe585ace58fb8e7aea1e79086 USING utf8mb4), 'MENU', 'settings:menu', 'companies', '/settings?tab=companies', 604, 1),
    ('settings:companies:create', CONVERT(0xe696b0e5a29ee585ace58fb8 USING utf8mb4), 'BUTTON', 'settings:companies:view', 'companies', NULL, 6041, 1),
    ('settings:companies:edit', CONVERT(0xe7bc96e8be91e585ace58fb8 USING utf8mb4), 'BUTTON', 'settings:companies:view', 'companies', NULL, 6042, 1),
    ('settings:companies:delete', CONVERT(0xe588a0e999a4e585ace58fb8 USING utf8mb4), 'BUTTON', 'settings:companies:view', 'companies', NULL, 6043, 1),
    ('settings:company_accounts:view', CONVERT(0xe585ace58fb8e8b4a6e688b7e7aea1e79086 USING utf8mb4), 'MENU', 'settings:menu', 'companyAccounts', '/settings?tab=companyAccounts', 605, 1),
    ('settings:company_accounts:create', CONVERT(0xe696b0e5a29ee585ace58fb8e8b4a6e688b7 USING utf8mb4), 'BUTTON', 'settings:company_accounts:view', 'companyAccounts', NULL, 6051, 1),
    ('settings:company_accounts:edit', CONVERT(0xe7bc96e8be91e585ace58fb8e8b4a6e688b7 USING utf8mb4), 'BUTTON', 'settings:company_accounts:view', 'companyAccounts', NULL, 6052, 1),
    ('settings:company_accounts:delete', CONVERT(0xe588a0e999a4e585ace58fb8e8b4a6e688b7 USING utf8mb4), 'BUTTON', 'settings:company_accounts:view', 'companyAccounts', NULL, 6053, 1);

INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT
    seed.permission_code,
    seed.permission_name,
    seed.permission_type,
    NULL,
    seed.module_code,
    seed.route_path,
    seed.sort_order,
    seed.status
FROM tmp_permission_seed seed
LEFT JOIN sys_permission permission
    ON permission.permission_code COLLATE utf8mb4_unicode_ci = seed.permission_code COLLATE utf8mb4_unicode_ci
WHERE permission.id IS NULL;

UPDATE sys_permission permission
JOIN tmp_permission_seed seed
    ON seed.permission_code COLLATE utf8mb4_unicode_ci = permission.permission_code COLLATE utf8mb4_unicode_ci
SET permission.permission_name = seed.permission_name,
    permission.permission_type = seed.permission_type,
    permission.module_code = seed.module_code,
    permission.route_path = seed.route_path,
    permission.sort_order = seed.sort_order,
    permission.status = seed.status;

UPDATE sys_permission child
JOIN tmp_permission_seed seed
    ON seed.permission_code COLLATE utf8mb4_unicode_ci = child.permission_code COLLATE utf8mb4_unicode_ci
LEFT JOIN sys_permission parent
    ON parent.permission_code COLLATE utf8mb4_unicode_ci = seed.parent_code COLLATE utf8mb4_unicode_ci
SET child.parent_id = parent.id;

INSERT INTO sys_role (role_code, role_name, role_description, status)
SELECT 'SUPER_ADMIN', '瓒呯骇绠＄悊鍛?', '鎷ユ湁绯荤粺鍏ㄩ儴鍚敤鏉冮檺', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

UPDATE sys_role
SET role_name = '瓒呯骇绠＄悊鍛?',
    role_description = '鎷ユ湁绯荤粺鍏ㄩ儴鍚敤鏉冮檺',
    status = 1
WHERE role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.status = 1
WHERE role.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.id, role.id
FROM sys_user user
JOIN sys_role role ON role.role_code = 'SUPER_ADMIN'
WHERE user.username = 'admin';

SELECT permission_code, permission_name, permission_type, sort_order
FROM sys_permission
WHERE permission_code IN (
    'dashboard:menu',
    'profile:menu',
    'expense:menu',
    'expense:payment:menu',
    'expense:workbench:menu',
    'finance:menu',
    'finance:general_ledger:menu',
    'finance:reports:menu',
    'finance:archives:menu',
    'archives:menu',
    'agents:menu',
    'settings:menu'
)
ORDER BY sort_order, permission_code;

DROP TEMPORARY TABLE IF EXISTS tmp_permission_seed;
