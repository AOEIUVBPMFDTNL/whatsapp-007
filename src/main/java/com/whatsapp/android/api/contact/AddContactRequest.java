package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.contact.SyncContactPack;
import com.whatsapp.android.request.AbstractRequest;

import java.util.List;


/**
 * @author sunnoc
 * @date 2021-06-09 19:13
 */
public class AddContactRequest extends AbstractRequest<StatusResult> {
    //用户id
    private SyncContactPack syncContactPack;

    public AddContactRequest(SyncContactPack syncContactPack) {
        this.syncContactPack = syncContactPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.ADD_CONTACT;
    }

    @Override
    public boolean request() {
        List<String> list = syncContactPack.getList();
        if (list == null || list.size() == 0) {
            return false;
        }
        user.getGorgeousEngine().AddContact(getTaskId(), list);
        return true;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        StatusResult checkResult = checkResult(node, StatusResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        return StatusResult.ok();
    }
}
