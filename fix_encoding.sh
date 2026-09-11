#!/bin/bash
# 修复 ExpenseVoucherPushDomainSupport.java 中的乱码

FILE="backend/auth-service/src/main/java/com/finex/auth/service/impl/expensevoucher/ExpenseVoucherPushDomainSupport.java"

sed -i 's/涓氬姟鍩燂細鎶ラ攢鍑瘉鐢熸垚涓庢帹閫?$/业务域：报销凭证生成与推送/g' "$FILE"
sed -i 's/鏂囦欢瑙掕壊锛氶鍩熻鍒欐敮鎾戠被$/文件角色：领域规则支撑类/g' "$FILE"
sed -i 's/涓婁笅娓稿叧绯伙細涓婃父閫氬父鏉ヨ嚜 鎶ラ攢鍗曞嚟璇佺敓鎴愭帴鍙ｅ拰璐㈠姟鎿嶄綔鍏ュ彛锛屼笅娓镐細缁х画鍗忚皟 鍑瘉鏄犲皠銆佹帹閫佽褰曞拰鎶ラ攢鍗曞嚟璇佺姸鎬併€?$/上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。/g' "$FILE"
sed -i 's/椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷淬€?$/风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。/g' "$FILE"
sed -i 's/ExpenseVoucherPushDomainSupport锛氶鍩熻鍒欐敮鎾戠被銆?$/ExpenseVoucherPushDomainSupport：领域规则支撑类。/g' "$FILE"
sed -i 's/鎵挎帴 鎶ラ攢鍗曞嚟璇佹帹閫佺殑鏍稿績涓氬姟瑙勫垯銆?$/承接 报销单凭证推送的核心业务规则。/g' "$FILE"
sed -i 's/鏀硅繖閲屾椂锛岃鐗瑰埆鍏虫敞 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷存槸鍚︿細琚竴璧峰甫鍧忋€?$/改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。/g' "$FILE"
sed -i 's/鍒濆鍖栬繖涓被鎵€闇€鐨勪緷璧栫粍浠躲€?$/初始化这个类所需的依赖组件。/g' "$FILE"
sed -i 's/鑾峰彇鎺ㄩ€佸崟鎹€?$/获取推送单据。/g' "$FILE"
sed -i 's/鎺ㄩ€佸崟鎹€?$/推送单据。/g' "$FILE"
sed -i 's/鐠囩兘鈧瀚ㄩ棁鈧憰浣瑰腹闁胶娈戦崡鏇熷祦/单据编码列表不能为空/g' "$FILE"
sed -i 's/閸楁洘宓佹稉宥呯摠閸︻煉绱濋弮鐘崇《閹恒劑鈧礁鍤熺拠?/报销单不存在或未完成审批/g' "$FILE"
sed -i 's/閹恒劑鈧礁銇戠拹?/推送失败/g' "$FILE"
sed -i 's/鎺ㄩ€丱ne鍗曟嵁銆?$/推送One单据。/g' "$FILE"
sed -i 's/褰撳墠鍗曟嵁娌℃湁鍙敓鎴愬嚟璇佺殑璐圭敤鏄庣粏/当前单据没有可生成凭证的费用明细/g' "$FILE"
sed -i 's/鏈厤缃垂鐢ㄧ被鍨嬪搴旂殑浼氳绉戠洰/未配置费用类型对应的会计科目/g' "$FILE"
sed -i 's/缁勮鍑瘉SaveDTO銆?$/组装凭证SaveDTO。/g' "$FILE"
sed -i 's/閹躲儵鏀㈤崙顓＄槈/报销单/g' "$FILE"
sed -i 's/闁炬儼顢戠粔鎴犳窗/报销汇总/g' "$FILE"
sed -i 's/淇濆瓨Success鎺ㄩ€佸崟鎹€?$/保存Success推送单据。/g' "$FILE"
sed -i 's/淇濆瓨Failed鎺ㄩ€佸崟鎹€?$/保存Failed推送单据。/g' "$FILE"

echo "ExpenseVoucherPushDomainSupport.java 修复完成"
