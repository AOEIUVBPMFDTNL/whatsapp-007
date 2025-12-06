package com.whatsapp.android.service.impl.group;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.group.ModifyGroupSubjectRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.service.ApiStrategy;
import org.springframework.stereotype.Service;

/**
 * 修改群主题
 *
 * @author sunnoc
 * @date 2021-03-17 10:06
 */
@Service
@ApiType(TypeConstant.TaskType.MODIFY_GROUP_SUBJECT)
public class ModifyGroupSubjectService implements ApiStrategy {
    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        JSONObject data = dataObject.getJSONObject("data");
        String groupId = data.getString("groupId");
        String subjectName = data.getString("subjectName");
        String username = user.getLoginPack().getUsername();
        StatusResult statusResult = user.sendRequest(new ModifyGroupSubjectRequest(groupId, subjectName));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
