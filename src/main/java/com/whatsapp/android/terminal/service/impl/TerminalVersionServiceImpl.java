package com.whatsapp.android.terminal.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.terminal.entity.InsTerminalVersion;
import com.whatsapp.android.terminal.service.TerminalVersionService;
import com.whatsapp.android.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-31
 */
@Service
public class TerminalVersionServiceImpl implements TerminalVersionService {
    @Autowired
    ServiceConstant serviceConstant;

    @Override
    public String getUpdateKey(String version) {
        String body = HttpUtils.get(serviceConstant.getServerAddr() + "/task/terminal/record/getUpdateKey?version=" + version + "&" + "platformType=" + Constant.PLATFORM_TYPE);
        if (StringUtils.isEmpty(body)) {
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(body);
        if (jsonObject.getIntValue("code") != 200) {
            return null;
        }
        return jsonObject.getString("data");
    }

    @Override
    public InsTerminalVersion getNewestVersion() {
        String body = HttpUtils.get(serviceConstant.getServerAddr() + "/task/terminal/record/getNewestVersion?platformType=" + Constant.PLATFORM_TYPE);
        if (StringUtils.isEmpty(body)) {
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(body);
        if (jsonObject.getIntValue("code") != 200) {
            return null;
        }
        return jsonObject.getObject("data", InsTerminalVersion.class);
    }
}
