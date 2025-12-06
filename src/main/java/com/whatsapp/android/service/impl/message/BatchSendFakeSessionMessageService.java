package com.whatsapp.android.service.impl.message;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.message.BatchSendFakeSessionMessageRequest;
import com.whatsapp.android.api.message.BatchSendMessageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.BatchSendMessagePack;
import com.whatsapp.android.entity.response.message.BatchSendMessageResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批量快速发送等待中消息
 */
@Service
@ApiType(TypeConstant.TaskType.BATCH_SEND_FAKE_SESSION_MESSAGE)
public class BatchSendFakeSessionMessageService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        BatchSendMessagePack data = dataObject.getObject("data", BatchSendMessagePack.class);
        String username = user.getLoginPack().getUsername();
        if (!StringUtils.hasLength(data.getContent())) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("发送消息不能为空"));
        }
        BatchSendMessageResult batchSendMessageResult = user.sendRequest(new BatchSendFakeSessionMessageRequest(data));
        if (Constant.OK.equals(batchSendMessageResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, batchSendMessageResult);
        }
        return Result.taskFail(type, taskId, username, batchSendMessageResult);
    }
}