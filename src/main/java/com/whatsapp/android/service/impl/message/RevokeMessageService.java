package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.RevokeMessageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.RevokeMessagePack;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.ws.WebSocketClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 撤回消息
 *
 * @author sunnoc
 * @date 2021-12-29 18:02
 */
@Service
@ApiType(TypeConstant.TaskType.REVOKE_MESSAGE)
public class RevokeMessageService implements ApiStrategy {
    @Resource(name = "tomcatThreadPoolExecutor")
    private Executor tomcatThreadPoolExecutor;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        RevokeMessagePack data = dataObject.getObject("data", RevokeMessagePack.class);
        String username = user.getLoginPack().getUsername();
        if (data.isAsync()) {
            tomcatThreadPoolExecutor.execute(() -> {
                String result = execute(user, username, type, data, taskId).toJson();
                WebSocketClient.sendMsg(username, result);
            });
            return Result.taskSuccess(type, taskId, username, StatusResult.ok());
        }
        return execute(user, username, type, data, taskId);
    }

    private Result execute(User user, String username, String type, RevokeMessagePack data, String taskId) {
        StatusResult statusResult = user.sendRequest(new RevokeMessageRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
