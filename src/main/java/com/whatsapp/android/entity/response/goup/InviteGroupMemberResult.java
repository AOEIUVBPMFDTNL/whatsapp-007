package com.whatsapp.android.entity.response.goup;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;

import java.util.List;

/**
 * 邀请群成员结果
 *
 * @author Rocky
 */
@Data
public class InviteGroupMemberResult extends StatusResult {
    /**
     * 操作的群id
     */
    private String groupId;
    /**
     * 成功进群的成员
     */
    private List<GroupMember> members;
    /**
     * 待审核的成员
     */
    private List<GroupMember> pendingApproveMember;
    /**
     * 已在群的成员
     */
    private List<GroupMember> alreadyInGroupMember;
    /**
     * 其余拉群失败的成员
     */
    private List<GroupMember> failureMember;
    /**
     * 未注册ws的成员
     */
    private List<GroupMember> unRegisteredMember;

    public InviteGroupMemberResult() {
    }

    public InviteGroupMemberResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

}
