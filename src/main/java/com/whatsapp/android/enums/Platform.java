package com.whatsapp.android.enums;

import lombok.Getter;

@Getter
public enum Platform {
    ANDROID(0),
    IOS(1);

    private final int code;

    Platform(int code) {
        this.code = code;
    }

    public static Platform fromCode(int code) {
        for (Platform platform : Platform.values()) {
            if (platform.code == code) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Invalid platform code: " + code);
    }
}
