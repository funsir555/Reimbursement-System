package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "褰撳墠瀵嗙爜涓嶈兘涓虹┖")
    private String currentPassword;

    @NotBlank(message = "鏂板瘑鐮佷笉鑳戒负绌?")
    private String newPassword;

    @NotBlank(message = "纭瀵嗙爜涓嶈兘涓虹┖")
    private String confirmPassword;
}
