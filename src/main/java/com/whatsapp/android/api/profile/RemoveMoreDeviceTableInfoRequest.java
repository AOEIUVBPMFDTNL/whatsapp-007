package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;

import java.util.Locale;

/**
 * 移除多设备表信息
 *
 * @author sunnoc
 * @date 2023-06-25 10:34
 */
public class RemoveMoreDeviceTableInfoRequest extends AbstractRequest<StatusResult> {
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public RemoveMoreDeviceTableInfoRequest() {
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.REMOVE_MORE_DEVICE_TABLE_INFO;
    }

    @Override
    public boolean request() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine != null) {
            //清空表数据
            try {
                KeyLockUtil.lock(user.getLoginPack().getUsername());
                gorgeousEngine.axolotlManager_.delScanWebInfo();
            } catch (Exception ignored) {
            } finally {
                KeyLockUtil.unlock(user.getLoginPack().getUsername());
            }
            gorgeousEngine.SendSyncDelete(getTaskId());
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
