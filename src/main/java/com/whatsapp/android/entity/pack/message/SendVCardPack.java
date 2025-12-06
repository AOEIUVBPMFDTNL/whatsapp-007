package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-11 09:36
 */
@Data
public class SendVCardPack {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 分享名片带区号的用户手机号
     */
    private String phone;
    /**
     * 名片名称
     */
    private String name;
    /**
     * 名片描述
     */
    private String describe;
    /**
     * 弹窗广告语
     */
    private String showName;
    /**
     * 自定义消息id
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
     * tcToken
     */
    private String tcToken;
}
