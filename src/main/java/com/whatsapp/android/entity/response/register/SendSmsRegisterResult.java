package com.whatsapp.android.entity.response.register;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-20 12:03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SendSmsRegisterResult extends StatusResult {
    /**
     * 注册key
     */
    private String username;
    private String registerKey;
    private Object data;
    private String envEncryptKey;
    private String env;
    private String signedPreKeyRecord;
    private int registrationId;
    private boolean success;

    public SendSmsRegisterResult() {
    }

    public SendSmsRegisterResult(String registerKey, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.registerKey = registerKey;
    }

    public SendSmsRegisterResult(String registerKey, Object data, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.registerKey = registerKey;
        this.data = data;
    }

    public SendSmsRegisterResult(String username, String envEncryptKey, boolean success, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.username = username;
        this.envEncryptKey = envEncryptKey;
        this.success = success;
    }

    public SendSmsRegisterResult(String username, String env, String signedPreKeyRecord, String identityKeyPair, int registrationId, boolean success, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.username = username;
        this.envEncryptKey = envEncryptKey;
        this.success = success;
    }

    public SendSmsRegisterResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
