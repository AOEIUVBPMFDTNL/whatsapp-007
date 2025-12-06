package ProtocolTree;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static ProtocolTree.XmppTokens.DOUBLE_BYTE;
import static ProtocolTree.XmppTokens.SINGLE_BYTE;

/**
 * xmpp解码
 *
 * @author Rocky
 */
@Slf4j
public class XmppEncode {
    private static final int BUFFER_SIZE = 0x2000;
    private static final Map<String, StringTag> map = makeBookToMap();
    private static final byte[] FIRST_BYTES = {0};

    private static void writeAttrLength(OutputStream outputStream, int length) throws Exception {
        if (length >= 0 && length < 0x100) {
            outputStream.write(length & 0xff);
        } else if (length >= 0x100 && length < 0x10000) {
            outputStream.write((0xff00 & length) >> 8);
            outputStream.write(length & 0xff);
        } else {
            throw new Exception("属性长度超出限制");
        }
    }

    private static void writeAttrLengthTag(OutputStream outputStream, int length) throws Exception {
        // 根据属性长度写入
        if (length == 0) {
            outputStream.write(0);
            return;
        }
        if (length < 0x100) {
            outputStream.write(0xf8);
            writeAttrLength(outputStream, length);
            return;
        }
        if (length < 0x10000) {
            outputStream.write(0xf9);
            writeAttrLength(outputStream, length);
            return;
        }
        throw new Exception("属性长度超出限制");
    }

    private static Map<String, StringTag> makeBookToMap() {
        // 将密码本使用hashMap进行保存
        Map<String, StringTag> passwordMap = new HashMap<>();
        for (int v = 0; true; ++v) {
            String[] arr_s = SINGLE_BYTE;
            if (v >= 0xec) {
                break;
            }
            String s1 = arr_s[v];
            if (!StrUtil.isEmpty(s1)) {
                passwordMap.put(s1, new StringTag(0, v, false));
            }
        }
        for (int v1 = 0; true; ++v1) {
            String[][] arr2_s = DOUBLE_BYTE;
            if (v1 >= 4) {
                break;
            }
            String[] arr_s1 = arr2_s[v1];
            for (int v2 = 0; v2 < arr_s1.length; ++v2) {
                String s2 = arr_s1[v2];
                if (!StrUtil.isEmpty(s2)) {
                    passwordMap.put(s2, new StringTag(v1 + 0xec, v2, true));
                }
            }
        }
        return passwordMap;
    }

    private static void writeInteropJid(OutputStream outputStream, String interopJidUser) throws Exception {
        // I-S.I1:I2@interop; 传入为 I-S.I1:I2
        // 先解析I-S.I1:I2(可能.I1会被省略)
        int v1 = interopJidUser.lastIndexOf(46);
        int v2 = interopJidUser.lastIndexOf(58);
        int v3 = interopJidUser.length();
        if (v2 == v3 - 1 || v1 == v3 - 1) {
            throw new Exception("解析InteropJid错误");
        }
        if (v2 != -1) {
            v3 = v2;
        }
        if (v1 == -1 || v1 >= v2 && v2 != -1) {
            v1 = v3;
        }
        int v4 = 0;
        if (v2 != -1) {
            // 多设备
            v4 = Integer.parseInt(interopJidUser.substring(v2 + 1));
        }
        // 需要判断InteropJid是否符合ws标准
        int v5 = interopJidUser.lastIndexOf("-");
        if (v5 == -1) {
            throw new Exception("interopJid 没有 -, 无法解析");
        }
        int v6 = 0;
        try {
            v6 = Integer.parseInt(interopJidUser.substring(0, v5));
        } catch (Exception e) {
            throw new Exception("interopJid -符号前应该为数字");
        }
        String id = interopJidUser.substring(v5 + 1, v1);
        if (!NumberUtil.isNumber(id)) {
            throw new Exception("interopJid -符号后应该为纯数字");
        }
        outputStream.write(0xf4);
        stringToXmppByte(outputStream, id, true, false);
        writeAttrLength(outputStream, v4);
        writeAttrLength(outputStream, v6);
        outputStream.write(0);
    }


