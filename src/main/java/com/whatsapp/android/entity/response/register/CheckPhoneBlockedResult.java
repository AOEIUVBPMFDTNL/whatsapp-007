package com.whatsapp.android.entity.response.register;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-11-04 18:11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CheckPhoneBlockedResult extends StatusResult {
    /**
     * 手机区号
     */
    private String phoneAreaCode;
    /**
     * 是否封号
     */
    private Boolean blocked;
    /**
     * 账号可能已注册
     */
    private Boolean possibleMigration;
    /**
     * 是否是商业号
     */
    private Boolean business;

    public CheckPhoneBlockedResult() {
    }

    public CheckPhoneBlockedResult(StatusResult statusResult, String phoneAreaCode, Boolean blocked) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.phoneAreaCode = phoneAreaCode;
        this.blocked = blocked;
    }

    public CheckPhoneBlockedResult(StatusResult statusResult, String phoneAreaCode, Boolean blocked, Boolean possibleMigration, Boolean business) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.phoneAreaCode = phoneAreaCode;
        this.blocked = blocked;
        this.possibleMigration = possibleMigration;
        this.business = business;
    }

    public CheckPhoneBlockedResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
