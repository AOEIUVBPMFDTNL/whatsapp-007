package com.whatsapp.android.api.upload;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.upload.UploadResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Locale;

/**
 * 上传资源文件
 *
 * @author sunnoc
 * @date 2021-04-27 10:22
 */
public class UploadResourcesRequest extends AbstractRequest<UploadResult> {
    private byte[] file;
    private String msgType;
    private String fileName;
    private String path;
    private String caption;
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public UploadResourcesRequest(byte[] file, String msgType, String fileName, String caption) {
        this.file = file;
        this.msgType = msgType;
        this.fileName = fileName;
        this.caption = caption;
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UPLOAD_RESOURCES;
    }

    @Override
    public boolean request() {
        if ("image".equals(msgType) || "ptt".equals(msgType) ||
                "video".equals(msgType) || "document".equals(msgType) || "gif".equals(msgType)) {
            String fileName = IdUtil.randomUUID();
            String tempPath = System.getProperty("user.dir") + "/out/media/" + fileName;
            FileUtil.writeBytes(file, tempPath);
            if ("image".equals(msgType)) {
                String outPath = WhatsAppUtils.imageToJpg(tempPath);
                FileUtil.del(tempPath);
                FileUtil.rename(new File(outPath), fileName, true);
            }
            path = tempPath;
            JSONObject mediaInfo = new JSONObject();
            String id = user.getGorgeousEngine().uploadMedia(path, msgType, mediaInfo, getTaskId(), this.fileName, caption, timeOut() * 1000);
            if (StringUtils.hasLength(id)) {
                ProtocolTreeNode node = ProtocolTreeNode.success(Constant.OK);
                node.AddAttribute(new StanzaAttribute("content", Base64.encode(JSONObject.toJSONString(mediaInfo))));
                user.getTaskNotify().setEventContent(getTaskId(), node);
            }
        } else {
            user.getTaskNotify().setEventContent(getTaskId(), ProtocolTreeNode.fail(Constant.FAIL, "发送类型错误"));
        }
        return true;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public boolean needDeleteFile() {
        return true;
    }

    @Override
    public String deleteFile() {
        return path;
    }

    @Override
    public long timeOut() {
        return 120L;
    }

    @Override
    public UploadResult parseResult(ProtocolTreeNode node) {
        String tag = node.GetTag();
        if (Constant.OK.equals(tag)) {
            String content = node.GetAttributeValue("content");
            return new UploadResult(StatusResult.ok(), content);
        }
        String message = node.GetAttributeValue("message");
        return new UploadResult(StatusResult.fail(message));
    }
}