    private static void writeString(OutputStream outputStream, byte[] arr_b, boolean z) throws Exception {
        int v5;
        int v;
        if (arr_b.length >= 0x100000) {
            outputStream.write(0xFE);
            outputStream.write((0x7F000000 & arr_b.length) >> 24);
            v = 0xFF0000;
            outputStream.write((v & arr_b.length) >> 16);
            outputStream.write((0xFF00 & arr_b.length) >> 8);
            outputStream.write(arr_b.length & 0xFF);
            outputStream.write(arr_b);
            return;

        } else {
//            if (jid)
            if (arr_b.length >= 0x100) {
                outputStream.write(0xfd);
                v = 0xF0000;
                outputStream.write((v & arr_b.length) >> 16);
                outputStream.write((0xFF00 & arr_b.length) >> 8);
                outputStream.write(arr_b.length & 0xFF);
                outputStream.write(arr_b);
                return;
            }
            if (z) {
                // z为true即为将arr_b hex化的操作, 先定义v1为0xff, 如果arr_b中出现了ABCDEF大写字母, 则为id的hex化v1则为0xfb
                int v1 = 0xff;
                if (arr_b.length >= 0x80) {
                    outputStream.write(0xFC);
                    writeAttrLength(outputStream, arr_b.length);
                    outputStream.write(arr_b);
                    return;
                }
                int v2 = (arr_b.length + 1) / 2;
                byte[] arr_b1 = new byte[v2];
                int v3 = 0;
                boolean flag = true;
                // 开始写入jidString
                while (v3 < arr_b.length) {
                    if (!flag) {
                        break;
                    }
                    int v4 = arr_b[v3];
                    switch (v4) {
                        case 0x2d:
                        case 0x2e:
                            v5 = v4 - 0x23;
                            if (v5 != -1) {
                                arr_b1[v3 / 2] = (byte) (((byte) (v5 << (1 - v3 % 2) * 4)) | arr_b1[v3 / 2]);
                                ++v3;
                                break;
                            }
                        case 0x30:
                        case 0x31:
                        case 0x32:
                        case 0x33:
                        case 0x34:
                        case 0x35:
                        case 0x36:
                        case 0x37:
                        case 0x38:
                        case 0x39:
                            v5 = v4 - 0x30;
                            if (v5 != -1) {
                                arr_b1[v3 / 2] = (byte) (((byte) (v5 << (1 - v3 % 2) * 4)) | arr_b1[v3 / 2]);
                                ++v3;
                                break;
                            }
                        case 58:
                            v5 = 12;
                            arr_b1[v3 / 2] = (byte) (((byte) (v5 << (1 - v3 % 2) * 4)) | arr_b1[v3 / 2]);
                            ++v3;
                            break;
                        case 65:
                        case 66:
                        case 67:
                        case 68:
                        case 69:
                        case 70:
                            // hex化的为uuid, tag为0xfb
                            v5 = v4 - 0x37;
                            if (v1 != 0xfb) {
                                v1 = 0xfb;
                            }
                            arr_b1[v3 / 2] = (byte) (((byte) (v5 << (1 - v3 % 2) * 4)) | arr_b1[v3 / 2]);
                            ++v3;
                            break;
                        default:
                            // 若出现了别的字符串则走0xf7即可
                            flag = false;
                            break;
                    }

                }
                if (!flag) {
                    outputStream.write(0xFC);
                    writeAttrLength(outputStream, arr_b.length);
                    outputStream.write(arr_b);
                    return;
                }
                if (arr_b.length % 2 == 1) {
                    arr_b1[v2 - 1] = (byte) (arr_b1[v2 - 1] | 15);
                }
                outputStream.write(v1);
                outputStream.write((arr_b.length & 1) << 7 | arr_b1.length);
                arr_b = arr_b1;
                outputStream.write(arr_b);
                return;
            }

        }
        outputStream.write(0xFC);
        writeAttrLength(outputStream, arr_b.length);
        outputStream.write(arr_b);
    }

    private static void writeJid(XmppJid xmppJid, OutputStream outputStream) throws Exception {
        String server = xmppJid.getServer();
        if ("interop".equals(server)) {
            writeInteropJid(outputStream, xmppJid.getUser());

        }
        if (xmppJid.getAgent() <= 0 && xmppJid.getDevice() <= 0) {
            outputStream.write(0xfa);
            if (StrUtil.isEmpty(xmppJid.getUser())) {
                // 发送单域名的时候, 如 "s.whatsapp.net"
                outputStream.write(0);
            } else {
                stringToXmppByte(outputStream, xmppJid.getUser(), true, false);
            }
            stringToXmppByte(outputStream, xmppJid.getServer(), false, false);
            return;
        }
        // 多设备逻辑
        outputStream.write(0xf7);
        int v = 0;
        if (xmppJid.getServer().equals("hosted") || xmppJid.getServer().equals("hosted.lid")) {
            v = 0x80;
        }
        if (xmppJid.getServer().equals("lid")) {
            v |= 1;
        }
        writeAttrLength(outputStream, v);
        writeAttrLength(outputStream, xmppJid.getDevice());
        stringToXmppByte(outputStream, xmppJid.getUser(), true, false);
    }

