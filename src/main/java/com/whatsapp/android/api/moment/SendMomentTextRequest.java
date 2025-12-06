package com.whatsapp.android.api.moment;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.message.MessagePack;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 发送文字动态
 *
 * @author sunnoc
 * @date 2021-08-30 12:09
 */
public class SendMomentTextRequest extends AbstractRequest<StatusResult> {
    private MessagePack messagePack;
    private String finalMsgId;

    public SendMomentTextRequest(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SEND_MOMENT_TEXT;
    }

    @Override
    public void init() {
        String msgId = this.messagePack.getMsgId();
        if (StringUtils.hasLength(msgId)) {
            this.finalMsgId = msgId;
        } else {
            String userId = this.messagePack.getUserId();
            this.finalMsgId = WhatsAppUtils.generateMsgId(userId, user.getLoginPack().isIosLogin());
        }
    }

    @Override
    public String getTaskId() {
        return finalMsgId;
    }

    @Override
    public boolean request() {
        List<String> toFriends = messagePack.getUserIds();
        if (toFriends == null || toFriends.isEmpty()) {
            return false;
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        if (gorgeousEngine == null) {
            return false;
        }
        String content = messagePack.isUnicode()
                ? UnicodeUtil.toString(StrUtil.replace(messagePack.getContent(), "%u", "\\u"))
                : messagePack.getContent();
        gorgeousEngine.sendMomentText(getTaskId(), content, toFriends);
        return true;
    }

    @Override
    public SendMessageResult parseResult(ProtocolTreeNode node) {
        return parseSendMessageResult(node);
    }
}
