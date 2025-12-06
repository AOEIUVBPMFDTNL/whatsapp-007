package com.whatsapp.android.service.impl.message;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.SendReactionMessageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.ws.WebSocketClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 消息发送表情
 *
 * @author sunnoc
 * @date 2021-03-17 10:33
 */
@Service
@ApiType(TypeConstant.TaskType.SEND_REACTION_MESSAGE)
public class SendReactionMessageService implements ApiStrategy {
    @Resource(name = "tomcatThreadPoolExecutor")
    private Executor tomcatThreadPoolExecutor;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        MessagePack data = dataObject.getObject("data", MessagePack.class);
        String username = user.getLoginPack().getUsername();
        if (data.isAsync()) {
            if (StringUtils.isEmpty(data.getMsgId())) {
                String msgId = IdUtil.simpleUUID().toUpperCase();
                data.setMsgId(msgId);
            }
            tomcatThreadPoolExecutor.execute(() -> {
                String result = execute(user, username, type, data, taskId).toJson();
                WebSocketClient.sendMsg(username, result);
            });
            SendMessageResult sendMessageResult = new SendMessageResult(StatusResult.ok());
            sendMessageResult.setMsgId(data.getMsgId());
            return Result.taskSuccess(type, taskId, username, sendMessageResult);
        }
        return execute(user, username, type, data, taskId);
    }

    private Result execute(User user, String username, String type, MessagePack data, String taskId) {
        SendMessageResult sendMessageResult = user.sendRequest(new SendReactionMessageRequest(data));
        if (Constant.OK.equals(sendMessageResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, sendMessageResult);
        }
        return Result.taskFail(type, taskId, username, sendMessageResult);
    }
}
