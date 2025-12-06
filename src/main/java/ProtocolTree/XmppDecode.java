package ProtocolTree;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;

import static ProtocolTree.XmppTokens.DOUBLE_BYTE;
import static ProtocolTree.XmppTokens.SINGLE_BYTE;

/**
 * xmpp解码
 *
 * @author Rocky
 */
@Slf4j
public class XmppDecode {
    private static final int BUFFER_SIZE = 0x2000;

    private static int getTagCalculatedLength(InputStream inputStream, int tag) throws IOException {
        switch (tag) {
            case 0:
                return 0;
            case 0xf8:
                return inputStream.read();
            case 0xf9:
                return (inputStream.read() << 8) + inputStream.read();
            default:
                throw new IOException("getTagCalculatedLength Error");
        }
    }

    private static int getTagByteArrayLength(InputStream inputStream, int tag) throws IOException {
        switch (tag) {
            case 0:
                return 0;
            case 0xfc:
                return inputStream.read();
            case 0xfd:
                return (inputStream.read() << 16) + (inputStream.read() << 8) + inputStream.read();
            default:
                throw new IOException("getTagByteArrayLength Error");
        }
    }

    private static void readByteArray(InputStream inputStream, byte[] arr_b) throws IOException {
        int v = 0;
        while (v < arr_b.length) {
            int v1 = inputStream.read(arr_b, v, arr_b.length - v);
            if (v1 != -1) {
                v += v1;
                continue;
            }
            throw new IOException("ran out of bytes while reading into buffer");
        }
    }

