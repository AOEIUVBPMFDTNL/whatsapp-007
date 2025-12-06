package com.whatsapp.android.api.register;

import ProtocolTree.ProtocolTreeNode;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.register.CheckPhoneBlockedPack;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.CheckPhoneBlockedResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.HttpClientUtil;
import com.whatsapp.android.util.PhoneAreaCodeSearchUtil;
import com.whatsapp.android.util.WaRegisterHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 校验手机号是否被封号
 *
 * @author sunnoc
 * @date 2021-11-04 18:10
 */
@Slf4j
public class CheckPhoneBlockedRequest extends AbstractRequest<CheckPhoneBlockedResult> {
    private final CheckPhoneBlockedPack checkPhoneBlockedPack;

    public CheckPhoneBlockedRequest(CheckPhoneBlockedPack checkPhoneBlockedPack) {
        this.checkPhoneBlockedPack = checkPhoneBlockedPack;

    }

    @Override
    public String funcName() {
        return null;
    }

    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public CheckPhoneBlockedResult execute() {
        String username = checkPhoneBlockedPack.getPhone();
        ProxyInfo proxyInfo = checkPhoneBlockedPack.getProxyInfo();
        String phoneAreaCode = checkPhoneBlockedPack.getCountryCode();
        if (StringUtils.isEmpty(phoneAreaCode)) {
            phoneAreaCode = PhoneAreaCodeSearchUtil.getPhoneAreaCode(username);
            if (StringUtils.isEmpty(phoneAreaCode)) {
                return new CheckPhoneBlockedResult(StatusResult.fail("获取手机区号失败"));
            }
        }
        RegisterPack registerPack = new RegisterPack();
        String phone = username.substring(phoneAreaCode.length());
        registerPack.setPhoneAreaCode(phoneAreaCode);
        registerPack.setPhone(phone);
        registerPack.setProxyInfo(proxyInfo);
        registerPack.setUsername(username);
        registerPack.setBusinessVersion(true);
        WaRegisterHelper waRegisterHelper = new WaRegisterHelper(username, registerPack);
        if (waRegisterHelper.getAndroidRegisterEnv() == null) {
            return new CheckPhoneBlockedResult(StatusResult.fail("环境生成失败"));
        }
        HttpClientUtil.ResponseResult responseResult = waRegisterHelper.checkExistRequest();
        if (!responseResult.isSuccess()) {
            return new CheckPhoneBlockedResult(StatusResult.fail("失败：" + responseResult.getErrMsg()));
        }
        log.info("用户：{}，注册请求反馈结果：{}", username, responseResult.getResultString());
        JSONObject responseJson = JSONObject.parseObject(responseResult.getResultString());
        String reason = responseJson.getString("reason");
        boolean possibleMigration;
        String cert = responseJson.getString("cert");
        boolean business = false;
        if (StringUtils.hasLength(cert)) {
            possibleMigration = true;
            business = true;
        } else {
            possibleMigration = responseJson.getBooleanValue("possible_migration");
        }
        CheckPhoneBlockedResult checkPhoneBlockedResult;
        if ("blocked".equals(reason)) {
            checkPhoneBlockedResult = new CheckPhoneBlockedResult(StatusResult.ok(), phoneAreaCode, true);
        } else if ("length_long".equals(reason)) {
            checkPhoneBlockedResult = new CheckPhoneBlockedResult(StatusResult.fail("手机号有误"), phoneAreaCode, false);
        } else if ("bad_param".equals(reason)) {
            //{"login":"1644832489","param":"backup_token","reason":"bad_param","status":"fail"}
            checkPhoneBlockedResult = new CheckPhoneBlockedResult(StatusResult.fail("查询失败"), phoneAreaCode, false);
        } else if ("old_version".equals(reason)) {
            checkPhoneBlockedResult = new CheckPhoneBlockedResult(StatusResult.fail("版本过旧，请更新版本"), phoneAreaCode, false);
        } else {
            checkPhoneBlockedResult = new CheckPhoneBlockedResult(StatusResult.ok(), phoneAreaCode, false, possibleMigration, business);
        }
        return checkPhoneBlockedResult;

    }

    @Override
    public CheckPhoneBlockedResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
