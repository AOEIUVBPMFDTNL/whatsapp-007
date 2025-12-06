package ProtocolTree;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * xmpp jid解析类
 *
 * @author Rocky
 */
@Data
public class XmppJid {
    // 用户名一般为手机号
    private String user;
    // 域名
    private String server;
    // 多设备
    private int device;
    private int agent;

    public XmppJid(String user, String server, int device, int agent) {
        this.user = user;
        this.server = server;
        this.device = device;
        this.agent = agent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XmppJid xmppJid = (XmppJid) o;
        return device == xmppJid.device && agent == xmppJid.agent && Objects.equals(user, xmppJid.user) && Objects.equals(server, xmppJid.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, server, device, agent);
    }

    @Override
    public String toString() {
        String user = StringUtils.hasLength(this.user) ? this.user : "";
        String device = String.format(":%s", this.device);
        String agent;
        if (StringUtils.hasLength(device)) {
            agent = String.format(".%s", this.agent);
        } else {
            agent = this.agent != 0 ? String.format(".%s", this.agent) : "";
        }
        String leading = String.format("%s%s%s", user, agent, device);
        return leading.isEmpty() ? server : String.format("%s@%s", leading, server);
    }

    public String toShortString() {
        if (device != 0) {
            return toString();
        } else {
            return String.format("%s@%s", user, server);
        }
    }

    public String toLongString() {
        if (device != 0) {
            return toString();
        } else {
            return String.format("%s.%s:%s@%s", user, agent, device, server);
        }
    }

    public static XmppJid of(String jidString) {
        // 将string转为XmppJid对象
        switch (jidString) {
            case "s.whatsapp.net":
            case "g.us":
                //case "call":
                return new XmppJid(null, jidString, 0, 0);
        }
        // 判断是否有@字符
        int flag = jidString.indexOf("@");
        if (flag < 1) {
            if (flag != -1) {
                return null;
            }
            if (jidString.equals("status_me") || jidString.equals("lid_me")) {
                return new XmppJid(null, jidString, 0, 0);
            } else {
                return null;
            }
        }
        // 切割@字符
        int v = jidString.lastIndexOf("@");
        String s1 = jidString.substring(0, v);
        String s2 = jidString.substring(v + 1);
        // 根据不同状况进行判断
        // 先判断无需考虑多设备的域名
        switch (s2) {
            case "broadcast":
            case "g.us":
            case "temp":
            case "newsletter":
                return new XmppJid(s1, s2, 0, 0);
            case "s.whatsapp.net":
            case "hosted":
            case "hosted.lid":
            case "lid":
            case "interop":
                // 考虑多设备的情况
                int v1 = s1.lastIndexOf(".");
                int v2 = s1.lastIndexOf(":");
                if (v2 == -1 && v1 == -1 && !"lid".equals(s2)) {
                    // 不是多设备情况
                    return new XmppJid(s1, s2, 0, 0);
                }
                // 组装多设备XmppJid
                int v3 = s1.length();
                if (v2 == v3 - 1 || v1 == v3 - 1) {
                    return null;
                }
                if (v2 != -1) {
                    v3 = v2;
                }
                if (v1 == -1 || v1 >= v2 && v2 != -1) {
                    v1 = v3;
                }
                int v4 = 0;
                // 获取设备值
                if (v2 != -1) {
                    try {
                        v4 = Integer.parseInt(s1.substring(v2 + 1));
                    } catch (NumberFormatException unused_ex) {
                        return null;
                    }
                }
                String user = s1.substring(0, v1);
                if ("lid".equals(s2) || "hosted.lid".equals(s2)) {
                    return new XmppJid(user, s2, v4, 1);
                } else {
                    return new XmppJid(user, s2, v4, 0);
                }
        }
        return null;
    }

    /**
     * 判断是否为lid
     */
    public static boolean isIncognitoJid(String id) {
        if (ObjectUtil.isNull(id)) {
            return false;
        }
        return id.contains("lid") || id.contains("hosted.lid");
    }

    public static void main(String[] args) throws Exception {
        XmppJid xmppJid = XmppJid.of("status@newsletter");
        System.out.println(xmppJid);

    }
}
