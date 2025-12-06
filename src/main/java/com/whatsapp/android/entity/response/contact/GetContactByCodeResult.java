package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-04-07 17:16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetContactByCodeResult extends StatusResult {
    private String userId;
    private String nickname;

    public GetContactByCodeResult(StatusResult statusResult, String userId, String nickname) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.userId = userId;
        this.nickname = nickname;
    }

    public GetContactByCodeResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
