package com.whatsapp.android.entity.response.contact;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-14 00:31
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SubscribeResult extends StatusResult {
    /**
     * 最后在线时间
     */
    private String lastOnlineTime;
    private String userId;
    /**
     * 是否看不见
     */
    private boolean invisible;

    public SubscribeResult() {
    }

    public SubscribeResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

    public SubscribeResult(StatusResult statusResult, String lastOnlineTime, String userId, boolean invisible) {
        super(statusResult.getStatus(), statusResult.getMessage());
        this.lastOnlineTime = lastOnlineTime;
        this.userId = userId;
        this.invisible = invisible;
    }
}
