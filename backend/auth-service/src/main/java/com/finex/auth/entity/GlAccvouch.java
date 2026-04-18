package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gl_accvouch")
public class GlAccvouch {

    @TableId(value = "i_id", type = IdType.AUTO)
    private Integer id;

    private Integer iperiod;

    private String csign;

    private Integer isignseq;

    private Integer inoId;

    private Integer inid;

    private LocalDateTime dbillDate;

    private Integer idoc;

    private String cbill;

    private String ccheck;

    private String cbook;

    private Integer ibook;

    private Integer iflag;

    private String ctext1;

    private String ctext2;

    private String cdigest;

    private String ccode;

    private String ccodeName;

    private String cexchName;

    private BigDecimal md;

    private BigDecimal mc;

    private BigDecimal mdF;

    private BigDecimal mcF;

    private BigDecimal nfrat;

    private BigDecimal ndS;

    private BigDecimal ncS;

    private String cdeptId;

    private String cpersonId;

    private String ccusId;

    private String csupId;

    private String citemId;

    private String citemClass;

    private Long cashFlowItemId;

    private String cashFlowItemName;

    private String companyId;
}
