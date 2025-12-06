package com.whatsapp.android.service.impl.profile;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.profile.ModifyHeadImageRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.ImageResizerAndCompressor;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 修改头像
 *
 * @author sunnoc
 * @date 2021-03-17 10:51
 */
@Service
@ApiType(TypeConstant.TaskType.MODIFY_HEAD_IMAGE)
@RequiredArgsConstructor
public class ModifyHeadImageService implements ApiStrategy {
    private final ApiService apiService;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        MessagePack data = dataObject.getObject("data", MessagePack.class);
        String username = user.getLoginPack().getUsername();
        byte[] downloadBytes = apiService.downPhoto(data.getContent());
        if (downloadBytes != null && downloadBytes.length <= 200) {
            return Result.taskFail(type, taskId, username, StatusResult.fail("头像下载失败"));
        }
        downloadBytes = ImageResizerAndCompressor.convertImageToByteArray(downloadBytes);
        if (!data.isNoPadding()) {
            downloadBytes = WhatsAppUtils.modifyFileMd5(downloadBytes);
        }
        StatusResult statusResult = user.sendRequest(new ModifyHeadImageRequest(downloadBytes, data.getGroupId()));
        if (Constant.OK.equals(statusResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, statusResult);
        }
        return Result.taskFail(type, taskId, username, statusResult);
    }
}
