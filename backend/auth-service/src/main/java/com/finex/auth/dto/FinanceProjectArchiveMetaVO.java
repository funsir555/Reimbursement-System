package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceProjectArchiveMetaVO {

    private List<FinanceProjectArchiveOptionVO> statusOptions = new ArrayList<>();

    private List<FinanceProjectArchiveOptionVO> closeStatusOptions = new ArrayList<>();

    private List<FinanceProjectArchiveOptionVO> projectClassOptions = new ArrayList<>();
}
