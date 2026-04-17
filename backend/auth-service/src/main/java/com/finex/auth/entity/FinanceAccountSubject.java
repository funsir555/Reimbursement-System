package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @TableField("cclassany")
    private String cclassany;

    @TableField("bproperty")
    private Integer bproperty;

    @TableField("cbook_type")
    private String cbookType;

    private String chelp;

    @TableField("cexch_name")
    private String cexchName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cmeasure;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    @TableField(value = "cass_item", updateStrategy = FieldStrategy.ALWAYS)
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
    @JsonProperty("bd_c")
    private Integer bdC;

    private LocalDateTime dbegin;

    private LocalDateTime dend;

    private Integer itrans;

    private Integer bclose;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cother;

    private Integer iotherused;

    @TableField("bReport")
    @JsonProperty("bReport")
    private Integer bReport;

    @TableField("bGCJS")
    @JsonProperty("bGCJS")
    private Integer bGCJS;

    @TableField("bCashItem")
    @JsonProperty("bCashItem")
    private Integer bCashItem;

    @TableField("iViewItem")
    @JsonProperty("iViewItem")
    private Integer iViewItem;

    @TableField("bcDefine1")
    private Integer bcDefine1;

    @TableField("bcDefine2")
    private Integer bcDefine2;

    @TableField("bcDefine3")
    private Integer bcDefine3;

    @TableField("bcDefine4")
    private Integer bcDefine4;

    @TableField("bcDefine5")
    private Integer bcDefine5;

    @TableField("bcDefine6")
    private Integer bcDefine6;

    @TableField("bcDefine7")
    private Integer bcDefine7;

    @TableField("bcDefine8")
    private Integer bcDefine8;

    @TableField("bcDefine9")
    private Integer bcDefine9;

    @TableField("bcDefine10")
    private Integer bcDefine10;

    @TableField("bcDefine11")
    private Integer bcDefine11;

    @TableField("bcDefine12")
    private Integer bcDefine12;

    @TableField("bcDefine13")
    private Integer bcDefine13;

    @TableField("bcDefine14")
    private Integer bcDefine14;

    @TableField("bcDefine15")
    private Integer bcDefine15;

    @TableField("bcDefine16")
    private Integer bcDefine16;

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
