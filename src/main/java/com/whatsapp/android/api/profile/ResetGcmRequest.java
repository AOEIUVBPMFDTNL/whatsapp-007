package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import axolotl.AxolotlManager;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;

/**
 * @author sunnoc
 * @date 2023-05-26 16:43
 */
public class ResetGcmRequest extends AbstractRequest<StatusResult> {
    private boolean force;

    public ResetGcmRequest(boolean force) {
        this.force = force;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.RESET_GCM;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public StatusResult execute() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            if (force) {
                AxolotlManager axolotlManager_ = gorgeousEngine.axolotlManager_;
                if (axolotlManager_ != null) {
                    try {
                        KeyLockUtil.lock(user.getLoginPack().getUsername());
                        axolotlManager_.deleteGcmSettings();
                    } catch (Exception ignored) {
                    } finally {
                        KeyLockUtil.unlock(user.getLoginPack().getUsername());
                    }
                }
            }
            //触发重连
            gorgeousEngine.StopEngine();
        }
        return StatusResult.ok();
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
