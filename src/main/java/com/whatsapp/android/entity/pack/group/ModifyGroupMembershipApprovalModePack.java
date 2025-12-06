package com.whatsapp.android.entity.pack.group;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-10-17 12:39
 */
@Data
public class ModifyGroupMembershipApprovalModePack {
    /**
     * 群ID
     */
    private String groupId;
    /**
     * 是否开启管理员审批进群成员功能
     */
    private boolean membershipApprovalModeOn;
}
