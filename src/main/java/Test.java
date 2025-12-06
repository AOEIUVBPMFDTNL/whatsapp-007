import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.HexUtil;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.util.LibLoader;

import com.whatsapp.android.util.WhatsAppUtils;
import jni.NoiseJni;
import jni.ProtocolNodeJni;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @author sunnoc
 * @date 2021-03-06 11:01
 */
@Slf4j
public class Test {
    public static void main(String[] args) {
        try {
            String os = System.getProperty("os.name");
            if (os.startsWith("Linux")) {
                LibLoader.loadLib("libNoiseJni.so");
            } else if (os.startsWith("Windows")) {
                System.load("C:\\YT\\ideaProjects\\whatsapp-android\\src\\main\\resources\\lib\\libNoiseJni.dll");
            } else if (os.startsWith("Mac")) {
                LibLoader.loadLib("libNoiseJni.dylib");
            }
            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setType(0);
            proxyInfo.setProxyHost("139.84.130.10");
            proxyInfo.setProxyPort(39119);
            proxyInfo.setProxyUser("fans007");
            proxyInfo.setProxyPwd("fans888");
            StatusResult checkWhatsappVersion = WhatsAppUtils.checkWhatsappVersion(proxyInfo);
            boolean succ = NoiseJni.InitConfig("C:\\YT\\ideaProjects\\whatsapp-android\\src\\main\\resources\\lib\\config.json");
            byte[] pack = HexUtil.decodeHex("00f809190429165708c711fa0003");
            System.out.println(Arrays.toString(pack));

            ProtocolTreeNode decode = ProtocolNodeJni.Decode(1, "53", pack);
            System.out.println(decode);
        } catch (Exception ignored) {
        }
    }
}
