package com.whatsapp.android.service.impl.message;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.SendHyperLinkTextMessageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.HyperLinkMessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.ws.WebSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 发送超链接图文消息
 */
@Service
@ApiType(TypeConstant.TaskType.SEND_HYPER_LINK_TEXT_MESSAGE)
@RequiredArgsConstructor
public class SendHyperLinkTextMessageService implements ApiStrategy {
    private final ApiService apiService;
    @Resource(name = "tomcatThreadPoolExecutor")
    private Executor tomcatThreadPoolExecutor;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        HyperLinkMessagePack data = dataObject.getObject("data", HyperLinkMessagePack.class);
        String username = user.getLoginPack().getUsername();
        if (!StringUtils.hasLength(data.getContent())) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("发送消息不能为空"));
        }
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

    private Result execute(User user, String username, String type, HyperLinkMessagePack data, String taskId) {
        byte[] downloadBytes = apiService.downPhoto(data.getImage(), 60 * 1000);
        if (downloadBytes == null || downloadBytes.length <= 200) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("图片下载失败"));
        }
        SendMessageResult sendMessageResult = user.sendRequest(new SendHyperLinkTextMessageRequest(data, downloadBytes));
        if (Constant.OK.equals(sendMessageResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, sendMessageResult);
        }
        return Result.taskFail(type, taskId, username, sendMessageResult);
    }
}
