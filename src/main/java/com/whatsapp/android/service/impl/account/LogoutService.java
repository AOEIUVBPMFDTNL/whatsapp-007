package com.whatsapp.android.service.impl.account;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 账号退出
 *
 * @author sunnoc
 * @date 2021-03-16 18:17
 */
@Service
@Slf4j
@ApiType(TypeConstant.TaskType.LOGOUT)
public class LogoutService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        try {
            user.setMarkLogout(true);
            if (user.isLogging() || user.isReLogin()) {
                return Result.taskFail(type, taskId, username, StatusResult.fail(Constant.OnlineStatus.LOGGING_EXIT_FAIL));
            }
            user.stop();
        } catch (Throwable ignored) {
        }
        return Result.taskSuccess(type, taskId, username, StatusResult.ok());
    }
}
