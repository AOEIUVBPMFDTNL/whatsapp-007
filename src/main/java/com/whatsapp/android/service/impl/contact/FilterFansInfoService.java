package com.whatsapp.android.service.impl.contact;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.contact.FilterFansInfoRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.contact.FilterFansInfoPack;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;


/**
 * 获取用户上次在线时间
 *
 * @author sunnoc
 * @date 2022-02-14 16:51
 */
@Service
@ApiType(TypeConstant.TaskType.FILTER_FANS_INFO_REQUEST)
public class FilterFansInfoService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        FilterFansInfoPack data = dataObject.getJSONObject("data").toJavaObject(FilterFansInfoPack.class);
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new FilterFansInfoRequest(data.getUserIds()));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
