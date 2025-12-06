package com.whatsapp.android.entity.response.register;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-21 21:24
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SubmitRegisterResult extends StatusResult {
    private String username;
    private String envEncryptKey;

    public SubmitRegisterResult() {
    }

    public SubmitRegisterResult(String username, String envEncryptKey, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.username = username;
        this.envEncryptKey = envEncryptKey;
    }

    public SubmitRegisterResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
