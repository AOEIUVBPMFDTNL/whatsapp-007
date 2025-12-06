package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.MarkReadPack;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 标记消息已读
 *
 * @author sunnoc
 * @date 2022-08-19 14:22
 */
public class MarkReadRequest extends AbstractRequest<StatusResult> {
    private final MarkReadPack markReadPack;
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public MarkReadRequest(MarkReadPack markReadPack) {
        this.markReadPack = markReadPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.REVOKE_MESSAGE;
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public boolean request() {
        Optional.ofNullable(user.getGorgeousEngine()).ifPresent(gorgeousEngine -> {
            String userId = WhatsAppUtils.JidNormalize(markReadPack.getUserId());
            String groupId = markReadPack.getGroupId();
            List<String> msgIds = markReadPack.getMsgIds();
            if (msgIds == null || msgIds.isEmpty()) {
                msgIds = Collections.singletonList(markReadPack.getMsgId());
            }
            gorgeousEngine.sendReadTag(msgIds, userId, groupId);
        });
        return true;
    }

    @Override
    public boolean onlyRequest() {
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return parseBaseResult(node);
    }
}
