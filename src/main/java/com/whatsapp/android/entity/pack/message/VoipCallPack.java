package com.whatsapp.android.entity.pack.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoipCallPack {
    /**
     * 粉丝id
     */
    private String userId;
    /**
     * 呼叫id
     */
    private String callId;
    /**
     * 播放url
     */
    private String playUrl;
    /**
     * 呼叫时长
     */
    private int callTime;
    /**
     * 视频电话
     */
    private boolean video;
    /**
     * 自定义消息id
     */
    private String msgId;
}
