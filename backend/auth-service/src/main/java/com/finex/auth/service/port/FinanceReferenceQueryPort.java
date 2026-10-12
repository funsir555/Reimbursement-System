package com.finex.auth.service.port;

import java.util.List;

/**
 * Finance 向其他业务域暴露的只读参考数据端口。
 */
public interface FinanceReferenceQueryPort {

    List<String> listDistinctAccountCodes(int limit);
}
