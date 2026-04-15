// 业务域：财务系统管理
// 文件角色：service 接口
// 上下游关系：上游通常来自 财务系统设置和账套相关接口，下游会继续协调 账套、同步任务和财务上下文基础数据。
// 风险提醒：改坏后最容易影响 账套切换、基础数据同步和下游系统连接。

package com.finex.auth.service;

import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;

import java.util.List;

/**
 * FinanceSystemManagementService：service 接口。
 * 定义财务系统管理这块对外提供的业务入口能力。
 * 改这里时，要特别关注 账套切换、基础数据同步和下游系统连接是否会被一起带坏。
 */
public interface FinanceSystemManagementService {

    /**
     * 获取元数据。
     */
    FinanceAccountSetMetaVO getMeta();

    /**
     * 查询账户Sets列表。
     */
    List<FinanceAccountSetSummaryVO> listAccountSets();

    /**
     * 提交创建任务。
     */
    FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto);

    /**
     * 获取任务Status。
     */
    FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo);
}
