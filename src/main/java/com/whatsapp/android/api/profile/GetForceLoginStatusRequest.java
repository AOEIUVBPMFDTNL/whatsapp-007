package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.entity.response.profile.ForceLoginStatus;
import com.whatsapp.android.request.AbstractRequest;

/**
 * 获取抢登状态
 */
public class GetForceLoginStatusRequest extends AbstractRequest<ForceLoginStatus> {

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_FORCE_LOGIN_STATUS;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public ForceLoginStatus execute() {
        LoginPack loginPack = user.getLoginPack();
        boolean forceLogin = loginPack.isForceLogin();
        return new ForceLoginStatus(StatusResult.ok(), forceLogin);
    }

    @Override
    public ForceLoginStatus parseResult(ProtocolTreeNode node) {
        return null;
    }
}