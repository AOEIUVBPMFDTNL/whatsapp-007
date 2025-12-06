package com.whatsapp.android.api.register;

import ProtocolTree.ProtocolTreeNode;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.register.AndroidRegisterEnv;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.HttpClientUtil;
import com.whatsapp.android.util.WaRegisterHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * 安卓注册发送验证码
 *
 * @author sunnoc
 * @date 2023-06-27 16:46
 */
@Slf4j
public class CheckPhoneExistRequest extends AbstractRequest<SendSmsRegisterResult> {
    private final RegisterPack registerPack;
    private String username;
    private AndroidRegisterEnv androidRegisterEnv;
    private WaRegisterHelper waRegisterHelper;

    public CheckPhoneExistRequest(RegisterPack registerPack) {
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
    public SendSmsRegisterResult execute() {
        this.username = registerPack.getPhoneAreaCode() + registerPack.getPhone();
        this.waRegisterHelper = new WaRegisterHelper(username, registerPack);
        if (waRegisterHelper.getAndroidRegisterEnv() == null) {
            return new SendSmsRegisterResult(StatusResult.fail("环境生成失败"));
        }
        this.androidRegisterEnv = waRegisterHelper.getAndroidRegisterEnv();
        String content = JSONObject.toJSONString(androidRegisterEnv);
        String registerKey = cn.hutool.core.codec.Base64.encode(content);
        // regOnboardAbProp();
        HttpClientUtil.ResponseResult responseResult = waRegisterHelper.checkExistRequest();
        if (!responseResult.isSuccess()) {
            return new SendSmsRegisterResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
        }
        log.info("用户：{}，注册请求反馈结果：{}", username, responseResult.getResultString());
        JSONObject responseJson = JSONObject.parseObject(responseResult.getResultString());
        String reason = responseJson.getString("reason");
        if ("blocked".equals(reason)) {
            return new SendSmsRegisterResult(StatusResult.fail("账号处于封号中"));
        } else if ("length_long".equals(reason)) {
            return new SendSmsRegisterResult(StatusResult.fail("手机号有误"));
        } else if ("bad_param".equals(reason)) {
            return new SendSmsRegisterResult(StatusResult.fail("参数无效"));
        } else if ("old_version".equals(reason)) {
            return new SendSmsRegisterResult(StatusResult.fail("版本过旧，请更新版本"));
        }
        return new SendSmsRegisterResult(registerKey, responseJson, StatusResult.ok());
    }


    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public SendSmsRegisterResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
