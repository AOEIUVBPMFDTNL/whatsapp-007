package com.whatsapp.android.entity.pack.group;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-10-17 11:40
 */
@Data
public class ModifyGroupSendMsgPermissionPack {
    /**
     * 群ID
     */
    private String groupId;
    /**
     * 是否禁言，true代表只允许管理员发送消息，false代表所有人都可以发送消息
     */
    private boolean forbid;
}
