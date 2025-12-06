package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.date.DateUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.SubscribeResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.UserIdSubscribeRecord;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

/**
 * 查询用户在线时间
 *
 * @author sunnoc
 * @date 2021-03-12 18:32
 */
public class SubscribeRequest extends AbstractRequest<SubscribeResult> {
    private String userId;

    public SubscribeRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SUBSCRIBE;
    }

    @Override
    public boolean request() {
        String userIdNormalize = WhatsAppUtils.JidNormalize(this.userId);
        if (StringUtils.hasLength(userIdNormalize)) {
            String lastOnlineTime = null;
            GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
            UserIdSubscribeRecord userIdSubscribeRecord = gorgeousEngine.getUserIdSubscribeRecord();
            if (userIdSubscribeRecord != null) {
                lastOnlineTime = userIdSubscribeRecord.get(user.getLoginPack().getUsername(), userIdNormalize);
            }
            if (StringUtils.hasLength(lastOnlineTime)) {
                if ("online".equals(lastOnlineTime)) {
                    lastOnlineTime = DateUtil.date(DateUtil.currentSeconds() * 1000).toString();
                } else if ("unknown".equals(lastOnlineTime)) {
                    user.getGorgeousEngine().Subscribe(this.userId);
                    return true;
                }
                ProtocolTreeNode node = ProtocolTreeNode.success(Constant.OK);
                node.AddAttribute(new StanzaAttribute("lastOnlineTime", lastOnlineTime));
                user.getTaskNotify().setEventContent(getTaskId(), node);
            } else {
                user.getGorgeousEngine().Subscribe(this.userId);
            }
            return true;
        }
        return false;
    }

    @Override
    public long timeOut() {
        return 15;
    }

    @Override
    public SubscribeResult parseResult(ProtocolTreeNode node) {
        SubscribeResult checkResult = checkResult(node, SubscribeResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        //<presence from='8618688676082@s.whatsapp.net' type='unavailable' last='1615534787'/>
        String lastOnlineTime = node.GetAttributeValue("lastOnlineTime");
        if (StringUtils.hasLength(lastOnlineTime)) {
            String invisible = node.GetAttributeValue("invisible");
            return new SubscribeResult(StatusResult.ok(), lastOnlineTime, WhatsAppUtils.JidNormalize(userId), "invisible".equals(invisible));
        } else {
            return new SubscribeResult(StatusResult.fail());
        }

    }
}
