package com.finex.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CodeSequenceMapper {

    @Update("""
            UPDATE pm_code_sequence
            SET current_value = LAST_INSERT_ID(current_value + 1),
                updated_at = CURRENT_TIMESTAMP
            WHERE biz_key = #{bizKey}
              AND biz_date = #{bizDate}
            """)
    int allocateNextTemplateCodeValue(
            @Param("bizKey") String bizKey,
            @Param("bizDate") String bizDate
    );

    @Insert("""
            INSERT IGNORE INTO pm_code_sequence (
                biz_key,
                biz_date,
                current_value,
                created_at,
                updated_at
            )
            VALUES (
                #{bizKey},
                #{bizDate},
                #{initialValue},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int initializeSequenceIfAbsent(
            @Param("bizKey") String bizKey,
            @Param("bizDate") String bizDate,
            @Param("initialValue") Long initialValue
    );

    @Select("SELECT LAST_INSERT_ID()")
    Long currentAllocatedValue();
}
