package com.whatsapp.android.terminal.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.terminal.entity.Terminal;
import com.whatsapp.android.terminal.service.TerminalService;
import com.whatsapp.android.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sunnoc
 * @since 2020-07-28
 */
@Service
public class TerminalServiceImpl implements TerminalService {
    @Autowired
    ServiceConstant serviceConstant;

    @Override
    public String getTerminalInfo(String uuid) {
        return HttpUtils.get(serviceConstant.getServerAddr() + "/task/terminal/record/getTerminalInfo?uuid=" + uuid + "&platformType=" + Constant.PLATFORM_TYPE);
    }

    @Override
    public boolean saveTerminalInfo(Terminal terminal) {
        String body = HttpUtils.postToJson(serviceConstant.getServerAddr() + "/task/terminal/record/saveTerminalInfo", JSONObject.toJSONString(terminal));
        if (StringUtils.isEmpty(body)) {
            return false;
        }
        JSONObject jsonObject = JSONObject.parseObject(body);
        return jsonObject.getIntValue("code") == 200;
    }

    @Override
    public boolean updateTerminalInfo(Terminal terminal) {
        String body = HttpUtils.postToJson(serviceConstant.getServerAddr() + "/task/terminal/record/updateTerminalInfo", JSONObject.toJSONString(terminal));
        if (StringUtils.isEmpty(body)) {
            return false;
        }
        JSONObject jsonObject = JSONObject.parseObject(body);
        return jsonObject.getIntValue("code") == 200;
    }

    @Override
    public boolean getIpWhite() {
        try {
            String body = HttpUtils.get(serviceConstant.getServerAddr() + "/task/terminal/record/getIpWhiteList");
            if (StringUtils.isEmpty(body)) {
                return false;
            }
            JSONObject jsonObject = JSONObject.parseObject(body);
            int code = jsonObject.getIntValue("code");
            if (code == 200) {
                List<String> IpWhiteList = jsonObject.getJSONObject("data").getJSONArray("ipWhiteList").toJavaList(String.class);
                if (IpWhiteList != null) {
                    Constant.IP_WHITE_LIST.clear();
                    for (String ip : IpWhiteList) {
                        if (StringUtils.hasLength(ip)) {
                            Constant.IP_WHITE_LIST.add(ip);
                        }
                    }
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }
}
