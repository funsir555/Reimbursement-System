// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceContextService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.voucher.VoucherContextSupport;
import org.springframework.stereotype.Service;

/**
 * FinanceContextServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
@Service
public class FinanceContextServiceImpl implements FinanceContextService {

    private final VoucherContextSupport voucherContextSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceContextServiceImpl(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            UserService userService
    ) {
        this.voucherContextSupport = new VoucherContextSupport(systemCompanyMapper, financeAccountSetMapper, userService);
    }

    /**
     * 获取元数据。
     */
    @Override
    public FinanceContextMetaVO getMeta(Long currentUserId) {
        return voucherContextSupport.getMeta(currentUserId);
    }
}
