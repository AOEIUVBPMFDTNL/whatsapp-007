package com.whatsapp.android.api.register;

import ProtocolTree.ProtocolTreeNode;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.register.AndroidRegisterEnv;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.entity.response.register.SubmitRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.HttpClientUtil;
import com.whatsapp.android.util.WaRegisterHelper;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 提交注册
 *
 * @author sunnoc
 * @date 2021-03-21 21:23
 */
@Slf4j
public class SubmitRegisterRequest extends AbstractRequest<SubmitRegisterResult> {
    private final RegisterPack registerPack;

    public SubmitRegisterRequest(RegisterPack registerPack) {
        this.registerPack = registerPack;
    }

    @Override
    public String funcName() {
        return null;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public SubmitRegisterResult execute() {
        String username = registerPack.getPhoneAreaCode() + registerPack.getPhone();
        String registerKey = registerPack.getRegisterKey();
        String content = cn.hutool.core.codec.Base64.decodeStr(registerKey);
        AndroidRegisterEnv androidRegisterEnv = JSONObject.parseObject(content, AndroidRegisterEnv.class);
        String verifyCode = registerPack.getVerifyCode();
        WaRegisterHelper waRegisterHelper = new WaRegisterHelper();
        waRegisterHelper.setUsername(username);
        waRegisterHelper.setRegisterPack(registerPack);
        waRegisterHelper.setAndroidRegisterEnv(androidRegisterEnv);
        HttpClientUtil.ResponseResult responseResult = waRegisterHelper.sendRegisterRequest(verifyCode);
        if (!responseResult.isSuccess()) {
            return new SubmitRegisterResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
        }
        log.info("用户：{}，注册请求反馈结果：{}", username, responseResult.getResultString());
        JSONObject responseJson = JSONObject.parseObject(responseResult.getResultString());
        String status = responseJson.getString("status");
        String reason;
        if ("ok".equals(status)) {
            //代表账号已经可以使用
            SendSmsRegisterResult sendSmsRegisterResult = WhatsAppUtils.generateRegisterDbEnv(username, registerPack, androidRegisterEnv, responseJson);
            if (Constant.FAIL.equals(sendSmsRegisterResult.getStatus())) {
                return new SubmitRegisterResult(StatusResult.fail(sendSmsRegisterResult.getMessage()));
            }
            return new SubmitRegisterResult(username, sendSmsRegisterResult.getEnvEncryptKey(), StatusResult.ok());
        } else if ("fail".equals(status)) {
            reason = responseJson.getString("reason");
            if ("old_version".equals(reason)) {
                return new SubmitRegisterResult(StatusResult.fail("版本过旧，请更新版本"));
            } else if ("mismatch".equals(reason)) {
                return new SubmitRegisterResult(StatusResult.fail("验证码有误"));
            }
            return new SubmitRegisterResult(StatusResult.fail("失败：" + reason));
        }
        return new SubmitRegisterResult(StatusResult.fail());
    }

    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public SubmitRegisterResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
