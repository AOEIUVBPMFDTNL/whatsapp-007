package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.MarkReadRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.MarkReadPack;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.ws.WebSocketClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 标记消息为已读
 *
 * @author sunnoc
 * @date 2022-08-19 14:27
 */
@Service
@ApiType(TypeConstant.TaskType.MARK_READ)
public class MarkReadService implements ApiStrategy {
    @Resource(name = "tomcatThreadPoolExecutor")
    private Executor tomcatThreadPoolExecutor;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        MarkReadPack data = dataObject.getObject("data", MarkReadPack.class);
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

    private Result execute(User user, String username, String type, MarkReadPack data, String taskId) {
        StatusResult statusResult = user.sendRequest(new MarkReadRequest(data));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
