package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sunnoc
 * @date 2021-03-10 18:29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetPrivacySettingsResult extends StatusResult {

    /**
     * 已读权限
     */
    private String readReceipts;

    /**
     * 头像权限
     */
    private String profile;

    /**
     * 状态信息权限
     */
    private String statusUpdate;

    /**
     * 上线状态
     */
    private String online;

    /**
     * 最后上线时间
     */
    private String last;

    /**
     * 被添加进群组
     */
    private String groupAdd;
    /**
     * 被拨打电话
     */
    private String callAdd;

    public GetPrivacySettingsResult() {
    }

    public GetPrivacySettingsResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

    public void updateSetting(String name, String value) {
        switch (name) {
            case "readreceipts":
                this.setReadReceipts(value);
                break;
            case "profile":
                this.setProfile(value);
                break;
            case "status":
                this.setStatusUpdate(value);
                break;
            case "online":
                this.setOnline(value);
                break;
            case "last":
                this.setLast(value);
                break;
            case "groupadd":
                this.setGroupAdd(value);
                break;
            case "calladd":
                this.setCallAdd(value);
                break;
            default:
                break;
        }
    }
}
