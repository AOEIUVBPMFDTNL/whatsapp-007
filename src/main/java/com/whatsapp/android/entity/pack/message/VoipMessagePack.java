package com.whatsapp.android.entity.pack.message;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2023-04-03 10:24
 */
@Data
public class VoipMessagePack {
    /**
     * 粉丝id
     */
    private String userId;
    /**
     * 自定义消息id
     */
    private String msgId;
    /**
     * 延时多久挂断语音视频电话
     */
    private int delay;
    /**
     * 是否异步发送
     */
    private boolean async;
    /**
     * tcToken
     */
    private String tcToken;
    /**
     * 是否使用假的session发送
     */
    private boolean fake;
}
