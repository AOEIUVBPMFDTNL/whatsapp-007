package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.GetUserDeviceInfoResult;
import com.whatsapp.android.request.AbstractRequest;

import java.util.List;

/**
 * 获取用户多设备信息
 *
 * @author sunnoc
 * @date 2021-12-08 12:08
 */
public class GetUserDeviceInfoRequest extends AbstractRequest<GetUserDeviceInfoResult> {
    private List<String> userList;

    public GetUserDeviceInfoRequest(List<String> userList) {
        this.userList = userList;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_USER_DEVICE_INFO;
    }

    @Override
    public boolean request() {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        ProtocolTreeNode protocolTreeNode = gorgeousEngine.generateUserDeviceInfo(taskId, userList);
        gorgeousEngine.AddTask(protocolTreeNode);
        return true;
    }

    @Override
    public GetUserDeviceInfoResult parseResult(ProtocolTreeNode node) {
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        List<String> userList = gorgeousEngine.parseDeviceResult(node);
        if (userList != null && userList.size() > 0) {
            return new GetUserDeviceInfoResult(userList);
        }
        return new GetUserDeviceInfoResult(StatusResult.fail(), null);
    }
}
