package com.whatsapp.android.entity.pack.group;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-10-17 12:39
 */
@Data
public class ModifyGroupAddPermissionPack {
    /**
     * 群ID
     */
    private String groupId;
    /**
     * 是否全员可以获取邀请链接以及邀请成员加入
     */
    private boolean allMemberAdd;
}
