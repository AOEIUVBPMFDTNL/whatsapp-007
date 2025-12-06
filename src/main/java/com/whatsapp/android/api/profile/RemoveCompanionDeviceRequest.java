package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.TaskEvent;

/**
 * 移除多设备
 *
 * @author sunnoc
 * @date 2022-12-29 10:21
 */
public class RemoveCompanionDeviceRequest extends AbstractRequest<StatusResult> {
    private final String userId;

    public RemoveCompanionDeviceRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.REMOVE_COMPANION_DEVICE;
    }

    @Override
    public boolean request() {
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        if (jidInfo.deviceId == 0) {
            //app设备不做移除
            user.getTaskNotify().setEventContent(taskId, ProtocolTreeNode.success(Constant.OK));
            return true;
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            gorgeousEngine.removeCompanionDevice(userId, taskId);
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}