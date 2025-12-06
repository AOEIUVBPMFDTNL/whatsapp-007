package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2023-03-13 18:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetBusinessUserNicknameResult extends StatusResult {
    /**
     * 昵称
     */
    private String nickname;

    public GetBusinessUserNicknameResult() {
    }

    public GetBusinessUserNicknameResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
