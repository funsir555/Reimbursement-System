package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_account_set_template_subject")
public class FinanceAccountSetTemplateSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_code")
    private String templateCode;

    @TableField("subject_key")
    private String subjectKey;

    @TableField("parent_subject_key")
    private String parentSubjectKey;

    @TableField("subject_level")
    private Integer subjectLevel;

    @TableField("level_segment")
    private String levelSegment;

    @TableField("subject_name")
    private String subjectName;

    @TableField("balance_direction")
    private String balanceDirection;

    @TableField("subject_category")
    private String subjectCategory;

    @TableField("leaf_flag")
    private Integer leafFlag;

    @TableField("sort_order")
    private Integer sortOrder;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
