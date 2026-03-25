package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class DownloadCenterVO {

    private List<DownloadRecordVO> inProgress;

    private List<DownloadRecordVO> history;
}
