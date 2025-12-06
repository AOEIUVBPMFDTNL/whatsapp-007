package com.whatsapp.android.entity.response.goup;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-30 11:29
 */
@Data
@Builder
public class GroupNotifyResult {
    /**
     * 通知类型，创建：create，离开群：remove，设置为管理员：promote，添加群成员：add，
     * 重置群链接：invite，撤销管理员身份：demote，群名称更改通知：subject
     * 允许所有群组成员发言：notAnnouncement，只允许管理员发言：announcement
     * 只允许管理员编辑群组信息：locked，允许所有群组编辑群组信息：unlocked
     * 修改群描述通知：description，修改群头像通知：setPicture
     * 允许所有成员邀请成员进群: allMemberAdd, 允许管理员邀请成员进群: adminAdd
     * 需要管理员审批进群成员: membershipApprovalModeOn, 不需要管理员审批进群成员: membershipApprovalModeOff,
     * 群封禁通知: suspended
     * 成员进群审批 createdMembershipRequests
     */
    private String notifyType;
    /**
     * 群id
     */
    private String groupId;
    /**
     * 操作人
     */
    private String participant;
    /**
     * 操作人昵称
     */
    private String notify;
    /**
     * 是否通过邀请链接进群
     */
    private boolean inviteUrlJoinGroup;
    /**
     * 群名称
     */
    private String subjectName;
    /**
     * 邀请链接
     */
    private String inviteLink;
    /**
     * 是否是新群
     */
    private Boolean newGroup;
    /**
     * 群成员
     */
    private List<GroupMember> members;
    /**
     * 是否为社群
     */
    private boolean community;
    /**
     * 是否为某个社群下的子群
     */
    private boolean subGroup;
    /**
     * 是否为某个社群下默认的公告群
     */
    private boolean defaultSubGroup;
    /**
     * 归属与哪个社群
     */
    private String linkedParentGroupId;
    /**
     * 是否所有群成员都可以邀请成员加入
     */
    private Boolean allMemberAdd;
    /**
     * 群成员进群是否需要管理员批准
     */
    private Boolean memberAddApprovalOn;
    /**
     * 群聊是否被封禁
     */
    private Boolean suspended;
    /**
     * 请求进群的用户userId
     */
    private List<String> requestedUser;
}
