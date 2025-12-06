package com.whatsapp.android.service.impl.message;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.SendVCardRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.SendVCardPack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.WhatsAppUtils;
import com.whatsapp.android.ws.WebSocketClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 发送名片消息
 *
 * @author sunnoc
 * @date 2021-03-17 10:37
 */
@Service
@ApiType(TypeConstant.TaskType.SEND_VCARD_MESSAGE)
public class SendVCardService implements ApiStrategy {
    @Resource(name = "tomcatThreadPoolExecutor")
    private Executor tomcatThreadPoolExecutor;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        SendVCardPack data = dataObject.getObject("data", SendVCardPack.class);
        String username = user.getLoginPack().getUsername();

        String msgId = data.getMsgId();
        if (!StringUtils.isEmpty(msgId) && msgId.contains("FEED")) {
            //如果养号允许转发聊天室
            msgId = WhatsAppUtils.generateFeedMsgIdToLTS(username, user.getLoginPack().isIosLogin());
        }
        data.setMsgId(msgId);

        if (data.isAsync()) {
            if (StringUtils.isEmpty(data.getMsgId())) {
                msgId = IdUtil.simpleUUID().toUpperCase();
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

    private Result execute(User user, String username, String type, SendVCardPack data, String taskId) {
        SendMessageResult sendMessageResult = user.sendRequest(new SendVCardRequest(data));
        if (Constant.OK.equals(sendMessageResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, sendMessageResult);
        }
        return Result.taskFail(type, taskId, username, sendMessageResult);
    }
}
