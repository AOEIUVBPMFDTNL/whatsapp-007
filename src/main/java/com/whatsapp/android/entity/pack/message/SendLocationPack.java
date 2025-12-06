package com.whatsapp.android.entity.pack.message;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-11 10:22
 */
@Data
public class SendLocationPack {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 纬度
     */
    private String latitude;
    /**
     * 经度
     */
    private String longitude;
    /**
     * 地点名称
     */
    private String locationName;
    /**
     * 地点地址
     */
    private String addr;
    /**
     * 留言
     */
    private String message;
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
