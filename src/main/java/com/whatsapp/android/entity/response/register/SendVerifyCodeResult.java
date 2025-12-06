package com.whatsapp.android.entity.response.register;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-20 13:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SendVerifyCodeResult extends StatusResult {
    public SendVerifyCodeResult() {
    }

    public SendVerifyCodeResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
