package com.finex.auth.service.impl.financequery;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.service.port.FinanceReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Finance 参考数据查询适配器。
 */
@Service
@RequiredArgsConstructor
public class FinanceReferenceQueryService implements FinanceReferenceQueryPort {

    private final GlAccvouchMapper glAccvouchMapper;

    @Override
    public List<String> listDistinctAccountCodes(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return glAccvouchMapper.selectObjs(
                        Wrappers.<GlAccvouch>lambdaQuery()
                                .select(GlAccvouch::getCcode)
                                .isNotNull(GlAccvouch::getCcode)
                                .groupBy(GlAccvouch::getCcode)
                                .orderByAsc(GlAccvouch::getCcode)
                                .last("limit " + safeLimit)
                ).stream()
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(value -> String.valueOf(value).trim())
                .distinct()
                .toList();
    }
}
