package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-04-27 10:46
 */
@Data
public class SendResourcesMessagePack {
    /**
     * 接收人id
     */
    private String userId;
    /**
     * 消息类型，图片：image，语音：ptt，视频：video，文件：document
     */
    private String msgType;
    /**
     * 加密内容
     */
    private String content;
    /**
     * 自定义msgId
     */
    private String msgId;
    /**
     * 参与人id，发送群引用消息时需要用到
     */
    private String participant;
    /**
     * 引用消息id
     */
    private String quotedMsgId;
    /**
     * 引用消息内容
     */
    private String quotedMsgContent;
    /**
     * 是否异步发送
     */
    private boolean async;
    /**
     * 批量用户ID
     */
    private List<String> userIds;
    /**
     * "@"人列表
     */
    private List<String> mentionedUserIds;
    /**
     * tcToken
     */
    private String tcToken;
}
