package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.GetQrCodeRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.profile.GetQrCodeResult;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 获取二维码
 *
 * @author sunnoc
 * @date 2021-04-07 16:53
 */
@Service
@ApiType(TypeConstant.TaskType.GET_QR_CODE)
public class GetQrCodeService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String username = user.getLoginPack().getUsername();
        GetQrCodeResult getQrCodeResult = user.sendRequest(new GetQrCodeRequest());
        if (Constant.OK.equals(getQrCodeResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, getQrCodeResult);
        }
        return Result.taskFail(type, taskId, username, getQrCodeResult);
    }
}
