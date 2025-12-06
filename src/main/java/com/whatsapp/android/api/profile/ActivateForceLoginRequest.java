package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.entity.pack.account.LoginPack;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;
import com.whatsapp.android.util.RedisService;
import com.whatsapp.android.util.SpringUtils;

/**
 * 激活抢登
 */
public class ActivateForceLoginRequest extends AbstractRequest<StatusResult> {
    private final boolean activateForceLogin;

    public ActivateForceLoginRequest(boolean activateForceLogin) {
        this.activateForceLogin = activateForceLogin;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.ACTIVATE_FORCE_LOGIN;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public StatusResult execute() {
        LoginPack loginPack = user.getLoginPack();
        String username = loginPack.getUsername();
        boolean forceLogin = loginPack.isForceLogin();
        if (activateForceLogin && forceLogin) {
            return StatusResult.ok("当前已是抢登状态");
        }
        loginPack.setForceLogin(activateForceLogin);
        String key = "disConnectReLogin:" + loginPack.getUsername();
        KeyLockUtil.lock(key);
        try {
            if (UserRecord.getRecord().containsKey(username)) {
                UserRecord.getRecord().put(username, user);
                RedisService redisService = SpringUtils.getBean(RedisService.class);
                redisService.hset(Constant.USER_INFO_KEY, username, loginPack);
            }
        } catch (Exception ignored) {
            loginPack.setForceLogin(forceLogin);
            return StatusResult.fail("设置失败");
        } finally {
            KeyLockUtil.unlock(key);
        }
        return StatusResult.ok();
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
