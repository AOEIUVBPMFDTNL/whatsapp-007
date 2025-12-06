package com.whatsapp.android.entity.response.message;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-10 18:29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessageResult extends StatusResult {
    private String msgId;

    public SendMessageResult() {
    }

    public SendMessageResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
