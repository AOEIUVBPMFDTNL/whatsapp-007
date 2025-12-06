package com.whatsapp.android.entity.pack.group;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-10-17 12:39
 */
@Data
public class ModifyGroupEditPermissionPack {
    /**
     * 群ID
     */
    private String groupId;
    /**
     * 是否锁定编辑群信息，true代表只允许管理员边界，false代表所有人都可以编辑
     */
    private boolean editGroupLocked;
}
