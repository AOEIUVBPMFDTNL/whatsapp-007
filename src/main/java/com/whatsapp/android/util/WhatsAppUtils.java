package com.whatsapp.android.util;


import Env.DeviceEnv;
import Message.WhatsMessage;
import axolotl.AxolotlManager;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.OssConstant;
import com.whatsapp.android.entity.*;
import com.whatsapp.android.entity.pack.register.AndroidRegisterEnv;
import com.whatsapp.android.entity.pack.register.RegisterPack;
import com.whatsapp.android.entity.response.register.SendSmsRegisterResult;
import com.whatsapp.android.run.StartedUpRunner;
import com.whatsapp.android.util.ffmpeg.FfmpegUtil;
import com.whatsapp.android.util.ffmpeg.MultimediaInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import jni.NoiseJni;
import jni.Register;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.springframework.data.util.Version;
import org.springframework.util.StringUtils;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author sunnoc
 * @date 2021-03-13 22:59
 */
@Slf4j
public class WhatsAppUtils {
    private static final int ANDROID_ID_LENGTH = 32;
    private static final int IOS_ID_LENGTH = 20;
    private static final String IOS_PREFIX = "3A";
    private static final int CHECKSUM_LENGTH = 8;

    public static String JidNormalize(String jid) {
        int pos = jid.indexOf("@");
        if (pos != -1) {
            return jid;
        }
        return jid + "@s.whatsapp.net";
    }

