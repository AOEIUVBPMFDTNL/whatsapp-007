package com.whatsapp.android.entity.pack.profile;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-05-15 13:29
 */
@Data
public class UpdatePrivacySettingPack {
    private String readReceipts;
    private String profile;
    private String status;
    private String online;
    private String last;
    private String groupAdd;
    private String callAdd;
}
