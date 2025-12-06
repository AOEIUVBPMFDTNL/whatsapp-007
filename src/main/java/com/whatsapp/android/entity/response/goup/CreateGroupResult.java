package com.whatsapp.android.entity.response.goup;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author sunnoc
 * @date 2021-03-12 15:07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateGroupResult extends StatusResult {
    /**
     * 创建者
     */
    private String creator;
    /**
     * 群id
     */
    private String groupId;
    /**
     * 群名称
     */
    private String subjectName;
    /**
     * 描述id
     */
    private String descId;
    /**
     * 描述
     */
    private String desc;
    /**
     * 邀请链接
     */
    private String inviteLink;
    /**
     * 群成员
     */
    private List<GroupMember> members;

    /**
     * 是否禁言，true代表只允许管理员发送消息，false代表所有人都可以发送消息
     */
    private Boolean forbid;
    /**
     * 是否锁定编辑群信息，true代表只允许管理员编辑，false代表所有人都可以编辑
     */
    private Boolean editGroupLocked;
    /**
     * 是否被封禁
     */
    private Boolean banned;

    /**
     * 是否所有群成员都可以邀请成员加入
     */
    private Boolean allMemberAdd;

    /**
     * 群成员进群是否需要管理员批准
     */
    private Boolean memberAddApprovalOn;
    /**
     * 是否为社群
     */
    private Boolean community;
    /**
     * 是否为子群
     */
    private Boolean subGroup;
    /**
     * 是否为公告群
     */
    private Boolean defaultSubGroup;
    /**
     * 子群的社群群id
     */
    private String linkedParentGroupId;
    /**
     * 审批群成员加群列表
     */
    private List<String> approveMemberJoin;

    public CreateGroupResult() {
    }

    public CreateGroupResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }
}
