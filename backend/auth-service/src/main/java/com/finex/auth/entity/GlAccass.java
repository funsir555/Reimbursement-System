package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("gl_accass")
public class GlAccass {

    @TableId(value = "i_id", type = IdType.AUTO)
    private Integer id;

    private Integer iyear;

    private Integer iyperiod;

    private Integer iperiod;

    private String ccode;

    @TableField("cbegind_c")
    private String cbegindC;

    @TableField("cbegind_c_engl")
    private String cbegindCEngl;

    @TableField("cendd_c")
    private String cenddC;

    @TableField("cendd_c_engl")
    private String cenddCEngl;

    @TableField("cexch_name")
    private String cexchName;

    @TableField("currency_code")
    private String currencyCode;

    @TableField("company_id")
    private String companyId;

    @TableField("cdept_id")
    private String cdeptId;

    @TableField("cperson_id")
    private String cpersonId;

    @TableField("ccus_id")
    private String ccusId;

    @TableField("csup_id")
    private String csupId;

    @TableField("citem_class")
    private String citemClass;

    @TableField("citem_id")
    private String citemId;

    private BigDecimal mb;

    @TableField("mb_f")
    private BigDecimal mbF;

    private BigDecimal md;

    @TableField("md_f")
    private BigDecimal mdF;

    private BigDecimal mc;

    @TableField("mc_f")
    private BigDecimal mcF;

    private BigDecimal me;

    @TableField("me_f")
    private BigDecimal meF;

    @TableField("nb_s")
    private BigDecimal nbS;

    @TableField("nd_s")
    private BigDecimal ndS;

    @TableField("nc_s")
    private BigDecimal ncS;

    @TableField("ne_s")
    private BigDecimal neS;
}
