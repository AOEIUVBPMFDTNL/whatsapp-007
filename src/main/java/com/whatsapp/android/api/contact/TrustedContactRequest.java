package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 信任联系人
 *
 * @author sunnoc
 * @date 2022-02-10 15:55
 */

public class TrustedContactRequest extends AbstractRequest<StatusResult> {
    private String userId;

    public TrustedContactRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.TRUSTED_CONTACT;
    }


    @Override
    public boolean request() {
        user.getGorgeousEngine().trustedContact(userId, getTaskId());
        return true;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public boolean onlyRequest() {
        return true;
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
