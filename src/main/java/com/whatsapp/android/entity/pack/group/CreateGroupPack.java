package com.whatsapp.android.entity.pack.group;

import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-12 15:13
 */
@Data
public class CreateGroupPack {
    /**
     * 邀请群成员时需要，群id
     */
    private String groupId;
    /**
     * 群聊主题名
     */
    private String subjectName;
    /**
     * 设置管理员,true设置管理员，false 取消管理员
     */
    private boolean admin;
    /**
     * 群成员
     */
    private List<String> list;
    /**
     * 邀请链接
     */
    private String inviteLink;
    /**
     * 是否为社群
     */
    private boolean community;
    /**
     * 是否同意成员进群
     */
    private boolean approval;
    /**
     * 是否所有成员可以邀请群成员进群
     */
    private boolean allMemberAdd;
    /**
     * 是否开启群审批
     */
    private boolean membershipApprovalModeOn;
}
