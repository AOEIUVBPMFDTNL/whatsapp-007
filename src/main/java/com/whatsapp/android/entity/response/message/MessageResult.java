package com.whatsapp.android.entity.response.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 发送消息结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResult {
    /**
     * 消息id
     */
    private String msgId;
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 原因
     */
    private String msg;
}
