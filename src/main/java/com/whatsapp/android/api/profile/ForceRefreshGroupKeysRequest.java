package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import axolotl.AxolotlManager;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;

public class ForceRefreshGroupKeysRequest extends AbstractRequest<StatusResult> {
    private final String groupId;

    public ForceRefreshGroupKeysRequest(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.FORCE_REFRESH_GROUP_KEYS;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public StatusResult execute() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            AxolotlManager axolotlManager_ = gorgeousEngine.axolotlManager_;
            if (axolotlManager_ != null) {
                String username = user.getLoginPack().getUsername();
                StringUtil.JidInfo jidInfo = StringUtil.ParseJid(gorgeousEngine.getUserId());
                try {
                    KeyLockUtil.lock(username);
                    gorgeousEngine.axolotlManager_.senderKeyStore_.deleteGroupRecord(groupId, jidInfo.recipientId);
                } catch (Exception ignored) {
                } finally {
                    KeyLockUtil.unlock(username);
                }
            }
        }
        return StatusResult.ok();

    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
