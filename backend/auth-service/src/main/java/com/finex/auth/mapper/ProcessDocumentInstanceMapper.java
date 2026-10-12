package com.finex.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ProcessDocumentInstanceMapper extends BaseMapper<ProcessDocumentInstance> {
    @Select("SELECT COUNT(*) AS total_count, COALESCE(SUM(total_amount), 0) AS total_amount "
            + "FROM pm_document_instance ${ew.customSqlSegment}")
    Map<String, Object> aggregateAuthorized(@Param(Constants.WRAPPER) Wrapper<ProcessDocumentInstance> wrapper);
}
