package com.whatsapp.android.api.register;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.AccountReRegistrationResult;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.entity.response.register.SubmitRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.service.WaOldRegistrationService;
import com.whatsapp.android.util.SpringUtils;
import org.springframework.util.StringUtils;

public class AccountReRegistrationRequest extends AbstractRequest<AccountReRegistrationResult> {
    private final WaOldRegistrationService waOldRegistrationService = SpringUtils.getBean(WaOldRegistrationService.class);
    private final RegisterPack registerPack;

    public AccountReRegistrationRequest(RegisterPack registerPack) {
        this.registerPack = registerPack;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.ACCOUNT_RE_REGISTRATION;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public AccountReRegistrationResult execute() {
        //开启重注册开关
        user.setReRegistering(true);
        SendSmsRegisterResult checkPhoneExist = user.checkPhoneExistRequest(registerPack);
        if (!Constant.OK.equals(checkPhoneExist.getStatus())) {
            user.setReRegistering(false);
            return new AccountReRegistrationResult(StatusResult.fail(checkPhoneExist.getMessage()));
        }
        registerPack.setRegisterKey(checkPhoneExist.getRegisterKey());
        registerPack.setMethod("wa_old");
        JSONObject jsonObject = (JSONObject) checkPhoneExist.getData();
        int waOldEligible = jsonObject.getIntValue("wa_old_eligible");
        if (waOldEligible != 1) {
            user.setReRegistering(false);
            return new AccountReRegistrationResult(StatusResult.fail("账号暂不支持转环境"));
        }
        SendSmsRegisterResult sendSmsRegisterResult = user.sendSms(registerPack);
        if (!Constant.OK.equals(sendSmsRegisterResult.getStatus())) {
            return new AccountReRegistrationResult(StatusResult.fail(sendSmsRegisterResult.getMessage()));
        }
        String verifyCode = null;
        for (int i = 0; i < 30; i++) {
            assert waOldRegistrationService != null;
            verifyCode = waOldRegistrationService.getVerifyCode(user.getLoginPack().getUsername());
            if (StringUtils.hasLength(verifyCode)) {
                break;
            }
            ThreadUtil.sleep(1000);
        }
        if (!StringUtils.hasLength(verifyCode)) {
            user.setReRegistering(false);
            return new AccountReRegistrationResult(StatusResult.fail("验证码获取失败"));
        }
        registerPack.setVerifyCode(verifyCode);
        //退出账号
        user.stop();
        SubmitRegisterResult submitRegisterResult = user.submitRegisterRequest(registerPack);
        if (!Constant.OK.equals(submitRegisterResult.getStatus())) {
            user.setReRegistering(false);
            return new AccountReRegistrationResult(StatusResult.fail(submitRegisterResult.getMessage()));
        }
        return new AccountReRegistrationResult(registerPack.getRegisterKey(), StatusResult.ok());
    }

    @Override
    public AccountReRegistrationResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