    private static byte[] getHexJidToString(InputStream inputStream, int tag) throws Exception {
        int v6;
        int v1 = inputStream.read() & 0xff;
        byte[] arr_b = new byte[v1 & 0x7F];
        readByteArray(inputStream, arr_b);
        int v3 = (v1 & 0x7F) * 2 - ((v1 & 0x80) == 0 ? 0 : 1);
        byte[] arr_b1 = new byte[v3];
        for (int v2 = 0; v2 < v3; ++v2) {
            int v4 = (1 - v2 % 2) * 4;
            int v5 = (arr_b[v2 / 2] & 15 << v4) >> v4;
            switch (tag) {
                case 0xFB: {
                    switch (v5) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9: {
                            v6 = v5 + 0x30;
                            break;
                        }
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15: {
                            v6 = v5 + 55;
                            break;
                        }
                        default:
                            throw new Exception("解析手机号异常");
                    }
                    break;
                }
                case 0xFF: {
                    switch (v5) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9: {
                            v6 = v5 + 0x30;
                            break;
                        }
                        case 10:
                        case 11: {
                            v6 = v5 + 35;
                            break;
                        }
                        case 12: {
                            v6 = 58;
                            break;
                        }
                        default:
                            throw new Exception("解析手机号异常");
                    }
                    break;

                }
                default: {
                    throw new Exception("解析手机号异常");
                }
            }
            arr_b1[v2] = (byte) v6;

        }

        return arr_b1;

    }

    private static String getJid(InputStream inputStream) throws Exception {
        String s = getStringFromTagId(inputStream, inputStream.read());
        String s1 = getStringFromTagId(inputStream, inputStream.read());
        return StrUtil.isEmpty(s) ? s1 : s + '@' + s1;
    }

    private static String getJidDevice(InputStream inputStream) throws Exception {
        // 获取多设备Jid或者lid
        int jidFlag = inputStream.read();
        int multiDeviceNum = inputStream.read();
        String stringFromTagId = getStringFromTagId(inputStream, inputStream.read());
        boolean z = false;
        int lidFlag = (jidFlag & 1) == 0 ? 1 : 0;
        if ((jidFlag & 0x80) != 0) {
            z = true;
        }
        if (z) {
            if (lidFlag != 0) {
                return StrUtil.isEmpty(stringFromTagId) ? "hosted" : stringFromTagId + (multiDeviceNum == 0 ? "" : ":" + multiDeviceNum) + '@' + "hosted";
            }
            return StrUtil.isEmpty(stringFromTagId) ? "hosted.lid" : (multiDeviceNum == 0 ? "" : ":" + multiDeviceNum) + '@' + "hosted.lid";
        }
        String s1 = lidFlag == 0 ? "lid" : "s.whatsapp.net";
        return StrUtil.isEmpty(stringFromTagId) ? s1 : stringFromTagId + "." + jidFlag + ":" + multiDeviceNum + '@' + s1;
    }


    private static String getStringFromTagId(InputStream inputStream, int tag) throws Exception {
        if (tag == 2) {
            return SINGLE_BYTE[tag];
        }
        if (tag != -1) {
            if (tag > 2) {
                if (tag < 0xec) {
                    // 读取第一个密码表
                    return SINGLE_BYTE[tag];
                }
                if (tag != 0xf7) {
                    switch (tag) {
                        case 0xec:
                        case 0xed:
                        case 0xee:
                        case 0xef:
                            int a01Index = inputStream.read();
                            // 读取第二个密码表
                            return DOUBLE_BYTE[tag - 0xec][a01Index];
                        case 0xfc:
                        case 0xfd:
                            int bytearrayLength = getTagByteArrayLength(inputStream, tag);
                            byte[] bytes = new byte[bytearrayLength];
                            readByteArray(inputStream, bytes);
                            return new String(bytes, StandardCharsets.UTF_8);
                        case 0xfb:
                        case 0xff:
                            // 读取手机号
                            byte[] hexJidToString = getHexJidToString(inputStream, tag);
                            return new String(hexJidToString, StandardCharsets.UTF_8);
                        default:
                            // 直接读取长度并读取字符串
                            byte[] bytes1 = new byte[((inputStream.read() & 0x7f) << 24) | (inputStream.read() << 16) | (inputStream.read() << 8) | inputStream.read()];
                            readByteArray(inputStream, bytes1);
                            return new String(bytes1, StandardCharsets.UTF_8);
                    }

                }
            }
        }
        if (tag == 0) {
            return null;
        }
        return getJidDevice(inputStream);
    }

    private static ProtocolTreeNode readXmppNode(InputStream inputStream) throws Exception {
        StanzaAttribute[] stanzaAttributes = new StanzaAttribute[0];
        int tag = inputStream.read();
        int tagCalculatedLength = getTagCalculatedLength(inputStream, tag);
        int tagId = inputStream.read();
        if (tagId == 2) {
            String stringFromTagId = getStringFromTagId(inputStream, tagId);
            return new ProtocolTreeNode(stringFromTagId);
        }
        String stringFromTagId = getStringFromTagId(inputStream, tagId);
        if (tagCalculatedLength != 0 && stringFromTagId != null) {
            int attributeLength = (tagCalculatedLength - 2 + tagCalculatedLength % 2) / 2;
            if (attributeLength == 0) {
                // 该节点下没有属性
                stanzaAttributes = null;
            } else {
                stanzaAttributes = new StanzaAttribute[attributeLength];
                // 开始读属性
                int readAttributeLength = 0;
                while (readAttributeLength < attributeLength) {
                    // 读取属性名字
                    String attrName = getStringFromTagId(inputStream, inputStream.read());
                    int attrValueTag = inputStream.read();
                    switch (attrValueTag) {
                        case 0xf4:
                            String f4String = getStringFromTagId(inputStream, inputStream.read());
                            int v5 = (inputStream.read() << 8) + inputStream.read();
                            int v6 = (inputStream.read() << 8) + inputStream.read();
                            if (inputStream.read() == 0) {
                                if (!StrUtil.isNotEmpty(f4String)) {
                                    throw new Exception("0xf4节点获取字符串失败");
                                }
                                String f4Jid = v6 + "-" + f4String + (v5 == 0 ? "" : ":" + v5) + '@' + "interop";
                                stanzaAttributes[readAttributeLength] = new StanzaAttribute(attrName, f4Jid);
                                readAttributeLength++;
                                continue;
                            }
                        case 0xf7:
                            // 多设备模式下的jid或者lid
                            String jidDevice = getJidDevice(inputStream);
                            stanzaAttributes[readAttributeLength] = new StanzaAttribute(attrName, jidDevice);
                            readAttributeLength++;
                            continue;

                        case 0xfa:
                            String jid = getJid(inputStream);
                            stanzaAttributes[readAttributeLength] = new StanzaAttribute(attrName, jid);
                            readAttributeLength++;
                            continue;
                        default:
                            // 不是上面的tag的话直接读即可
                            String attrValueString = getStringFromTagId(inputStream, attrValueTag);
                            stanzaAttributes[readAttributeLength] = new StanzaAttribute(attrName, attrValueString);
                            readAttributeLength++;
                    }
                }
            }

        }
        if (tagCalculatedLength % 2 == 1) {
            return new ProtocolTreeNode(stringFromTagId, stanzaAttributes);
        }
        // 嵌套下的子节点
        int v7 = inputStream.read();
        switch (v7) {
            case 0:
            case 0xf8:
            case 0xf9: {
                // 新的node节点
                int subTagCalculatedLength = getTagCalculatedLength(inputStream, v7);
                ProtocolTreeNode[] protocolTreeNodes = new ProtocolTreeNode[subTagCalculatedLength];
                for (int i = 0; i < subTagCalculatedLength; ++i) {
                    protocolTreeNodes[i] = readXmppNode(inputStream);
                }
                return new ProtocolTreeNode(stringFromTagId, protocolTreeNodes, stanzaAttributes);
            }
            case 0xfc:
                // body节点
                int fcTagLength = inputStream.read();
                byte[] fc_arr_b = new byte[fcTagLength];
                readByteArray(inputStream, fc_arr_b);
                return new ProtocolTreeNode(stringFromTagId, stanzaAttributes, fc_arr_b);

            case 0xfd:
                // body节点
                int fdTagLength = ((inputStream.read() & 0xf) << 16) + (inputStream.read() << 8) + inputStream.read();
                byte[] fd_arr_b = new byte[fdTagLength];
                readByteArray(inputStream, fd_arr_b);
                return new ProtocolTreeNode(stringFromTagId, stanzaAttributes, fd_arr_b);

            case 0xfe:
                break;
            case 0xfb:
            case 0xff:
                return new ProtocolTreeNode(stringFromTagId, stanzaAttributes, getHexJidToString(inputStream, v7));
            default:
                return new ProtocolTreeNode(stringFromTagId, stanzaAttributes, getStringFromTagId(inputStream, v7).getBytes(StandardCharsets.UTF_8));
        }
        throw new Exception("readXmppNode 解析异常");
    }

    private static byte[] unZlib(byte[] bytes) throws IOException {
        byte firstByte = bytes[0];
        if ((firstByte & 2) != 0) {
            try (
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes, 1, bytes.length - 1);
                    InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
            ) {
                byte[] firstBytes = {firstByte};
                byteArrayOutputStream.write(firstBytes);
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inflaterInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                return byteArrayOutputStream.toByteArray();
            }
        }
        return bytes;
    }

    /**
     * xmpp解码
     *
     * @param xmpp xmpp字节数组
     * @return ProtocolTreeNode
     */
    public static ProtocolTreeNode decode(byte[] xmpp) throws Exception {
        byte[] bytes = unZlib(xmpp);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            int read = byteArrayInputStream.read();
            if (((byte) read & 1) != 0) {
                throw new RuntimeException("解析异常, 不支持节分段");
            }
            ProtocolTreeNode protocolTreeNode = readXmppNode(byteArrayInputStream);
            if (ObjectUtil.isNull(protocolTreeNode)) {
                throw new RuntimeException("解析异常");
            }
            return protocolTreeNode;
        }
    }

    public static void main(String[] args) throws Exception {
        String hex = "00f8100906faff091203632620828911351c04ec2008fb05332117410105f70100ff075530214994742012ecdf18fc1056696b6173205961647576616e7368691aff051712555497f801f806afec72fc1031373132353535343937343636313336ec26fc1031373132353535343836303531383739f801f805050cf70100ff075530214994742048fc1d2b3931e28899e28899e28899e28899e28899e28899e28899e288993637";
        byte[] bytes = HexUtil.decodeHex(hex);
        ProtocolTreeNode protocolTreeNode = XmppDecode.decode(bytes);
        log.info(protocolTreeNode.toString());
    }


}
