package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.ScanWebWhatsappRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 扫码web whatsapp
 *
 * @author sunnoc
 * @date 2022-06-17 14:08
 */
@Service
@ApiType(TypeConstant.TaskType.SCAN_WEB_WHATSAPP)
public class ScanWebWhatsappService implements ApiStrategy {

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        String qrCode = dataObject.getJSONObject("data").getString("qrCode");
        String username = user.getLoginPack().getUsername();
        if (StringUtils.isEmpty(qrCode)) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("二维码不能为空"));
        }
        StatusResult statusResult = user.sendRequest(new ScanWebWhatsappRequest(qrCode));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
