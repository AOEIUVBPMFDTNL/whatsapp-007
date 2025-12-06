package com.whatsapp.android.entity.response.goup;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-03-12 15:30
 */
@Data
public class GroupMember {
    /**
     * 成员类型，0普通成员，1管理员，2超级管理员
     */
    private int type = 0;
    /**
     * 成员用户id
     */
    private String userId;

    /**
     * 用户Lid
     */
    private String userLid;

    public GroupMember() {
    }

    public GroupMember(int type, String userId) {
        this.type = type;
        this.userId = userId;
    }

    public GroupMember(int type, String userId, String userLid) {
        this.type = type;
        this.userId = userId;
        this.userLid = userLid;
    }
}
