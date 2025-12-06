package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.profile.SecondAuthPack;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 二次验证
 *
 * @author sunnoc
 * @date 2021-05-15 13:25
 */
public class SecondAuthRequest extends AbstractRequest<StatusResult> {
    private SecondAuthPack secondAuthPack;

    public SecondAuthRequest(SecondAuthPack secondAuthPack) {
        this.secondAuthPack = secondAuthPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SECOND_AUTH;
    }

    @Override
    public boolean request() {
        user.getGorgeousEngine().open2FAuth(getTaskId(), secondAuthPack.getCode(), secondAuthPack.getEmail());
        return true;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        ProtocolTreeNode error = node.getOneChildren("error");
        if (ObjectUtil.isNotNull(error)) {
            // 修改或移除2fa失败
            return StatusResult.fail("修改或移除2fa失败");
        }
        return parseBaseResult(node);
    }
}