    public static StatusResult checkWhatsappVersion(ProxyInfo proxyInfo) {
        String jniDir = System.getProperty("user.dir") + "/jni";
        String status;
        if (proxyInfo != null) {
            if (proxyInfo.getType() == 0) {
                if (StringUtils.hasLength(proxyInfo.getProxyUser())) {
                    status = NoiseJni.CheckWhatsappVersion("http", proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd(), jniDir);
                } else {
                    status = NoiseJni.CheckWhatsappVersion("http", proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), "", "", jniDir);
                }
            } else {
                if (StringUtils.hasLength(proxyInfo.getProxyUser())) {
                    status = NoiseJni.CheckWhatsappVersion("socks5", proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), proxyInfo.getProxyUser(), proxyInfo.getProxyPwd(), jniDir);
                } else {
                    status = NoiseJni.CheckWhatsappVersion("socks5", proxyInfo.getProxyHost(), proxyInfo.getProxyPort(), "", "", jniDir);
                }
            }

        } else {
            status = NoiseJni.CheckWhatsappVersion("", "", 0, "", "", jniDir);
        }
        log.info("校验WhatsApp版本：{}", status);
        if ("whatsapp version is older".equals(status)) {
            String errMsg = "校验whatsApp版本太旧了";
            log.error(errMsg);
            return StatusResult.fail(errMsg);
        }
        if (!"success".equals(status)) {
            String errMsg = "校验whatsApp版本失败，可能代理ip存在问题";
            log.error(errMsg);
            return StatusResult.fail(errMsg);
        }
        return StatusResult.ok();
    }

    public static byte[] modifyFileMd5(byte[] file) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byteArrayOutputStream.write(file);
            byteArrayOutputStream.write(RandomUtil.randomBytes(4));
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return file;
        }
    }

    public static byte[] modifyTxtMd5(byte[] file) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byteArrayOutputStream.write(file);
            int randomInt = RandomUtil.randomInt(1, 100);
            for (int i = 0; i < randomInt; i++) {
                byteArrayOutputStream.write((byte) 0);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return file;
        }
    }

    /**
     * 校验代理
     *
     * @param proxyInfo proxyInfo
     * @return boolean
     */
    public static StatusResult checkProxyIp(ProxyInfo proxyInfo) {
        WaProxyCheck.StatusResult check = WaProxyCheck.check(proxyInfo);
        if (check.isSuccess()) {
            return StatusResult.ok(check.getMessage());
        } else {
            return StatusResult.fail(check.getMessage());
        }
    }

    /**
     * 处理代理ip异常反馈信息
     */
    public static String proxyErrorHandle(String errMsg) {
        if (StrUtil.indexOf(errMsg, "Connection refused", 0, true) != -1) {
            return "网络异常，代理连接被拒绝";
        } else if (StrUtil.indexOf(errMsg, "ClientProtocolException", 0, true) != -1) {
            return "代理ip协议错误";
        } else if (StrUtil.indexOf(errMsg, "ConnectTimeoutException", 0, true) != -1) {
            return "代理ip连接超时异常";
        } else if (StrUtil.indexOf(errMsg, "NotSslRecordException", 0, true) != -1) {
            return "SSL/TLS通信异常";
        } else {
            return "代理ip异常";
        }
    }

    /**
     * 校验是否安装ffmpeg
     */
    public static boolean checkIsInstallFfmpeg() {
        try {
            String info = RuntimeUtil.execForStr("ffmpeg -version");
            return StrUtil.indexOf(info, "ffmpeg version", 0, false) != -1;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 图片缩略图截取
     *
     * @param inFile  输入文件(包括完整路径)
     * @param outFile 输出文件(可包括完整路径)
     * @return outFilePath
     */
    public static String imageCapture(String inFile, String outFile) {
        String command = "ffmpeg -y -i " + inFile + " -vf scale=200:200:force_original_aspect_ratio=decrease -q:v 75 " + outFile;
        String str = RuntimeUtil.execForStr(command);
        if (str == null || "".equals(str)) {
            log.error("ffmpeg转换获取未获取到信息");
            return null;
        }
        String s = StrUtil.subBetween(str, "Input #0", "Output #0");
        if (s == null || "".equals(s)) {
            log.error("ffmpeg转换获取未获取到信息");
            return null;
        }
        return outFile;
    }

    /**
     * 图片转jpg格式
     */
    public static String imageToJpg(String path) {
        String outPath = path + "_converted.jpg";
        String command = "ffmpeg -y -i " + path + " " + outPath;
        exeFfmpeg(command);
        return outPath;
    }

    /**
     * 视频缩略图截取
     *
     * @param inFile  输入文件(包括完整路径)
     * @param outFile 输出文件(可包括完整路径)
     * @return multimediaInfo
     */
    public static MultimediaInfo videoCapture(String inFile, String outFile) {
        //1代表从第一秒开始截取
        //String command = "/Applications/ffmpeg -ss 1 -i " + inFile + " -y -f image2 -update 1 " + outFile;
        String command = "ffmpeg -ss 1 -i " + inFile + " -y -f image2 -update 1 -q 75 " + outFile;
        return exeFfmpeg(command);
    }

    public static MultimediaInfo mp3ToOpus(String inFile, String outFile) {
        String command = "ffmpeg -i " + inFile + " -y -ar 48000 -ac 1 -acodec libopus -ab 256k " + outFile;
        return exeFfmpeg(command);
    }

    public static MultimediaInfo videoGifCapture(String inFile, String outFile) {
        // gif 一般不到1秒 不能用上面的的函数来提取
        // ffmpeg -i input.mp4 -vframes 1 -q:v 2 output.jpg
        String command = "ffmpeg -i " + inFile + " -vframes 1 -q:v 2 " + outFile;
        return exeFfmpeg(command);
    }

    private static MultimediaInfo exeFfmpeg(String command) {
        String str = RuntimeUtil.execForStr(command);
        if (str == null || "".equals(str)) {
            log.error("ffmpeg转换获取未获取到信息");
            return null;
        }
        String s = StrUtil.subBetween(str, "Input #0", "Output #0");
        if (s == null || "".equals(s)) {
            log.error("ffmpeg转换获取未获取到信息");
            return null;
        }
        s = "Input #0" + s;
        List<String> strings = Arrays.asList(StrUtil.split(s, System.lineSeparator()));
        return FfmpegUtil.parseMultimediaInfo(strings);
    }

    /**
     * 获取语音消息长度
     */
    public static int getVoiceLength(byte[] bytes) {
        int len = 0;
        try (FFmpegFrameGrabber fg = new FFmpegFrameGrabber(IoUtil.toStream(bytes))) {
            fg.start();
            len = (int) (fg.getLengthInTime() / 1000 / 1000);
        } catch (FrameGrabber.Exception e) {
            log.error("An exception has occured during video preprocessing: " + e.getMessage());
        }
        return len;
    }

    /**
     * 生成文字引用消息
     */
    public static WhatsMessage.WhatsAppContextInfo.Builder generateQuotedMsg(String participant, String quotedMsgId, String quotedMsgContent, boolean unicode) {
        WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
        contextInfoBuilder.setStanzaId(quotedMsgId);
        contextInfoBuilder.setParticipant(participant);
        WhatsMessage.WhatsAppMessage.Builder messageBuilder = WhatsMessage.WhatsAppMessage.newBuilder();
        if (unicode) {
            messageBuilder.setConversation(UnicodeUtil.toString(StrUtil.replace(quotedMsgContent, "%u", "\\u")));
        } else {
            messageBuilder.setConversation(quotedMsgContent);
        }
        contextInfoBuilder.setQuotedMessage(messageBuilder);
        return contextInfoBuilder;
    }

    /**
     * 引用媒体消息
     */
    public static WhatsMessage.WhatsAppContextInfo.Builder generateMediaQuotedMsg(String participant, String quotedMsgId, String quotedMediaMessage) {
        WhatsMessage.WhatsAppContextInfo.Builder contextInfoBuilder = WhatsMessage.WhatsAppContextInfo.newBuilder();
        contextInfoBuilder.setStanzaId(quotedMsgId);
        contextInfoBuilder.setParticipant(participant);
        try {
            WhatsMessage.WhatsAppMessage whatsAppMessage = WhatsMessage.WhatsAppMessage.parseFrom(cn.hutool.core.codec.Base64.decode(quotedMediaMessage));
            contextInfoBuilder.setQuotedMessage(whatsAppMessage);
        } catch (Exception ignore) {
        }
        return contextInfoBuilder;
    }

    /**
     * 生成@人contextInfo
     */
    public static WhatsMessage.WhatsAppContextInfo.Builder generateMentionedMsg(List<String> mentioned, WhatsMessage.WhatsAppContextInfo.Builder builder) {
        if (ObjectUtil.isNotNull(mentioned) && !mentioned.isEmpty()) {
            if (builder == null) {
                builder = WhatsMessage.WhatsAppContextInfo.newBuilder();
            }
            for (String s : mentioned) {
                builder.addMentionedJid(JidNormalize(s));
            }
            return builder;
        }
        return builder;
    }


    public static String generateMediaQuoteMessage(WhatsMessage.WhatsAppMessage whatsAppMessage) {
        WhatsMessage.WhatsAppMessage.Builder builder = whatsAppMessage.toBuilder().clearMessageContextInfo();
        if (builder.hasImageMessage()) {
            WhatsMessage.WhatsAppImageMessage.Builder imageBuilder = builder.getImageMessage().toBuilder();
            imageBuilder.clearJpegThumbnail();
            builder.setImageMessage(imageBuilder);
        }
        if (builder.hasVideoMessage()) {
            WhatsMessage.WhatsAppVideoMessage.Builder videoMessageBuilder = builder.getVideoMessageBuilder();
            videoMessageBuilder.clearJpegThumbnail();
            builder.setVideoMessage(videoMessageBuilder);
        }

        return cn.hutool.core.codec.Base64.encode(builder.build().toByteArray());
    }

    public static void applyEphemeralMessage(WhatsMessage.WhatsAppContextInfo.Builder builder, int expirationTime) {
        builder.setExpiredTime(expirationTime);
        builder.setTimestamp((int) (System.currentTimeMillis() / 1000));
    }

    /**
     * 校验是否是离线消息
     *
     * @param loginTime 登陆时间
     * @param msgTime   收到消息时间
     * @return boolean
     */
    public static boolean checkIsOfflineMessage(long loginTime, long msgTime) {
        if (Convert.toStr(loginTime, "0").length() == 10) {
            loginTime = loginTime * 1000;
        }
        return loginTime == 0 || loginTime > msgTime;
    }


    /**
     * 获取安卓版本
     */
    public static WaVersion getWaVersion(boolean businessVersion, DeviceEnv.AndroidEnv.Builder envBuilder_, boolean isoLogin) {
        WaVersion androidWaVersionFromEnv = getAndroidWaVersionFromEnv(envBuilder_);
        if (androidWaVersionFromEnv == null) {
            return null;
        }
        int channel = businessVersion ? 1 : 0;
        String url;
        if (StartedUpRunner.environment == 3 || StartedUpRunner.environment == 2) {
            url = "http://tk.zc.lu/terminalRecordTest/getWaVersion?version=" + androidWaVersionFromEnv.getVersion() +
                    "&releaseVersion=" + androidWaVersionFromEnv.getReleaseVersion() + "&channel=" + channel + "&device=" + (isoLogin ? "1" : "0");
        } else {
            url = "http://tk.zc.lu/terminalRecord/getWaVersion?version=" + androidWaVersionFromEnv.getVersion() +
                    "&releaseVersion=" + androidWaVersionFromEnv.getReleaseVersion() + "&channel=" + channel + "&device=" + (isoLogin ? "1" : "0");
        }
        try {
            String content = HttpUtils.get(url);
            JSONObject jsonObject = JSONObject.parseObject(content);
            int code = jsonObject.getIntValue("code");
            if (code == 200) {
                String msg = jsonObject.getString("msg");
                if (StringUtils.hasLength(msg)) {
                    String[] split = StrUtil.split(msg, ":");
                    String version = split[0];
                    Integer releaseVersion = Integer.parseInt(split[1]);
                    if (androidWaVersionFromEnv.getVersion().equals(version)) {
                        return androidWaVersionFromEnv;
                    }
                    return new WaVersion(version, releaseVersion);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 刷新wa注册版本信息
     */
    public static boolean refreshRegisterVersionInfo() {
        WaRegisterVersionInfo waRegisterVersionInfo = getWaRegisterVersionInfo();
        if (waRegisterVersionInfo == null) {
            return false;
        }
        String version = waRegisterVersionInfo.getVersion();
        String classesMd5Base64 = waRegisterVersionInfo.getClassesMd5Base64();
        Register.SetNormalVersion(version);
        Register.SetNormalMd5(classesMd5Base64);
        return true;
    }

    /**
     * 获取wa注册版本信息
     */
    private static WaRegisterVersionInfo getWaRegisterVersionInfo() {
        String url;
        if (StartedUpRunner.environment == 3 || StartedUpRunner.environment == 2) {
            url = "http://tk.zc.lu/terminalRecordTest/getWaRegisterVersionInfo";
        } else {
            url = "http://tk.zc.lu/terminalRecord/getWaRegisterVersionInfo";
        }
        try {
            String content = HttpUtils.get(url);
            log.info("刷新wa注册版本信息：{}", content);
            JSONObject jsonObject = JSONObject.parseObject(content);
            int code = jsonObject.getIntValue("code");
            if (code == 200) {
                return jsonObject.getObject("data", WaRegisterVersionInfo.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 从环境获取安卓版本
     */
    private static WaVersion getAndroidWaVersionFromEnv(DeviceEnv.AndroidEnv.Builder envBuilder_) {
        DeviceEnv.AppVersion.Builder useragentBuilder = envBuilder_.getUserAgentBuilder().getAppVersionBuilder();
        DeviceEnv.UserAgent.Builder userAgentBuilder = envBuilder_.getUserAgentBuilder();
        int platform = userAgentBuilder.getPlatform().getNumber();
        int primary = useragentBuilder.getPrimary();
        int secondary = useragentBuilder.getSecondary();
        int tertiary = useragentBuilder.getTertiary();
        int quaternary = useragentBuilder.getQuaternary();
        DeviceEnv.ReleaseChannel releaseChannel = envBuilder_.getUserAgentBuilder().getReleaseChannel();
        Integer releaseVersion = releaseChannel.getNumber();
        String version = primary + "." + secondary + "." + tertiary + "." + quaternary;
        return new WaVersion(version, releaseVersion);
    }

    public static String format(String hexStr, String separator) {
        final int length = hexStr.length();
        final StringBuilder builder = StrUtil.builder(length + length / 2);
        builder.append(separator).append(hexStr.charAt(0)).append(hexStr.charAt(1));
        for (int i = 2; i < length - 1; i += 2) {
            builder.append(separator).append(hexStr.charAt(i)).append(hexStr.charAt(i + 1));
        }
        return builder.toString();
    }

    /**
     * map转换为url
     *
     * @param map map
     * @return string
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        String key;
        Object value;
        String valueStr;
        for (Map.Entry<String, ?> item : map.entrySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }
            key = item.getKey();
            value = item.getValue();
            if (value instanceof Iterable) {
                value = CollectionUtil.join((Iterable<?>) value, ",");
            } else if (value instanceof Iterator) {
                value = CollectionUtil.join((Iterator<?>) value, ",");
            }
            valueStr = Convert.toStr(value);
            if (StrUtil.isNotEmpty(key)) {
                sb.append(key).append("=");
                if (StrUtil.isNotEmpty(valueStr)) {
                    sb.append(valueStr);
                }
            }
        }
        return sb.toString();
    }

    public static DefaultHttpHeaders applyRegisterHeaders(String version, String osVersion, String deviceName) {
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/" + version + " iOS/" + osVersion + " Device/" + deviceName);
        entries.set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
        entries.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        entries.set(HttpHeaderNames.ACCEPT_LANGUAGE, "en-us");
        entries.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate, br");
        entries.set(HttpHeaderNames.CONNECTION, "keep-alive");
        return entries;
    }

    public static DefaultHttpHeaders applyAndroidRegisterHeaders(String version, String osVersion, String deviceName, boolean businessVersion) {
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        if (businessVersion) {
            entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/" + version + " SMBA/" + osVersion + " Device/" + deviceName);
        } else {
            entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/" + version + " Android/" + osVersion + " Device/" + deviceName);
        }
        entries.set("WaMsysRequest", "1");
        entries.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip");
        entries.set(HttpHeaderNames.CONNECTION, "keep-alive");
        return entries;
    }

    public static DefaultHttpHeaders applyIosRegisterHeaders(String version, String osVersion, String deviceName, boolean businessVersion) {
        DefaultHttpHeaders entries = new DefaultHttpHeaders();
        if (businessVersion) {
            entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/" + version + " SMB iOS/" + osVersion + " Device/" + deviceName);
        } else {
            entries.set(HttpHeaderNames.USER_AGENT, "WhatsApp/" + version + " iOS/" + osVersion + " Device/" + deviceName);
        }
        entries.set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
        entries.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        entries.set(HttpHeaderNames.ACCEPT_LANGUAGE, "en-us");
        entries.set(HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate, br");
        entries.set(HttpHeaderNames.CONNECTION, "keep-alive");
        return entries;
    }

    /**
     * @param jid 粉丝id
     * @param ios 是否是ios登录
     * @return msgId
     */
    public static String generateFeedMsgId(String jid, boolean ios) {
        if (ios) {
            String sourceMsgId = generateIosMsgId(jid);
            return sourceMsgId.substring(0, sourceMsgId.length() - 4) + Constant.NT_MSG_TAG_FEED;
        } else {
            String sourceMsgId = IdUtil.simpleUUID().toUpperCase();
            //替换内容不要修改它
            String sub = StrUtil.sub(sourceMsgId, 11, 21);
            return StrUtil.replace(sourceMsgId, sub, Constant.NT_MSG_TAG_NEW);
        }
    }

    /**
     * 养号消息允许转发聊天室生成msgId
     *
     * @param userId 粉丝id
     * @param ios    是否是ios登录
     * @return msgId
     */
    public static String generateFeedMsgIdToLTS(String userId, boolean ios) {
        if (ios) {
            String iosMsgId = generateIosMsgId(userId);
            String subId = iosMsgId.substring(0, IOS_ID_LENGTH - CHECKSUM_LENGTH);
            String checksum = calculateSystemChecksum(subId);
            return subId + checksum;
        }
        String androidMsgId = generateAndroidMsgId();
        String subId = androidMsgId.substring(0, ANDROID_ID_LENGTH - CHECKSUM_LENGTH);
        String checksum = calculateSystemChecksum(subId);
        return subId + checksum;
    }

    /**
     * 验证是否为系统消息
     */
    public static boolean isSystemMessage(String msgId) {
        // 验证长度
        if (msgId.length() != ANDROID_ID_LENGTH && msgId.length() != IOS_ID_LENGTH) {
            return false;
        }
        // iOS消息需要验证前缀
        if (msgId.length() == IOS_ID_LENGTH && !msgId.startsWith(IOS_PREFIX)) {
            return false;
        }
        String body = msgId.substring(0, msgId.length() - CHECKSUM_LENGTH);
        String checksum = msgId.substring(msgId.length() - CHECKSUM_LENGTH);
        String expectedChecksum = calculateSystemChecksum(body);
        return checksum.equals(expectedChecksum);
    }

    /**
     * 计算系统消息校验和
     */
    private static String calculateSystemChecksum(String body) {
        byte[] hash = DigestUtil.sha1(body);
        return String.format("%0" + CHECKSUM_LENGTH + "X", new BigInteger(1, hash).mod(BigInteger.valueOf(0xFFFFFF)));
    }

    /**
     * 生成android消息id
     */
    public static String generateAndroidMsgId() {
        return IdUtil.simpleUUID().toUpperCase(Locale.ROOT);
    }

    /**
     * IOS msgId生成
     *
     * @param jid 粉丝id
     * @return msgId
     */
    public static String generateIosMsgId(String jid) {
        long timeMillis = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, timeMillis);
        byte[] array = buffer.array();
        ByteBuffer byteBuffer = ByteBuffer.wrap(array, 0, 8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        String substring = Long.toUnsignedString(byteBuffer.getLong()).substring(0, 8);
        byte[] bytes1 = (substring + JidNormalize(jid)).getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = RandomUtil.randomBytes(16);
        byte[] bytes3 = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bytes3, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bytes3, bytes1.length, bytes2.length);
        return Constant.IOS_MSG_PREFIX + DigestUtil.md5Hex(bytes3).substring(0, 18).toUpperCase();
    }

    public static String generateMsgId(String userId, boolean ios) {
        if (ios) {
            return generateIosMsgId(userId);
        }
        return generateAndroidMsgId();
    }

    /**
     * pb int 压缩
     */
    public static byte[] intToVarInt(int n) {
        List<Byte> list = new ArrayList<>();
        while (true) {
            if ((n & ~0x7F) == 0) {
                list.add((byte) n);
                break;
            } else {
                list.add((byte) ((n & 0x7F) | 0x80));
                n >>>= 7;
            }
        }
        byte[] bytes = new byte[list.size()];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }

    public static SendSmsRegisterResult generateRegisterDbEnv(String username, RegisterPack registerPack, AndroidRegisterEnv androidRegisterEnv, JSONObject responseJson) {
        ByteString edgeRoutingInfo;
        String chatDnsDomain;
        if (responseJson.containsKey("edge_routing_info")) {
            edgeRoutingInfo = ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getString("edge_routing_info")));
        } else {
            edgeRoutingInfo = ByteString.copyFrom(Base64.getDecoder().decode("CA0IDA=="));
        }
        if (!responseJson.containsKey("chat_dns_domain")) {
            chatDnsDomain = "fb";
        } else {
            chatDnsDomain = responseJson.getString("chat_dns_domain");
        }
        String envPath = System.getProperty("user.dir") + "/out/register/" + IdUtil.simpleUUID();
        AxolotlManager axolotlManager = null;
        try {
            //初始化数据表
            axolotlManager = new AxolotlManager(envPath, username);
            //创建 env数据
            DeviceEnv.AndroidEnv.Builder envBuild = DeviceEnv.AndroidEnv.newBuilder();
            envBuild.setChatDnsDomain(chatDnsDomain);
            envBuild.setFullphone(username);
            DeviceEnv.UserAgent.Builder useragentBuild = DeviceEnv.UserAgent.newBuilder();
            useragentBuild.setPlatform(DeviceEnv.Platform.ANDROID);
            useragentBuild.setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
            DeviceInfo deviceInfo = androidRegisterEnv.getDeviceInfo();
            useragentBuild.setOsVersion(deviceInfo.getVersion());
            useragentBuild.setManufacturer(deviceInfo.getManufacturer());
            useragentBuild.setDevice(deviceInfo.getDevice());
            useragentBuild.setOsBuildNumber(deviceInfo.getBuild());
            envBuild.setUserAgent(useragentBuild);
            if (registerPack.isBusinessVersion()) {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_ANDROID);
            } else {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.ANDROID);
            }
            DeviceEnv.UserAgent.Builder userAgentBuilder = DeviceEnv.UserAgent.newBuilder();
            DeviceEnv.AppVersion.Builder appVersionBuilder = userAgentBuilder.getAppVersionBuilder();
            String[] versions = androidRegisterEnv.getVersion().split("\\.");
            appVersionBuilder.setPrimary(Integer.parseInt(versions[0]));
            appVersionBuilder.setSecondary(Integer.parseInt(versions[1]));
            appVersionBuilder.setTertiary(Integer.parseInt(versions[2]));
            if (versions.length >= 4) {
                appVersionBuilder.setQuaternary(Integer.parseInt(versions[3]));
            }
            userAgentBuilder.setMcc(androidRegisterEnv.getMcc());
            userAgentBuilder.setMnc(androidRegisterEnv.getMnc());
            userAgentBuilder.setLocaleLanguageIso6391(androidRegisterEnv.getIso639());
            userAgentBuilder.setLocaleCountryIso31661Alpha2(androidRegisterEnv.getIso3166());
            envBuild.setPushname("");
            envBuild.setEdgeRoutingInfo(edgeRoutingInfo);
            envBuild.getUserAgentBuilder().setPhoneId(UUID.randomUUID().toString());
            envBuild.setFdid(androidRegisterEnv.getFdId());
            envBuild.setExpid(ByteString.copyFrom(androidRegisterEnv.getExpId()));

            DeviceEnv.KeyPair.Builder keyBuild = envBuild.getClientStaticKeyPairBuilder();
            keyBuild.setStrPrivateKey(ByteString.copyFrom(androidRegisterEnv.getPrivateKey()));
            keyBuild.setStrPubKey(ByteString.copyFrom(androidRegisterEnv.getPublicKey()));

            IdentityKeyPair identityKeyPair = new IdentityKeyPair(androidRegisterEnv.getIdentityKeyPair());
            SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(androidRegisterEnv.getSignedPreKey());
            axolotlManager.setRegisterSecretKey(identityKeyPair, androidRegisterEnv.getRegistrationId(), signedPreKeyRecord);
            axolotlManager.SetBytesSetting("env", envBuild.build().toByteArray());
            axolotlManager.Close();
            try {
                File envFile = new File(envPath);
                OssService ossService = SpringUtils.getBean(OssService.class);
                boolean success = ossService.uploadOssEnvFile("env/" + username + ".db", envFile);
                if (success) {
                    String url = OssConstant.PRE_BUCKET_URL + "env/" + username + ".db";
                    AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
                    String envEncryptKey = aes.encryptBase64(url);
                    return new SendSmsRegisterResult(username, envEncryptKey, true, StatusResult.ok());
                } else {
                    log.error("注册账号上传失败：{}", username);
                }
            } catch (Exception e) {
                log.error("转换异常", e);
            }
        } catch (Exception e) {
            log.error("转换异常", e);
        } finally {
            try {
                if (axolotlManager != null) {
                    axolotlManager.Close();
                }
            } catch (Exception ignore) {
            }
            FileUtil.del(envPath);
        }
        return new SendSmsRegisterResult(username, StatusResult.fail());
    }

    public static SendSmsRegisterResult generateRegisterJsonEnv(String username, RegisterPack registerPack, AndroidRegisterEnv androidRegisterEnv, JSONObject responseJson) {
        ByteString edgeRoutingInfo;
        String chatDnsDomain;
        if (responseJson.containsKey("edge_routing_info")) {
            edgeRoutingInfo = ByteString.copyFrom(Base64.getDecoder().decode(responseJson.getString("edge_routing_info")));
        } else {
            edgeRoutingInfo = ByteString.copyFrom(Base64.getDecoder().decode("CA0IDA=="));
        }
        if (!responseJson.containsKey("chat_dns_domain")) {
            chatDnsDomain = "fb";
        } else {
            chatDnsDomain = responseJson.getString("chat_dns_domain");
        }
        try {
            //创建 env数据
            DeviceEnv.AndroidEnv.Builder envBuild = DeviceEnv.AndroidEnv.newBuilder();
            envBuild.setChatDnsDomain(chatDnsDomain);
            envBuild.setFullphone(username);
            DeviceEnv.UserAgent.Builder useragentBuild = DeviceEnv.UserAgent.newBuilder();
            useragentBuild.setPlatform(DeviceEnv.Platform.ANDROID);
            useragentBuild.setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
            DeviceInfo deviceInfo = androidRegisterEnv.getDeviceInfo();
            useragentBuild.setOsVersion(deviceInfo.getVersion());
            useragentBuild.setManufacturer(deviceInfo.getManufacturer());
            useragentBuild.setDevice(deviceInfo.getDevice());
            useragentBuild.setOsBuildNumber(deviceInfo.getBuild());
            envBuild.setUserAgent(useragentBuild);
            if (registerPack.isBusinessVersion()) {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.SMB_ANDROID);
            } else {
                envBuild.getUserAgentBuilder().setPlatform(DeviceEnv.Platform.ANDROID);
            }
            DeviceEnv.UserAgent.Builder userAgentBuilder = DeviceEnv.UserAgent.newBuilder();
            DeviceEnv.AppVersion.Builder appVersionBuilder = userAgentBuilder.getAppVersionBuilder();
            String[] versions = androidRegisterEnv.getVersion().split("\\.");
            appVersionBuilder.setPrimary(Integer.parseInt(versions[0]));
            appVersionBuilder.setSecondary(Integer.parseInt(versions[1]));
            appVersionBuilder.setTertiary(Integer.parseInt(versions[2]));
            if (versions.length >= 4) {
                appVersionBuilder.setQuaternary(Integer.parseInt(versions[3]));
            }
            userAgentBuilder.setMcc(androidRegisterEnv.getMcc());
            userAgentBuilder.setMnc(androidRegisterEnv.getMnc());
            userAgentBuilder.setLocaleLanguageIso6391(androidRegisterEnv.getIso639());
            userAgentBuilder.setLocaleCountryIso31661Alpha2(androidRegisterEnv.getIso3166());
            envBuild.setPushname("");
            envBuild.setEdgeRoutingInfo(edgeRoutingInfo);
            envBuild.getUserAgentBuilder().setPhoneId(UUID.randomUUID().toString());
            envBuild.setFdid(androidRegisterEnv.getFdId());
            envBuild.setExpid(ByteString.copyFrom(androidRegisterEnv.getExpId()));

            DeviceEnv.KeyPair.Builder keyBuild = envBuild.getClientStaticKeyPairBuilder();
            keyBuild.setStrPrivateKey(ByteString.copyFrom(androidRegisterEnv.getPrivateKey()));
            keyBuild.setStrPubKey(ByteString.copyFrom(androidRegisterEnv.getPublicKey()));
            String env = cn.hutool.core.codec.Base64.encode(envBuild.build().toByteArray());
            String signedPreKeyRecord = cn.hutool.core.codec.Base64.encode(new SignedPreKeyRecord(androidRegisterEnv.getSignedPreKey()).serialize());
            String identityKeyPair = cn.hutool.core.codec.Base64.encode(new IdentityKeyPair(androidRegisterEnv.getIdentityKeyPair()).serialize());
            return new SendSmsRegisterResult(username, env, signedPreKeyRecord, identityKeyPair, androidRegisterEnv.getRegistrationId(), true, StatusResult.ok());
        } catch (Exception e) {
            log.error("转换异常", e);
        }
        return new SendSmsRegisterResult(username, StatusResult.fail());
    }

    /**
     * urlencode 字节数组
     **/
    public static String urlEncode(byte[] unencodedBytes) {

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < unencodedBytes.length; i++) {

            if (((unencodedBytes[i] >= 'a') && (unencodedBytes[i] <= 'z')) || ((unencodedBytes[i] >= 'A') && (unencodedBytes[i] <= 'Z')) || ((unencodedBytes[i] >= '0') && (unencodedBytes[i] <= '9')) || (unencodedBytes[i] == '.') || (unencodedBytes[i] == '-') || (unencodedBytes[i] == '*') || (unencodedBytes[i] == '_')) {
                buffer.append((char) unencodedBytes[i]);
            } else if (unencodedBytes[i] == ' ') {
                buffer.append('+');
            } else {
                buffer.append(String.format("%%%02x", unencodedBytes[i]));
            }

        }

        return buffer.toString();
    }

    /**
     * urlencode 字符串
     **/
    @SneakyThrows
    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public static boolean isVersionLessThan(String now, String old) {
        Version parse = Version.parse(now);
        return parse.isLessThan(Version.parse(old));
    }

    public static boolean isGreaterThan(String now, String old) {
        Version parse = Version.parse(now);
        return parse.isGreaterThan(Version.parse(old));
    }

    /**
     * 根据动态代理ip获取mcc mnc
     *
     * @param proxyInfo 代理
     * @return mcc mnc
     */
    public static MccMnc getMccMncFromProxyIp(String username, ProxyInfo proxyInfo) {
        String proxyPwd = proxyInfo.getProxyPwd();
        String country = StrUtil.subBetween(proxyPwd, "country-", "_");
        if (!StringUtils.hasLength(country)) {
            log.info("用户: {}, 获取代理ip国家信息失败", username);
            return null;
        }
        MccMnc mccMnc = DeviceUtil.getMccMnc(country);
        mccMnc.setCountry(country);
        log.info("用户: {}, 代理国家: {}, MCC: {}, MNC: {}", username, country, mccMnc.getMcc(), mccMnc.getMnc());
        return mccMnc;
    }

    public static String changeProxy(String proxyPwd) {
        String password = StrUtil.subBetween(proxyPwd, "session-", "_lifetime");
        if (!StringUtils.hasLength(password)) {
            return proxyPwd;
        }
        String randomstring = RandomUtil.randomString("0123456789abcdef", 8);
        return StrUtil.replace(proxyPwd, password, randomstring);
    }

    public static String changeRolaProxy(String proxyUser, String randomValue) {
        String randomstring = RandomUtil.randomString("0123456789abcdef", 10);
        return StrUtil.replace(proxyUser, randomValue, randomstring);
    }

    public static void main(String[] args) {
        String msgId = generateFeedMsgIdToLTS("fasdfas", true);
        System.out.println(msgId + "：" + isSystemMessage(msgId));
        msgId = generateMsgId("fasdfas", true);
        System.out.println(msgId + "：" + isSystemMessage(msgId));
        /*AtomicInteger num = new AtomicInteger();
        for (int i = 0; i < 100000000; i++) {
           String msgId = generateAndroidMsgId();
            if (isSystemMessage(msgId)) {
                num.incrementAndGet();
            }
        }
        System.out.println("系统Android消息ID出现次数：" + num.get());
        num = new AtomicInteger();
        for (int i = 0; i < 100000000; i++) {
            String msgId = generateIosMsgId("fasdfasdfas");
            if (isSystemMessage(msgId)) {
                num.incrementAndGet();
            }
        }
        System.out.println("系统Ios消息ID出现次数：" + num.get());*/
    }

    private static Map<String, String> parseCdnTrace(String content) {
        Map<String, String> dataMap = new HashMap<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length == 2) {
                dataMap.put(parts[0], parts[1]);
            }
        }
        return dataMap;
    }
}
