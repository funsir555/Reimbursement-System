package com.finex.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finex.auth.entity.ProcessDocumentTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProcessDocumentTemplateMapper extends BaseMapper<ProcessDocumentTemplate> {

    @Select("""
            SELECT COALESCE(MAX(CAST(RIGHT(template_code, 4) AS UNSIGNED)), 0)
            FROM pm_document_template
            WHERE template_code LIKE CONCAT(#{codePrefix}, '%')
            """)
    Long selectMaxTemplateCodeValueByPrefix(@Param("codePrefix") String codePrefix);
}
