package com.whatsapp.android.entity.pack.profile;

import lombok.Data;

@Data
public class GenerateMsgIdPack {
    private String userId;
    /**
     * 是否是养号消息id
     */
    private boolean feed;
}