    private static void stringToXmppByte(OutputStream outputStream, String s, boolean z, boolean z1) throws Exception {
        byte[] arr_b;
        // 将xmpp字符串转为字节
        StringTag stringTag = map.get(s);
        if (stringTag == null) {
            // 不需要密码本进行序列化
            if (z1) {
                if (s == null) {
                    arr_b = null;
                    writeString(outputStream, arr_b, z);
                    return;
                }
                XmppJid xmppJid = XmppJid.of(s);
                if (xmppJid != null) {
                    writeJid(xmppJid, outputStream);
                    return;
                }
                arr_b = s.getBytes(StandardCharsets.UTF_8);
                writeString(outputStream, arr_b, z);
                return;
            }
            if (s == null) {
                arr_b = null;
                writeString(outputStream, arr_b, z);
                return;
            }
            arr_b = s.getBytes(StandardCharsets.UTF_8);
            writeString(outputStream, arr_b, z);
            return;
        }
        // 从密码本读取写入
        if (stringTag.A02) {
            int v3 = stringTag.A01;
            if (v3 >= 0 && v3 <= 0xff) {
                outputStream.write(((int) (((byte) v3))));
                int v4 = stringTag.A00;
                if (v4 >= 0 && v4 <= 0xff) {
                    outputStream.write(((int) (((byte) v4))));
                    return;
                }
                throw new Exception("StringTag A00 大于0xff");
            }
            throw new Exception("StringTag A01 大于0xff");
        }
        int v4 = stringTag.A00;
        if (v4 >= 0 && v4 <= 0xFF) {
            outputStream.write(((int) (((byte) v4))));
            return;
        }
        throw new Exception("StringTag A00 大于0xff");

    }


    private static void sendXmpp(ProtocolTreeNode protocolTreeNode, OutputStream outputStream) throws Exception {
        // 获取node的子节点
        LinkedList<ProtocolTreeNode> subNodes = protocolTreeNode.GetChildren();
        int v = 0;
        int sunNodesFlag;
        if (subNodes == null) {
            sunNodesFlag = 0;
        } else {
            sunNodesFlag = 1;
            if (subNodes.size() == 0) {
                sunNodesFlag = 0;
            }
        }
        // 获取node下的data
        byte[] data = protocolTreeNode.GetData();
        if (data != null) {
            ++sunNodesFlag;
            if (sunNodesFlag > 1) {
                throw new Exception("node 子节点或data不允许同时存在!!");
            }
        }
        // 获取node属性
        LinkedList<StanzaAttribute> nodeAttrs = protocolTreeNode.GetAttributes();
        // 写入变换后的属性数量
        writeAttrLengthTag(outputStream, (nodeAttrs == null ? 0 : nodeAttrs.size() * 2) + 1 + sunNodesFlag);
        // 写入标签字符串
        stringToXmppByte(outputStream, protocolTreeNode.GetTag(), false, true);
        // 写入属性
        if (nodeAttrs != null) {
            for (StanzaAttribute attr : nodeAttrs) {
                // 写入属性名
                stringToXmppByte(outputStream, attr.key_, false, false);
                // 判断value是否为jid
                XmppJid xmppJid = XmppJid.of(attr.value_);
                if (xmppJid != null) {
                    writeJid(xmppJid, outputStream);
                } else {
                    // 读取密码本或者直接写入byte
                    stringToXmppByte(outputStream, attr.value_, true, true);
                }
            }
        }
        // 如果body不为空, 则写入
        if (data != null) {
            writeString(outputStream, data, false);
            return;
        }
        // 写入子节点
        if (subNodes != null && !subNodes.isEmpty()) {
            // 写入子节点个数
            writeAttrLengthTag(outputStream, subNodes.size());
            while (true) {
                sendXmpp(subNodes.get(v), outputStream);
                ++v;
                if (v >= subNodes.size()) {
                    return;
                }
            }
        }
    }

    /**
     * xmpp 编码
     *
     * @param protocolTreeNode 协议节点
     * @return 字节数组
     */
    public static byte[] encode(ProtocolTreeNode protocolTreeNode) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        byteArrayOutputStream.write(FIRST_BYTES);
        sendXmpp(protocolTreeNode, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        String hex = "00f80c0906faff8685293791709f03040f08fb05240060145076f70100ff877258914205759f1aff051713429377f801f806af10fc0a323a494e447048336f2bfc0f6465766963655f6c69645f68617368fc0a323a6d52595667436778f802f8073b76f70101ff877258914205759f0cf70001ff8685293791709f5055f8032473fc0a31373133333730373838";
        log.info(hex);
        byte[] bytes = HexUtil.decodeHex(hex);
        ProtocolTreeNode protocolTreeNode = XmppDecode.decode(bytes);
        log.info(protocolTreeNode.toString());
        byte[] encode = XmppEncode.encode(protocolTreeNode);
        String ret = HexUtil.encodeHexStr(encode);
        log.info(ret);
        log.info(String.valueOf(hex.equals(ret)));
    }

}
