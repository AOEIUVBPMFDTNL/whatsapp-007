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
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class SendSmsRequest extends AbstractRequest<SendSmsRegisterResult> {
    private final RegisterPack registerPack;
    private String username;
    private AndroidRegisterEnv androidRegisterEnv;
    private WaRegisterHelper waRegisterHelper;

    public SendSmsRequest(RegisterPack registerPack) {
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
        username = registerPack.getPhoneAreaCode() + registerPack.getPhone();
        String registerKey = registerPack.getRegisterKey();
        String content = cn.hutool.core.codec.Base64.decodeStr(registerKey);
        androidRegisterEnv = JSONObject.parseObject(content, AndroidRegisterEnv.class);
        waRegisterHelper = new WaRegisterHelper();
        waRegisterHelper.setUsername(username);
        waRegisterHelper.setRegisterPack(registerPack);
        waRegisterHelper.setAndroidRegisterEnv(androidRegisterEnv);
        HttpClientUtil.ResponseResult responseResult = waRegisterHelper.sendSmsRequest();
        log.info("用户：{}，注册发送验证码反馈结果：{}", username, responseResult.getResultString());
        return handlerSendSmsResponse(responseResult, registerKey, true);
    }

    private SendSmsRegisterResult handlerSendSmsResponse(HttpClientUtil.ResponseResult responseResult, String registerKey, boolean first) {
        if (!responseResult.isSuccess()) {
            return new SendSmsRegisterResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
        }
        JSONObject responseJson = JSONObject.parseObject(responseResult.getResultString());
        String status = responseJson.getString("status");
        String reason;
        if ("sent".equals(status)) {
            return new SendSmsRegisterResult(registerKey, responseJson, StatusResult.ok());
        } else if ("ok".equals(status)) {
            //代表账号已经可以使用
            return WhatsAppUtils.generateRegisterDbEnv(username, registerPack, androidRegisterEnv, responseJson);
        } else if ("fail".equals(status)) {
            reason = responseJson.getString("reason");
            if ("too_recent".equals(reason)) {
                int smsWait = responseJson.getIntValue("sms_wait");
                return new SendSmsRegisterResult(registerKey, StatusResult.fail("请求太频繁，等待" + smsWait + "秒"));
            } else if ("old_version".equals(reason)) {
                return new SendSmsRegisterResult(StatusResult.fail("版本过旧，请更新版本"));
            } else if ("no_routes".equals(reason)) {
                return new SendSmsRegisterResult(StatusResult.fail("wa发送短信失败"));
            } else if (!"code_checkpoint".equals(reason)) {
                return new SendSmsRegisterResult(registerKey, StatusResult.fail(responseResult.getErrMsg()));
            }
            if (!first) {
                return new SendSmsRegisterResult(StatusResult.fail("失败：" + reason));
            }
            boolean verifySuccess = false;
            for (int i = 0; i < 5; i++) {
                responseResult = waRegisterHelper.getImageCaptcha();
                if (!responseResult.isSuccess()) {
                    continue;
                }
                responseJson = JSONObject.parseObject(responseResult.getResultString());
                String imageBlob = responseJson.getString("image_blob");
                if (!StringUtils.hasLength(imageBlob)) {
                    continue;
                }
                String captcha = waRegisterHelper.ocrCaptcha(imageBlob);
                if (!(StringUtils.hasLength(captcha) && captcha.length() == 3 && captcha.matches("[0-9]+"))) {
                    continue;
                }
                responseResult = waRegisterHelper.sendCaptchaVerifyRequest(captcha);
                log.info("用户：{}，注册校验图片验证码请求结果：{}", username, responseResult.getResultString());
                if (!responseResult.isSuccess()) {
                    continue;
                }
                responseJson = JSONObject.parseObject(responseResult.getResultString());
                //{"flash_type":0,"voice_wait":0,"voice_length":6,"sms_wait":0,"sms_length":6,"email_otp_eligible":0,"wa_old_eligible":0,"login":"639296813098","status":"verified"}
                status = responseJson.getString("status");
                if (!"verified".equals(status)) {
                    continue;
                }
                verifySuccess = true;
                break;
            }
            if (!verifySuccess) {
                return new SendSmsRegisterResult(StatusResult.fail("人机验证失败"));
            }
            //发送短信验证码
            responseResult = waRegisterHelper.sendSmsRequest();
            return handlerSendSmsResponse(responseResult, registerKey, false);
        }
        return new SendSmsRegisterResult(StatusResult.fail());
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
