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

    @TableField("cclassany")
    private String cclassany;

    @TableField("bproperty")
    private Integer bproperty;

    @TableField("cbook_type")
    private String cbookType;

    private String chelp;

    @TableField("cexch_name")
    private String cexchName;

    private String cmeasure;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    @TableField("cass_item")
    private String cassItem;

    private Integer br;

    private Integer be;

    private String cgather;

    @TableField("leaf_flag")
    private Integer leafFlag;

    private Integer bexchange;

    private Integer bcash;

    private Integer bbank;

    private Integer bused;

    @TableField("bd_c")
    private Integer bdC;

    private LocalDateTime dbegin;

    private LocalDateTime dend;

    private Integer itrans;

    private Integer bclose;

    private String cother;

    private Integer iotherused;

    @TableField("bReport")
    private Integer bReport;

    @TableField("bGCJS")
    private Integer bGCJS;

    @TableField("bCashItem")
    private Integer bCashItem;

    @TableField("iViewItem")
    private Integer iViewItem;

    private Integer bcDefine1;

    private Integer bcDefine2;

    private Integer bcDefine3;

    private Integer bcDefine4;

    private Integer bcDefine5;

    private Integer bcDefine6;

    private Integer bcDefine7;

    private Integer bcDefine8;

    private Integer bcDefine9;

    private Integer bcDefine10;

    private Integer bcDefine11;

    private Integer bcDefine12;

    private Integer bcDefine13;

    private Integer bcDefine14;

    private Integer bcDefine15;

    private Integer bcDefine16;

    @TableField("sort_order")
    private Integer sortOrder;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
