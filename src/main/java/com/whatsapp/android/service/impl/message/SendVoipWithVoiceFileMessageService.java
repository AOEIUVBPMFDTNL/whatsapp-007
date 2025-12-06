package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.SendVoipWithVoiceFileRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.VoipCallPack;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 发送语音视频消息
 *
 * @author sunnoc
 * @date 2021-03-17 10:33
 */
@Service
@ApiType(TypeConstant.TaskType.SEND_VOIP_WITH_VOICE_FILE_MESSAGE)
public class SendVoipWithVoiceFileMessageService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        boolean systemMsgId;
        VoipCallPack data = dataObject.getObject("data", VoipCallPack.class);
        String username = user.getLoginPack().getUsername();
        String msgId = data.getMsgId();
        if (!StringUtils.isEmpty(msgId) && msgId.contains("FEED")) {
            //如果养号允许转发聊天室
            msgId = WhatsAppUtils.generateFeedMsgIdToLTS(username, user.getLoginPack().isIosLogin());
            systemMsgId = true;
        } else {
            systemMsgId = false;
        }
        data.setMsgId(msgId);
        return execute(user, username, type, data, taskId, systemMsgId);
    }

    private Result execute(User user, String username, String type, VoipCallPack data, String taskId, boolean systemMsgId) {
        StatusResult statusResult = user.sendRequest(new SendVoipWithVoiceFileRequest(data, systemMsgId));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
