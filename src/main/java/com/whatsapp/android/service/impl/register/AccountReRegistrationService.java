package com.whatsapp.android.service.impl.register;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.register.AccountReRegistrationRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.AccountReRegistrationResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

@Service
@ApiType(TypeConstant.TaskType.ACCOUNT_RE_REGISTRATION)
public class AccountReRegistrationService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        RegisterPack registerPack = dataObject.getObject("data", RegisterPack.class);
        AccountReRegistrationResult accountReRegistrationResult = user.sendRequest(new AccountReRegistrationRequest(registerPack));
        if (Constant.OK.equals(accountReRegistrationResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, accountReRegistrationResult);
        }
        return Result.taskFail(type, taskId, username, accountReRegistrationResult);
    }
}
