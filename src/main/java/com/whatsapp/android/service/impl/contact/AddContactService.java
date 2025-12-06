package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.AddContactRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.contact.SyncContactPack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 添加通讯录
 *
 * @author sunnoc
 * @date 2021-06-09 19:15
 */
@Service
@ApiType(TypeConstant.TaskType.ADD_CONTACT)
public class AddContactService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        SyncContactPack data = dataObject.getObject("data", SyncContactPack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new AddContactRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
