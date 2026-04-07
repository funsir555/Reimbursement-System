package com.finex.auth.service;

import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import org.springframework.core.io.Resource;

public interface UserCenterService {

    PersonalCenterVO getPersonalCenter(Long userId);

    DownloadCenterVO getDownloadCenter(Long userId);

    DownloadContent loadDownloadContent(Long userId, Long downloadId);

    void changePassword(Long userId, ChangePasswordDTO dto);

    record DownloadContent(Resource resource, String fileName, long contentLength) {
    }
}
