package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.SyncContactRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.contact.SyncContactPack;
import com.whatsapp.android.entity.response.contact.SyncContactResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 同步通讯录校验开通
 *
 * @author sunnoc
 * @date 2021-03-17 09:51
 */
@Service
@ApiType(TypeConstant.TaskType.SYNC_CONTACT)
public class SyncContactService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        SyncContactPack data = dataObject.getObject("data", SyncContactPack.class);
        String username = user.getLoginPack().getUsername();
        SyncContactResult syncContactResult = user.sendRequest(new SyncContactRequest(data));
        if (Constant.OK.equals(syncContactResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, syncContactResult);
        }
        return Result.taskFail(type, taskId, username, syncContactResult);
    }
}
