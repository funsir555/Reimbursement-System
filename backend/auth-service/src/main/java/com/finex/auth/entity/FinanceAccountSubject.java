package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_account_subject")
public class FinanceAccountSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("subject_code")
    private String subjectCode;

    @TableField("subject_name")
    private String subjectName;

    @TableField("parent_subject_code")
    private String parentSubjectCode;

    @TableField("subject_level")
    private Integer subjectLevel;

    @TableField("balance_direction")
    private String balanceDirection;

    @TableField("subject_category")
    private String subjectCategory;

    @TableField("leaf_flag")
    private Integer leafFlag;

    private Integer status;

    @TableField("template_code")
    private String templateCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
