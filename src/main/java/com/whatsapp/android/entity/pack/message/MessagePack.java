package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-10 18:40
 */
@Data
public class MessagePack {
    /**
     * 接收人id
     */
    private String userId;
    /**
     * 发送消息
     */
    private String content;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 群id
     */
    private String groupId;
    /**
     * 消息类型
     */
    private String msgType;
    /**
     * 兼容易语言发送文字
     */
    private boolean unicode;
    /**
     * 自定义消息id
     */
    private String msgId;
    /**
     * 图片，视频自定义文字
     */
    private String caption;
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
     * 链接模式
     */
    private boolean linkMode;
    /**
     * 链接标题
     */
    private String linkTitle;
    /**
     * 链接url
     */
    private String linkUrl;
    /**
     * 是否异步发送
     */
    private boolean async;
    /**
     * 批量用户ID
     */
    private List<String> userIds;
    /**
     * 超链跳转用户
     */
    private String linkUserId;
    /**
     * 引用消息是否是自己的
     */
    private boolean fromMe;
    /**
     * "@"人列表
     */
    private List<String> mentionedUserIds;
    /**
     * 是否引用媒体类型
     */
    private boolean quotedMedia;
    /**
     * 是否不填充
     */
    private boolean noPadding;
    /**
     * tcToken
     */
    private String tcToken;
}
