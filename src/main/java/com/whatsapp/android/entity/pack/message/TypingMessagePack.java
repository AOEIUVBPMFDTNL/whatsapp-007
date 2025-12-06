package com.whatsapp.android.entity.pack.message;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-08-30 11:15
 */
@Data
public class TypingMessagePack {
    private String userId;
    /**
     * 开始-start，结束-stop
     */
    private String status;
    /**
     * 是否是发送语音
     */
    private boolean voice;
    /**
     * 是否异步发送
     */
    private boolean async;
}
