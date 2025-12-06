package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.SubscribeResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;
import org.springframework.util.StringUtils;

import java.util.Locale;


/**
 * 取消订阅
 *
 * @author sunnoc
 * @date 2021-07-01 11:23
 */
public class UnSubscribeRequest extends AbstractRequest<StatusResult> {
    private String userId;
    private final String randomTaskId = IdUtil.simpleUUID().toUpperCase(Locale.ROOT);

    public UnSubscribeRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String getTaskId() {
        return randomTaskId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.UN_SUBSCRIBE;
    }

    @Override
    public boolean request() {
        String userIdNormalize = WhatsAppUtils.JidNormalize(this.userId);
        if (StringUtils.hasLength(userIdNormalize)) {
            user.getGorgeousEngine().unSubscribe(this.userId);
            return true;
        }
        return false;
    }

    @Override
    public boolean onlyRequest() {
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        SubscribeResult checkResult = checkResult(node, SubscribeResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        return StatusResult.ok();
    }
}
