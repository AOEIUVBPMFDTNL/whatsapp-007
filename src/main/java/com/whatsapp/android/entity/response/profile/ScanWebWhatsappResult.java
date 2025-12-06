package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2022-06-17 14:37
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ScanWebWhatsappResult extends StatusResult {
    private String userId;

    public ScanWebWhatsappResult() {
    }

    public ScanWebWhatsappResult(String userId) {
        this.userId = userId;
    }

    public ScanWebWhatsappResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
