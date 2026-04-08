package com.finex.auth.service;

import com.finex.auth.dto.BankAccountVO;
import com.finex.auth.dto.ChangePasswordDTO;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.PersonalCenterVO;
import com.finex.auth.dto.UserBankAccountSaveDTO;
import org.springframework.core.io.Resource;

import java.util.List;

public interface UserCenterService {

    PersonalCenterVO getPersonalCenter(Long userId);

    List<BankAccountVO> listBankAccounts(Long userId);

    BankAccountVO createBankAccount(Long userId, UserBankAccountSaveDTO dto);

    BankAccountVO updateBankAccount(Long userId, Long accountId, UserBankAccountSaveDTO dto);

    Boolean updateBankAccountStatus(Long userId, Long accountId, Integer status);

    Boolean setDefaultBankAccount(Long userId, Long accountId);

    DownloadCenterVO getDownloadCenter(Long userId);

    DownloadContent loadDownloadContent(Long userId, Long downloadId);

    void changePassword(Long userId, ChangePasswordDTO dto);

    record DownloadContent(Resource resource, String fileName, long contentLength) {
    }
}
