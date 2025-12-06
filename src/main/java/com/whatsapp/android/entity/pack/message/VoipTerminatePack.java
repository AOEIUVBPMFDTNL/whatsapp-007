package com.whatsapp.android.entity.pack.message;

import lombok.Data;

@Data
public class VoipTerminatePack {
    /**
     * 粉丝id
     */
    private String userId;
    /**
     * 呼叫id
     */
    private String callId;
    /**
     * 呼叫时长，单位毫秒
     */
    private long callTime;
    /**
     * 视频电话
     */
    private boolean video;

}
