// 涓氬姟鍩燂細鎶ラ攢鍗曞綍鍏ャ€佹祦杞笌鏌ヨ
// 鏂囦欢瑙掕壊锛氶鍩熻鍒欐敮鎾戠被
// 涓婁笅娓稿叧绯伙細涓婃父閫氬父鏉ヨ嚜 鎶ラ攢鍗曢〉闈€佸鎵归〉闈€佷粯娆鹃〉闈㈠搴旂殑 Controller锛屼笅娓镐細缁х画鍗忚皟 鎶ラ攢鍗曘€佹祦绋嬭妭鐐广€侀檮浠躲€佷粯娆句笌鏍搁攢绛夋暟鎹€?
// 椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷 鍗曟嵁鐘舵€併€佸鎵归摼銆侀噾棰濈粨鏋滃拰閲嶅鎻愪氦銆?

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

/**
 * ExpenseDocumentMutationDomainSupport锛氶鍩熻鍒欐敮鎾戠被銆?
 * 鎵挎帴 鎶ラ攢鍗曞崟鎹殑鏍稿績涓氬姟瑙勫垯銆?
 * 鏀硅繖閲屾椂锛岃鐗瑰埆鍏虫敞 鍗曟嵁鐘舵€併€佸鎵归摼銆侀噾棰濈粨鏋滃拰閲嶅鎻愪氦鏄惁浼氳涓€璧峰甫鍧忋€?
 */
@Service
class ExpenseDocumentMutationDomainSupport {

    private final ExpenseDocumentSubmitBootstrapSupport submitBootstrapSupport;
    private final ExpenseDocumentMutationApplySupport mutationApplySupport;

    /**
     * 鍒濆鍖栬繖涓被鎵€闇€鐨勪緷璧栫粍浠躲€?
     */
    ExpenseDocumentMutationDomainSupport(
            ExpenseDocumentSubmitBootstrapSupport submitBootstrapSupport,
            ExpenseDocumentMutationApplySupport mutationApplySupport
    ) {
        this.submitBootstrapSupport = submitBootstrapSupport;
        this.mutationApplySupport = mutationApplySupport;
    }

    /**
     * 鎻愪氦鍗曟嵁銆?
     */
    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return submitBootstrapSupport.submitDocument(userId, username, dto);
    }

    ProcessDocumentInstance createDraftDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return submitBootstrapSupport.createDraftDocument(userId, username, dto);
    }

    ProcessDocumentInstance saveDraftDocument(Long userId, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return submitBootstrapSupport.saveDraftDocument(userId, documentCode, dto);
    }

    /**
     * 閲嶆柊鎻愪氦鍗曟嵁銆?
     */
    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return submitBootstrapSupport.resubmitDocument(userId, username, documentCode, dto);
    }

    /**
     * 缁勮鍙樻洿涓婁笅鏂囥€?
     */
    AbstractExpenseDocumentSupport.DocumentMutationContext buildMutationContext(
            ProcessDocumentInstance instance,
            ExpenseDocumentUpdateDTO dto,
            boolean resetRuntime
    ) {
        return mutationApplySupport.buildMutationContext(instance, dto, resetRuntime);
    }

    void applyDocumentMutation(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext context,
            boolean resetRuntime
    ) {
        mutationApplySupport.applyDocumentMutation(instance, context, resetRuntime);
    }
}
