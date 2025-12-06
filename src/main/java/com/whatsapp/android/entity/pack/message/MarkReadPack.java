package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2022-08-19 14:22
 */
@Data
public class MarkReadPack {
    /**
     * 消息id
     */
    private String msgId;
    /**
     * 粉丝id
     */
    private String userId;
    /**
     * 群id
     */
    private String groupId;
    /**
     * 是否异步发送
     */
    private boolean async;
    /**
     * 已读多条消息
     */
    private List<String> msgIds;
}
