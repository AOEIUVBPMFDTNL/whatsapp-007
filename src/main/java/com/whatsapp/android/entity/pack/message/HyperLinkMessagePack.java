package com.whatsapp.android.entity.pack.message;

import lombok.Data;

/**
 * TODO 写明类的作用
 *
 * @author Rocky
 */
@Data
public class HyperLinkMessagePack {
    /**
     * 自定义消息id
     */
    private String msgId;
    /**
     * 接收人id
     */
    private String userId;
    /**
     * 链接标题
     */
    private String linkTitle;
    /**
     * 链接url
     */
    private String linkUrl;
    /**
     * 链接内容
     */
    private String linkCaption;
    /**
     * 文本内容
     */
    private String content;
    /**
     * 图片数据Base64编码格式
     */
    private String image;
    /**
     * 是否为异步模式
     */
    private boolean async;
    /**
     * tcToken
     */
    private String tcToken;

}
