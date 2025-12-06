package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ForceLoginStatus extends StatusResult {
    private boolean activateForce;

    public ForceLoginStatus(StatusResult statusResult, boolean activateForce) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.activateForce = activateForce;
    }
}
