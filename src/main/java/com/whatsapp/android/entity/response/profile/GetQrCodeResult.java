package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-04-07 16:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetQrCodeResult extends StatusResult {
    private String qrUrl;

    public GetQrCodeResult() {
    }


    public GetQrCodeResult(StatusResult statusResult, String qrUrl) {
        super(statusResult.getStatus(), statusResult.getMessage());

        this.qrUrl = qrUrl;
    }

    public GetQrCodeResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
