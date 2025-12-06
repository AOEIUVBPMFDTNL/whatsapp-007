package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-04-20 15:20
 */
@Data
public class DelayTask {
    /**
     * 小号账号
     */
    private String username;
    /**
     * 注册key，通过此key索引到数据库
     */
    private String registerKey;
    /**
     * 消息id
     */
    private String iqId;
    /**
     * 粉丝id
     */
    private String to;
    /**
     * 参与者
     */
    private String participant;
    /**
     * 类型 sendReadTag
     */
    private String type;

    public DelayTask() {
    }

    public DelayTask(String username, String registerKey) {
        this.username = username;
        this.registerKey = registerKey;
    }

    public DelayTask(String username, String iqId, String to, String participant, String type) {
        this.username = username;
        this.iqId = iqId;
        this.to = to;
        this.participant = participant;
        this.type = type;
    }
}
