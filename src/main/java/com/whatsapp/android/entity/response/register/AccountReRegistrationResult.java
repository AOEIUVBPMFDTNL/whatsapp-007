package com.whatsapp.android.entity.response.register;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AccountReRegistrationResult extends StatusResult {
    private String registerKey;

    public AccountReRegistrationResult() {
    }

    public AccountReRegistrationResult(String registerKey, StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.registerKey = registerKey;
    }

    public AccountReRegistrationResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
