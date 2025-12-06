package com.whatsapp.android.entity.pack.message;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-12-29 17:49
 */
@Data
public class RevokeMessagePack {
    /**
     * 撤回谁的消息, 个人/群聊
     */
    private String userId;
    /**
     * 撤回消息id
     */
    private String revokeMsgId;
    /**
     * 是否异步发送
     */
    private boolean async;
    /**
     * 群成员
     */
    private String participant;
    /**
     * 是否管理员删除
     */
    private boolean isAdminDelete;
    /**
     * 是否撤回心情消息
     */
    private boolean reaction;
    /**
     * 撤回心情消息是否为自己发
     */
    private boolean fromMe;
    /**
     * tcToken
     */
    private String tcToken;
}
