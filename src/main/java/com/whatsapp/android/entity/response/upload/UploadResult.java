package com.whatsapp.android.entity.response.upload;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-04-27 10:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UploadResult extends StatusResult {
    private String content;

    public UploadResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

    public UploadResult(StatusResult statusResult, String content) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.content = content;
    }
}
