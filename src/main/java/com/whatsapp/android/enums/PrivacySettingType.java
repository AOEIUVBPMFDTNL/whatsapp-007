package com.whatsapp.android.enums;

/**
 * 隐私设置
 */
public enum PrivacySettingType {
    /**
     * 仅自己可见
     */
    NONE("none"),
    /**
     * 所有人可见
     */
    ALL("all"),
    /**
     * 联系人可见
     */
    CONTACTS("contacts"),
    /**
     * 与最后上线时间一致, 仅在线设置有这一选项
     */
    MATCH_LAST_SEEN("match_last_seen"),
    /**
     * 语音视频通话设置
     */
    KNOWN("known");
    private final String type;

    PrivacySettingType(String type) {
        this.type = type;
    }

    // 获取描述的方法
    public String getType() {
        return type;
    }

    /**
     * 判断是否为有效的参数
     */
    public static boolean isEffectiveSetting(String setting) {
        for (PrivacySettingType privacySettingType : PrivacySettingType.values()) {
            if (privacySettingType.getType().equals(setting)) {
                return true;
            }
        }
        return false;
    }
}
