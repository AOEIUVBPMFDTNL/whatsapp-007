package com.whatsapp.android.entity.response.account;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-10 10:53
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoginResult extends StatusResult {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 注册时间
     */
    private String creationTime;
    private String headUrl;
    /**
     * 违规原因
     */
    private String violationReason;

    public static LoginResult fail(String message) {
        LoginResult statusResult = new LoginResult();
        statusResult.setStatus("fail");
        statusResult.setMessage(message);
        return statusResult;
    }
}
