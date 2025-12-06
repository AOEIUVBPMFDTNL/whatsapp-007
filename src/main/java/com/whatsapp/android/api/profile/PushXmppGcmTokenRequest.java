package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;

/**
 * @author sunnoc
 * @date 2023-05-17 17:45
 */
public class PushXmppGcmTokenRequest extends AbstractRequest<StatusResult> {
    /**
     * gcm token
     */
    private String token;

    public PushXmppGcmTokenRequest(String status) {
        this.token = token;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SET_STATUS;
    }

    @Override
    public boolean request() {
        String taskId = getTaskId();
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}