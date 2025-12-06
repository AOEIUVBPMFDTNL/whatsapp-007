package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * 批量发送消息包
 */
@Data
public class BatchSendMessagePack {
    /**
     * 粉丝id列表
     */
    private List<String> userIds;
    /**
     * 上传通讯录
     */
    private boolean uploadContact;
    /**
     * 消息内容
     */
    private String content;
}
