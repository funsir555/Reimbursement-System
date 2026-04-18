# `fin_cash_flow_item` 乱码修复执行记录

## 1. 基本信息
- 执行日期：2026-04-18
- 目标库：`127.0.0.1:3306/finex_db`
- 修复脚本：`C:\Users\funsir\Desktop\报销系统\backend\sql\migrate_fix_fin_cash_flow_item_garbled_text.sql`
- 修复范围：
  - `fin_cash_flow_item.cash_flow_name`
  - `fin_cash_flow_item` 表注释
  - `fin_cash_flow_item` 各字段注释
  - `gl_accvouch.cash_flow_item_id`
  - `gl_accvouch.cash_flow_item_name`

## 2. 执行前校验
- 预检文件：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_183212_fix_fin_cash_flow_item_garbled_text\precheck.txt`
- 预检结论：
  - `fin_cash_flow_item` 当前默认 10 条现金流量名称均已损坏，`HEX(cash_flow_name)` 为 `3F...`
  - 当前受影响公司：`COMPANY202604050001`
  - `fin_cash_flow_item` 表注释当前为 `3F3F3F3F3F3F3F`
  - `fin_cash_flow_item` 各字段注释当前均为 `?` 形式的错误值
  - `gl_accvouch.cash_flow_item_id / cash_flow_item_name` 注释当前也为 `?` 形式的错误值
  - 当前现金流量总数为 `10`，去重编码数为 `10`

## 3. 备份信息
- 备份目录：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_183212_fix_fin_cash_flow_item_garbled_text`
- 备份产物：
  - `precheck.txt`
  - `show_create_fin_cash_flow_item_before.txt`
  - `show_create_gl_accvouch_before.txt`
  - `fin_cash_flow_item_rows_backup.sql`
- 备份口径：
  - 仅备份当前 10 条受影响默认现金流量行
  - 导出修复前 `fin_cash_flow_item` 与 `gl_accvouch` 结构定义，供注释回滚参考

## 4. 执行说明
- 首次编写修复脚本后，先执行了两次校验性运行：
  - `execute_first.txt`
  - `execute_second.txt`
- 随后发现脚本中的匹配条件写成了 `^3F+$`，只能匹配 `3FFFF...`，不能匹配真实受损值 `3F3F3F...`，因此首次运行未命中任何现金流量名称数据。
- 已将修复条件更正为 `^(3F)+$`，并再次执行两次：
  - `execute_fix_first.txt`
  - `execute_fix_second.txt`
- 更正后脚本成功完成以下动作：
  - 修复 `fin_cash_flow_item` 表注释
  - 修复 `fin_cash_flow_item` 字段注释
  - 修复 `gl_accvouch` 两个现金流量字段注释
  - 修复 10 条默认现金流量名称

## 5. 执行后验证
- 验证文件：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_183212_fix_fin_cash_flow_item_garbled_text\postcheck.txt`
- 验证结论：
  - `fin_cash_flow_item` 10 条默认现金流量名称已恢复为正确中文：
    - `1001` 销售商品、提供劳务收到的现金
    - `1002` 收到其他与经营活动有关的现金
    - `2001` 购买商品、接受劳务支付的现金
    - `2002` 支付给职工以及为职工支付的现金
    - `2003` 支付的各项税费
    - `2004` 支付其他与经营活动有关的现金
    - `3001` 收回投资收到的现金
    - `3002` 购建固定资产、无形资产和其他长期资产支付的现金
    - `4001` 吸收投资收到的现金
    - `4002` 偿还债务支付的现金
  - 修复后名称字节值已恢复为正确 UTF-8 十六进制，不再是 `3F...`
  - `fin_cash_flow_item` 表注释十六进制已恢复为：
    - `E78EB0E98791E6B581E9878FE6A1A3E6A188E8A1A8`
  - `fin_cash_flow_item` 字段注释十六进制均已恢复为正确中文
  - `gl_accvouch.cash_flow_item_id / cash_flow_item_name` 注释十六进制均已恢复为正确中文
  - 修复后现金流量总数仍为 `10`，去重编码数仍为 `10`

## 6. 幂等性验证
- 修复脚本在更正匹配条件后已再次重跑 1 次。
- 第二次重跑后：
  - 不新增数据
  - 不改动非目标行
  - 注释保持正确
  - 默认 10 条现金流量名称保持正确

## 7. 回滚说明
- 本次未自动回滚。
- 如需回滚：
  1. 使用 `fin_cash_flow_item_rows_backup.sql` 恢复 10 条受影响行的修复前值
  2. 参考 `show_create_fin_cash_flow_item_before.txt` 恢复 `fin_cash_flow_item` 表/列注释
  3. 参考 `show_create_gl_accvouch_before.txt` 恢复 `gl_accvouch` 相关字段注释

## 8. 结果结论
- 当前本机 `finex_db` 中现金流量名称乱码已修复完成
- 现金流量相关表注释与字段注释乱码已修复完成
- 已新增可复用修复脚本，后续同类环境可直接复用该脚本执行
