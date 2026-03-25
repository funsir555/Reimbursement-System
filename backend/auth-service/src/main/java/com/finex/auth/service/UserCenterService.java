package com.finex.auth.service;

import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;

public interface UserCenterService {

    PersonalCenterVO getPersonalCenter(Long userId);

    DownloadCenterVO getDownloadCenter(Long userId);

    void changePassword(Long userId, ChangePasswordDTO dto);
}
