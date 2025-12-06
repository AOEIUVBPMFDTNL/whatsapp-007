package com.whatsapp.android;

import Env.DeviceEnv;
import axolotl.AxolotlManager;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.util.*;
import jni.NoiseJni;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author sunnoc
 * @date 2021-03-18 11:19
 */
@Slf4j
public class EnvEncrypt {
    public static void main(String[] args) {
        encrypted();
    }

    public static void encrypted() {
        String content = "[\"66958129618\"]";
        List<String> list = JSONObject.parseArray(content).toJavaList(String.class);
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            String url = "https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + s + ".db";
            AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
            String encryptBase64 = aes.encryptBase64(url);
            stringBuilder.append(s).append("----").append(encryptBase64).append("\n");
            // System.out.println(s + "----" + encryptBase64);
        }
        FileUtil.writeString(stringBuilder.toString(), "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/batch-env.txt", StandardCharsets.UTF_8);
        /*String url = "https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/917205874770.db";
        AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
        String encryptBase64 = aes.encryptBase64(url);
        System.out.println("加密后：" + encryptBase64);
        System.out.println("解密后：" + aes.decryptStr(encryptBase64));*/
    }

    public static void getPublicKey() {
        String username = "33665761028";
        String envUrl = "https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + username + ".db";
        String envPath = "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/" + username + ".db";
        long l = HttpUtil.downloadFile(envUrl, FileUtil.file(envPath), 60 * 1000);
        AxolotlManager axolotlManager = new AxolotlManager(envPath, username);
        byte[] envBuffer;
        try {
            envBuffer = axolotlManager.GetBytesSetting("env");
            DeviceEnv.AndroidEnv.Builder builder = DeviceEnv.AndroidEnv.parseFrom(envBuffer).toBuilder();
            DeviceEnv.AppVersion.Builder useragentBuilder = builder.getUserAgentBuilder().getAppVersionBuilder();
            useragentBuilder.setPrimary(2);
            useragentBuilder.setSecondary(21);
            useragentBuilder.setTertiary(9);
            useragentBuilder.setQuaternary(11);
            log.info("账号：{}", username);
            log.info("私钥：{}", Base64.encode(builder.getClientStaticKeyPair().getStrPrivateKey().toByteArray()));
            log.info("公钥：{}", Base64.encode(builder.getClientStaticKeyPair().getStrPubKey().toByteArray()));
        } catch (Exception e) {

        }
    }

    public static void channelEnvConvert() {
        boolean cosConfig = CosUploadUtil.cosUpload.createCosConfig("http://127.0.0.1:84/api/cos/getTempSecretKey");
        if (!cosConfig) {
            return;
        }
        LibLoader.loadLib("libNoiseJni.dylib");
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setType(0);
        proxyInfo.setProxyHost("8.210.42.7");
        proxyInfo.setProxyPort(59394);
        StatusResult checkWhatsappVersion = WhatsAppUtils.checkWhatsappVersion(proxyInfo);
        if (Constant.FAIL.equals(checkWhatsappVersion.getStatus())) {
            return;
        }

        String username = "6282365432509";
        String country = "US";
        String publicKey = "hOJzlIpqrA5+uYe/74qAEF7GxohcIbDbY4BBr3wotU0=";
        String privateKey = "sAsZ4S3ZLse2+1VNdMpmorg3UZnvhTA8o8qmQ8IEtks=";
        String dataDir = System.getProperty("user.dir") + "/out/register/";
        boolean b = DeviceUtil.loadDeviceFile("/Users/sunnoc/IdeaProjects/whatsapp-android/src/main/resources/envJson.txt");
        if (!b) {
            return;
        }
        File mkdir = FileUtil.mkdir(dataDir);
        String filePath = new File(dataDir, username + ".db").getAbsolutePath();
        //初始化数据表
        AxolotlManager axolotlManager = new AxolotlManager(filePath, username);
        //创建 env数据
        DeviceEnv.AndroidEnv.Builder envBuild = DeviceEnv.AndroidEnv.newBuilder();
        envBuild.setChatDnsDomain("fb");
        envBuild.setFullphone(username);
        DeviceEnv.UserAgent.Builder useragentBuild = DeviceEnv.UserAgent.newBuilder();
        useragentBuild.setPlatform(DeviceEnv.Platform.ANDROID);
        useragentBuild.setReleaseChannel(DeviceEnv.ReleaseChannel.RELEASE);
        String s = DeviceUtil.randomGetOneDevice();
        JSONObject envJsonObject = JSONObject.parseObject(s);
        useragentBuild.setOsVersion(envJsonObject.getString("release"));
        useragentBuild.setManufacturer(envJsonObject.getString("manufacturer"));
        useragentBuild.setDevice(envJsonObject.getString("model"));
        useragentBuild.setOsBuildNumber(envJsonObject.getString("display"));
        envBuild.setUserAgent(useragentBuild);
        DeviceEnv.AppVersion.Builder appVersionOrBuilder = envBuild.getUserAgentBuilder().getAppVersionBuilder();
        appVersionOrBuilder.setPrimary(2);
        appVersionOrBuilder.setSecondary(21);
        appVersionOrBuilder.setTertiary(9);
        appVersionOrBuilder.setQuaternary(11);
        //Country
        String countryInfo = NoiseJni.getCountryInfo2(country);
        JSONObject jsonObject = JSONObject.parseObject(countryInfo);
        DeviceEnv.UserAgent.Builder userAgentBuilder = envBuild.getUserAgentBuilder();
        userAgentBuilder.setMcc(jsonObject.getString("mcc"));
        userAgentBuilder.setMnc(jsonObject.getString("mnc"));
        userAgentBuilder.setLocaleLanguageIso6391(jsonObject.getString("iso639"));
        userAgentBuilder.setLocaleCountryIso31661Alpha2(jsonObject.getString("iso3166"));
        envBuild.setPushname("");
        envBuild.setEdgeRoutingInfo(ByteString.copyFrom(Base64.decode("CAIICA")));
        envBuild.getUserAgentBuilder().setPhoneId(UUID.randomUUID().toString());
        String id = UUID.randomUUID().toString();
        envBuild.setFdid(id);
        String exPid = id.substring(0, 20);
        try {
            envBuild.setExpid(ByteString.copyFrom(exPid, "UTF-8"));
        } catch (Exception e) {
            return;
        }
        Env.DeviceEnv.KeyPair.Builder keyBuild = envBuild.getClientStaticKeyPairBuilder();
        keyBuild.setStrPrivateKey(ByteString.copyFrom(Base64.decode(privateKey)));
        keyBuild.setStrPubKey(ByteString.copyFrom(Base64.decode(publicKey)));
        axolotlManager.SetBytesSetting("env", envBuild.build().toByteArray());
        axolotlManager.Close();
        try {
            File envFile = new File(filePath);
            boolean success = CosUploadUtil.cosUpload.uploadCosEnvFile("env/" + username + ".db", envFile);
            if (success) {
                String url = "https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + username + ".db";
                AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
                String envEncryptKey = aes.encryptBase64(url);
                log.info("username：{}", username);
                log.info("envEncryptKey：{}", envEncryptKey);
            }
        } catch (Exception e) {
        }

    }

    public static void decrypt() {
        String str = "OoUPZXppAziqZSi+RS2XweorzdaWkD9/SzHQijZ+nlIWrlzA7YpExMBzpWrZ38jmLo67qLquHtUh1BVY0SlRVhK3EHFsG5YUcq1gOM6Tq7jvpRmsaXuZprI3+4tuw4+h";
        AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
        System.out.println("解密后：" + aes.decryptStr(str));
    }

    public static void handleExceptionEnv() {
        String key = "11111";
        String content = FileUtil.readString("/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/wa-business-country.txt", StandardCharsets.UTF_8);
        String[] split = StrUtil.split(content, System.lineSeparator());
        StringBuilder updateBuilder = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        StandardThreadExecutor loginInitThreadExecutor = ThreadPoolConfig.startThreadPool("login-thread-", 10, 10);
        CountDownLatch countDownLatch = split.length > 0 ? new CountDownLatch(split.length) : null;

        for (String s : split) {
            loginInitThreadExecutor.execute(() -> {
                try {
                    System.out.println(s);
                    String[] split1 = StrUtil.split(s, "----");
                    String envEnc = split1[1];
                    AES aes = SecureUtil.aes(Constant.ENV_ENCRYPT_KEY.getBytes());
                    String envUrl = aes.decryptStr(envEnc);
                    String name = FileUtil.getName(envUrl);
                    if (!CosUploadUtil.cosUpload.doesObjectExist("env/" + name)) {
                        if (StrUtil.indexOf(name, ".db", 0, false) != -1) {
                            //去掉.db查询
                            String tempName = StrUtil.replace(name, ".db", "");
                            boolean b = CosUploadUtil.cosUpload.doesObjectExist("env/" + tempName);
                            if (!b) {
                                try {
                                    KeyLockUtil.lock(key);
                                    stringBuilder.append(name).append(System.lineSeparator());
                                } catch (Exception e) {
                                } finally {
                                    KeyLockUtil.unlock(key);
                                }
                            } else {
                                String encryptBase64 = aes.encryptBase64("https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + tempName);
                                try {
                                    KeyLockUtil.lock(key);
                                    updateBuilder.append(split1[0]).append("----").append(encryptBase64).append("----").append(split1[2]).append(System.lineSeparator());
                                } catch (Exception e) {
                                } finally {
                                    KeyLockUtil.unlock(key);
                                }
                            }
                        } else {
                            String tempName = name + ".db";
                            boolean b = CosUploadUtil.cosUpload.doesObjectExist("env/" + tempName);
                            if (!b) {
                                try {
                                    KeyLockUtil.lock(key);
                                    stringBuilder.append(name).append(System.lineSeparator());
                                } catch (Exception e) {
                                } finally {
                                    KeyLockUtil.unlock(key);
                                }
                            } else {
                                String encryptBase64 = aes.encryptBase64("https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + tempName);
                                try {
                                    KeyLockUtil.lock(key);
                                    updateBuilder.append(split1[0]).append("----").append(encryptBase64).append("----").append(split1[2]).append(System.lineSeparator());
                                } catch (Exception e) {
                                } finally {
                                    KeyLockUtil.unlock(key);
                                }
                            }

                        }
                    } else {
                        String encryptBase64 = aes.encryptBase64("https://nanshan-whatsapp-1257892306.cos.ap-singapore.myqcloud.com/env/" + name);
                        try {
                            KeyLockUtil.lock(key);
                            updateBuilder.append(split1[0]).append("----").append(encryptBase64).append("----").append(split1[2]).append(System.lineSeparator());
                        } catch (Exception e) {
                        } finally {
                            KeyLockUtil.unlock(key);
                        }
                    }
                } catch (Exception e) {
                    log.error("异常", e);
                } finally {
                    countDownLatch.countDown();
                }

            });
        }
        if (split.length > 0) {
            try {
                countDownLatch.await();
            } catch (Exception ignored) {
            }
        }
        ThreadPoolConfig.stopThreadPool(loginInitThreadExecutor);
        System.out.println("不存在环境：" + stringBuilder);
        FileUtil.writeString(stringBuilder.toString(), "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/not_exist.txt", CharsetUtil.UTF_8);
        System.out.println("需要更新环境：" + System.lineSeparator() + updateBuilder);
        FileUtil.writeString(updateBuilder.toString(), "/Users/sunnoc/IdeaProjects/whatsapp-android/src/test/java/com/whatsapp/android/new.txt", CharsetUtil.UTF_8);
    }
}
