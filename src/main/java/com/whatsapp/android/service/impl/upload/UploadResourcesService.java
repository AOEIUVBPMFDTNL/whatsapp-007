package com.whatsapp.android.service.impl.upload;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.api.upload.UploadResourcesRequest;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.upload.UploadResult;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 上传资源文件
 *
 * @author sunnoc
 * @date 2021-04-27 14:13
 */
@Service
@ApiType(TypeConstant.TaskType.UPLOAD_RESOURCES)
@RequiredArgsConstructor
public class UploadResourcesService implements ApiStrategy {
    private final ApiService apiService;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        MessagePack data = dataObject.getObject("data", MessagePack.class);
        String username = user.getLoginPack().getUsername();
        byte[] downloadBytes = apiService.downPhoto(data.getContent(), 60 * 1000);
        String msgType = data.getMsgType();
        if ("document".equals(msgType)) {
            if (downloadBytes == null || downloadBytes.length <= 0) {
                return Result.taskFail(type, taskId, username, StatusResult.fail("资源下载失败"));
            }
        } else {
            if (downloadBytes == null || downloadBytes.length <= 200) {
                return Result.taskFail(type, taskId, username, StatusResult.fail("资源下载失败"));
            }
        }
        String fileName = data.getFileName();
        if ("document".equals(msgType) && ObjectUtil.isNotNull(fileName) && fileName.endsWith(".txt")) {
            downloadBytes = WhatsAppUtils.modifyTxtMd5(downloadBytes);
        }
        UploadResult uploadResult = user.sendRequest(new UploadResourcesRequest(downloadBytes, msgType, fileName, data.getCaption()));
        if (Constant.OK.equals(uploadResult.getStatus())) {
            return Result.taskSuccess(type, taskId, username, uploadResult);
        }
        return Result.taskFail(type, taskId, username, uploadResult);
    }
}
