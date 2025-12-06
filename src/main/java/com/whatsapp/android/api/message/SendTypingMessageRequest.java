package com.whatsapp.android.api.message;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.TypingMessagePack;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 发送输入消息请求
 *
 * @author sunnoc
 * @date 2021-08-30 10:48
 */
public class SendTypingMessageRequest extends AbstractRequest<StatusResult> {
    private final TypingMessagePack messagePack;
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public SendTypingMessageRequest(TypingMessagePack messagePack) {
        this.messagePack = messagePack;
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_TYPING_MESSAGE;
    }

    @Override
    public boolean onlyRequest() {
        return true;
    }

    @Override
    public boolean request() {
        String userId = messagePack.getUserId();
        if (StringUtils.isEmpty(userId)) {
            return false;
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine == null) {
            return false;
        }
        if ("start".equals(messagePack.getStatus())) {
            if (messagePack.isVoice()) {
                gorgeousEngine.sendVoiceRecording(userId);
                return true;
            }
            gorgeousEngine.sendChatComposing(userId);
        } else if ("stop".equals(messagePack.getStatus())) {
            gorgeousEngine.sendChatPaused(userId);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        if (node == null || node.isEmpty()) {
            return StatusResult.fail("超时");
        }
        if (Constant.OK.equals(node.GetTag())) {
            return StatusResult.ok("成功");
        }
        return StatusResult.fail("失败");
    }
}
