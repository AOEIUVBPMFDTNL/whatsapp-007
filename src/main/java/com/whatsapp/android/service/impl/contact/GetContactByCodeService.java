package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.GetContactByCodeRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.contact.GetContactByCodeResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 通过二维码获取信息
 *
 * @author sunnoc
 * @date 2021-04-07 17:21
 */
@Service
@ApiType(TypeConstant.TaskType.GET_CONTACT_BY_CODE)
public class GetContactByCodeService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String qrUrl = dataObject.getJSONObject("data").getString("qrUrl");
        String username = user.getLoginPack().getUsername();
        GetContactByCodeResult getContactByCodeResult = user.sendRequest(new GetContactByCodeRequest(qrUrl));
        if (Constant.OK.equals(getContactByCodeResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getContactByCodeResult);
        }
        return Result.taskFail(type, taskId, username, getContactByCodeResult);
    }
}
