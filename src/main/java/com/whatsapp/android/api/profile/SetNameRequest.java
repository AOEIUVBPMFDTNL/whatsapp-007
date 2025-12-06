package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.enums.NodeTaskType;
import com.whatsapp.android.request.AbstractRequest;

import java.util.Locale;

/**
 * 设置昵称
 *
 * @author sunnoc
 * @date 2021-03-10 15:19
 */
public class SetNameRequest extends AbstractRequest<StatusResult> {
    private String nickName;
    private String event;
    private boolean businessVersion;
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public SetNameRequest(String nickName) {
        this.nickName = nickName;
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SET_NAME;
    }

    @Override
    public boolean request() {
        if (StringUtil.isEmpty(nickName)) {
            return false;
        }
        LoginPack loginPack = user.getLoginPack();
        businessVersion = loginPack.isBusinessVersion();
        if (businessVersion) {
            event = user.getTaskNotify().createEvent();
            user.getGorgeousEngine().modifyBusinessVersionNickname(getTaskId(), event, nickName);
        } else {
            // user.getGorgeousEngine().SetPushName(nickName, getTaskId());
            ProtocolTreeNode presence = new ProtocolTreeNode("presence");
            presence.AddAttribute(new StanzaAttribute("type", "available"));
            presence.AddAttribute(new StanzaAttribute("id", getTaskId()));
            presence.AddAttribute(new StanzaAttribute(Constant.TASK_TAG, TypeConstant.TaskType.SET_NAME));
            presence.AddAttribute(new StanzaAttribute("name", nickName));
            user.getGorgeousEngine().AddTask(presence, NodeTaskType.MODIFY_NICKNAME);
        }
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        if (node == null || node.isEmpty()) {
            return StatusResult.fail("超时");
        }
        String status;
        if (businessVersion) {
            StatusResult statusResult = parseBaseResult(node);
            status = statusResult.getStatus();
            if (Constant.OK.equals(status)) {
                ProtocolTreeNode eventContent = user.getTaskNotify().getEventContent(event);
                if (eventContent != null) {
                    status = eventContent.GetTag();
                }
            }
        } else {
            status = node.GetTag();
        }
        if (Constant.OK.equals(status)) {
            user.getGorgeousEngine().setPbNickname(nickName);
            return StatusResult.ok("成功");
        }
        return StatusResult.fail("失败");

    }
}
