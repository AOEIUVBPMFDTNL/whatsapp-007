package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.profile.GenerateMsgIdPack;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.response.profile.GenerateMsgIdResult;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ApiType(TypeConstant.TaskType.GENERATE_MSG_ID)
public class GenerateMsgIdService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        GenerateMsgIdPack generateMsgIdPack = dataObject.getObject("data", GenerateMsgIdPack.class);
        String userId = generateMsgIdPack.getUserId();
        LoginPack loginPack = user.getLoginPack();
        String username = loginPack.getUsername();
        boolean iosLogin = loginPack.isIosLogin();
        String msgId;
        if (iosLogin) {
            msgId = generateMsgIdPack.isFeed() ? WhatsAppUtils.generateFeedMsgId(userId, true) : WhatsAppUtils.generateIosMsgId(userId);
        } else {
            msgId = generateMsgIdPack.isFeed() ? WhatsAppUtils.generateFeedMsgId(userId, false) : WhatsAppUtils.generateAndroidMsgId();
        }
        boolean businessVersion = loginPack.isBusinessVersion();
        return Result.taskSuccess(type, taskId, username, new GenerateMsgIdResult(msgId, iosLogin, businessVersion));
    }
}