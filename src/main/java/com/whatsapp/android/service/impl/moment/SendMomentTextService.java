package com.whatsapp.android.service.impl.moment;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.moment.SendMomentTextRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 发送文本动态
 *
 * @author sunnoc
 * @date 2021-08-30 12:22
 */
@Service
@ApiType(TypeConstant.TaskType.SEND_MOMENT_TEXT)
public class SendMomentTextService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        MessagePack data = dataObject.getObject("data", MessagePack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new SendMomentTextRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
