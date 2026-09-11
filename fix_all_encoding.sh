#!/bin/bash
# 完整修复所有凭证生成模块的乱码问题

BASE_DIR="backend/auth-service/src/main/java/com/finex/auth/service/impl/expensevoucher"

# 修复 AbstractExpenseVoucherGenerationSupport.java
FILE="$BASE_DIR/AbstractExpenseVoucherGenerationSupport.java"
sed -i 's/褰撳墠鍏徃鍜屾姤閿€妯℃澘鏈厤缃粺涓€璐锋柟绉戠洰绛栫暐/当前公司和报销模板未配置统一贷方科目策略/g' "$FILE"
sed -i 's/浼氳绉戠洰/会计科目/g' "$FILE"
sed -i 's/鍗曟嵁鏈瘑鍒埌浠樻鍏徃锛屾棤娉曟帹閫佸嚟璇?/单据未识别到付款公司，无法推送凭证/g' "$FILE"
sed -i 's/鍏徃涓嶅瓨鍦紝鏃犳硶淇濆瓨妯℃澘绉戠洰绛栫暐/公司不存在，无法保存模板科目策略/g' "$FILE"
sed -i 's/鎶ラ攢妯℃澘涓嶅瓨鍦紝鏃犳硶淇濆瓨妯℃澘绉戠洰绛栫暐/报销模板不存在，无法保存模板科目策略/g' "$FILE"
sed -i 's/鍚屼竴鍏徃鍜屾姤閿€妯℃澘鍙兘缁存姢涓€濂楃粺涓€璐锋柟绛栫暐/同一公司和报销模板只能维护一套统一贷方策略/g' "$FILE"
sed -i 's/鍏徃涓嶅瓨鍦紝鏃犳硶淇濆瓨璐圭敤绫诲瀷鏄犲皠/公司不存在，无法保存费用类型映射/g' "$FILE"
sed -i 's/鎶ラ攢妯℃澘涓嶅瓨鍦紝鏃犳硶淇濆瓨璐圭敤绫诲瀷鏄犲皠/报销模板不存在，无法保存费用类型映射/g' "$FILE"
sed -i 's/璐圭敤绫诲瀷涓嶅瓨鍦紝鏃犳硶淇濆瓨绉戠洰鏄犲皠/费用类型不存在，无法保存科目映射/g' "$FILE"
sed -i 's/妯℃澘绉戠洰绛栫暐涓嶅瓨鍦?/模板科目策略不存在/g' "$FILE"
sed -i 's/璐圭敤绫诲瀷绉戠洰鏄犲皠涓嶅瓨鍦?/费用类型科目映射不存在/g' "$FILE"
sed -i 's/鎺ㄩ€佸け璐?/推送失败/g' "$FILE"
sed -i 's/鎺ㄩ€佹垚鍔?/推送成功/g' "$FILE"

# 修复 ExpenseVoucherPushDomainSupport.java
FILE="$BASE_DIR/ExpenseVoucherPushDomainSupport.java"
sed -i 's/涓氬姟鍩燂細鎶ラ攢鍑瘉鐢熸垚涓庢帹閫?/业务域：报销凭证生成与推送/g' "$FILE"
sed -i 's/鏂囦欢瑙掕壊锛氶鍩熻鍒欐敮鎾戠被/文件角色：领域规则支撑类/g' "$FILE"
sed -i 's/涓婁笅娓稿叧绯伙細涓婃父閫氬父鏉ヨ嚜 鎶ラ攢鍗曞嚟璇佺敓鎴愭帴鍙ｅ拰璐㈠姟鎿嶄綔鍏ュ彛锛屼笅娓镐細缁х画鍗忚皟 鍑瘉鏄犲皠銆佹帹閫佽褰曞拰鎶ラ攢鍗曞嚟璇佺姸鎬併€?/上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。/g' "$FILE"
sed -i 's/椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷淬€?/风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。/g' "$FILE"
sed -i 's/鏀硅繖閲屾椂锛岃鐗瑰埆鍏虫敞 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷存槸鍚︿細琚竴璧峰甫鍧忋€?/改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。/g' "$FILE"
sed -i 's/鍒濆鍖栬繖涓被鎵€闇€鐨勪緷璧栫粍浠躲€?/初始化这个类所需的依赖组件。/g' "$FILE"
sed -i 's/鑾峰彇鎺ㄩ€佸崟鎹€?/获取推送单据。/g' "$FILE"
sed -i 's/鎺ㄩ€佸崟鎹€?/推送单据。/g' "$FILE"
sed -i 's/鐠囩兘鈧瀚ㄩ棁鈧憰浣瑰腹闁胶娈戦崡鏇熷祦/单据编码列表不能为空/g' "$FILE"
sed -i 's/閸楁洘宓佹稉宥呯摠閸︻煉绱濋弮鐘崇《閹恒劑鈧礁鍤熺拠?/报销单不存在或未完成审批/g' "$FILE"
sed -i 's/閹恒劑鈧礁銇戠拹?/推送失败/g' "$FILE"
sed -i 's/褰撳墠鍗曟嵁娌℃湁鍙敓鎴愬嚟璇佺殑璐圭敤鏄庣粏/当前单据没有可生成凭证的费用明细/g' "$FILE"
sed -i 's/鏈厤缃垂鐢ㄧ被鍨嬪搴旂殑浼氳绉戠洰/未配置费用类型对应的会计科目/g' "$FILE"
sed -i 's/缁勮鍑瘉SaveDTO銆?/组装凭证SaveDTO。/g' "$FILE"
sed -i 's/淇濆瓨Success鎺ㄩ€佸崟鎹€?/保存Success推送单据。/g' "$FILE"
sed -i 's/淇濆瓨Failed鎺ㄩ€佸崟鎹€?/保存Failed推送单据。/g' "$FILE"
sed -i 's/鍒涘缓Batch涓婁笅鏂囥€?/创建Batch上下文。/g' "$FILE"

# 修复 ExpenseVoucherRecordQuerySupport.java
FILE="$BASE_DIR/ExpenseVoucherRecordQuerySupport.java"
sed -i 's/鎺ㄩ€佽褰曚笉瀛樺湪/推送记录不存在/g' "$FILE"

echo "所有凭证生成模块乱码修复完成"
