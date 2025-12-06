package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.ImportContactTcTokenRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ApiType(TypeConstant.TaskType.IMPORT_CONTACT_TC_TOKEN)
public class ImportContactTcTokenService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        List<TcToken> tcTokens = dataObject.getJSONObject("data").getJSONArray("tcTokens").toJavaList(TcToken.class);
        StatusResult statusResult = user.sendRequest(new ImportContactTcTokenRequest(tcTokens));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
