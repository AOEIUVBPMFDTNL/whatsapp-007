package com.whatsapp.android;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.util.LibLoader;
import com.whatsapp.android.util.WhatsAppUtils;
import jni.NoiseJni;
import jni.ProtocolNodeJni;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author sunnoc
 * @date 2021-12-03 15:10
 */
@Slf4j
public class EncodeTest {
    public static void main(String[] args) {
        //System.out.println(HexUtil.encodeHex("273512610721984".getBytes(StandardCharsets.UTF_8)));

        String os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            LibLoader.loadLib("libNoiseJni.so");
        } else if (os.startsWith("Windows")) {
            LibLoader.loadLib("libNoiseJni.dll");
        } else if (os.startsWith("Mac")) {
            LibLoader.loadLib("libNoiseJni.dylib");
        }
        ProxyInfo proxyInfo = null;
        proxyInfo = new ProxyInfo();
        proxyInfo.setType(0);
        proxyInfo.setProxyHost("127.0.0.1");
        proxyInfo.setProxyPort(1080);
        StatusResult checkWhatsappVersion = WhatsAppUtils.checkWhatsappVersion(proxyInfo);
        log.info(JSONObject.toJSONString(checkWhatsappVersion));
        String initConfigPath = System.getProperty("user.dir") + "/jni/config.json";
        if (!NoiseJni.InitConfig(initConfigPath)) {
            log.error("初始化密码本配置文件失败");
            return;
        }
        //<notification from='120363282203687021@g.us' type='w:gp2' id='1115066482' participant='273512610721984@lid' notify='xxxsundog' t='1712636359'>
        ProtocolTreeNode decode = ProtocolNodeJni.Decode(0, "63", HexUtil.decodeHex("00f80e0906faff091203632822036870211c04ec2008fb05111506648205f70100ff88273512610721984f18fc0978787873756e646f671aff051712636359f801f806afec72fc1031373132363336333539333034343836ec26fc1031373132363336323736393636373436f801f805050cf70100ff88273512610721984f48fc202b3836e28899e28899e28899e28899e28899e28899e28899e28899e288993032"));
        System.out.println(decode);
        ProtocolTreeNode notification = new ProtocolTreeNode("notification");
        notification.AddAttribute(new StanzaAttribute("participant", "273512610721984@lid"));
        byte[] encode = ProtocolNodeJni.Encode(0, "63", notification);
        String hexStr = HexUtil.encodeHexStr(encode);
        System.out.println(hexStr);
        ProtocolTreeNode decoded = ProtocolNodeJni.Decode(0, "63", encode);
        System.out.println(decoded);

        //00f805070ef7000bff878615228092350f08fb0a3eb052b8b110fc109611

    }
}
