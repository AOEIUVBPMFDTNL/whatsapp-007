package com.whatsapp.android.run;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.imx.apns.common.AppleBag;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.config.ApiContext;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.GcmProxyConstant;
import com.whatsapp.android.constant.RegisterInfoConstant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.terminal.service.TerminalService;
import com.whatsapp.android.util.*;
import com.whatsapp.android.util.cron.CronUtil;
import com.whatsapp.android.util.cron.task.Task;
import com.whatsapp.android.ws.WebSocketClient;
import jni.ProtocolNodeJni;
import jni.Register;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static com.imx.apns.common.Constants.BAG_URL;

/**
 * @author sunnoc
 * @date 2020-08-01 18:44
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartedUpRunner implements ApplicationRunner {

    private final WebSocketClient webSocketClient;
    private final ApiContext apiContext;
    private final TerminalService terminalService;
    private final ServiceConstant serviceConstant;
    private final RegisterInfoConstant registerInfoConstant;
    private final GcmProxyConstant gcmProxyConstant;
    @Value("${server.port}")
    private int serverPort;
    /**
     * 服务器运行版本
     */
    public static int environment = 0;
    /**
     * 是否安装了ffmpeg库
     */
    public static boolean installFfmpeg = false;
    /**
     * 国家代码
     */
    public static String countryCode = null;
    public static File encodeLogs;
    /**
     * 是否启动完毕
     */
    public static boolean run;
    /**
     * 不连接websocket
     */
    public static boolean disConWs;
    /**
     * 服务端口
     */
    public static int tomcatPort;

    @Override
    public void run(ApplicationArguments args) {
        tomcatPort = this.serverPort;
        String machineCodeDir = System.getProperty("user.dir") + "/machineCode";
        if (!FileUtil.exist(machineCodeDir)) {
            FileUtil.mkdir(machineCodeDir);
        }
        String encodeLogsDir = System.getProperty("user.dir") + "/encodeLogs";
        if (!FileUtil.exist(encodeLogsDir)) {
            FileUtil.mkdir(encodeLogsDir);
        }
        String domainPath = machineCodeDir + "/country.txt";
        String content = null;
        try {
            content = FileUtil.readString(domainPath, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
        }
        if (StringUtils.isEmpty(content)) {
            countryCode = "sg";
        } else {
            countryCode = StrUtil.trimToEmpty(content);
            String domainName = serviceConstant.getDomainName();
            serviceConstant.setServerAddr(domainName);
        }
        String os = System.getProperty("os.name");
        delInvalidJar();
        boolean result = DeviceUtil.loadIosDeviceFile();
        if (!result) {
            log.error("加载ios设备信息失败");
        }
        result = DeviceUtil.loadWaCountryInfoFile();
        if (!result) {
            log.error("加载国家信息表失败");
        }
        result = DeviceUtil.loadAndroidDeviceFile();
        if (!result) {
            log.error("加载android设备信息失败");
        }
        if (!PhoneAreaCodeSearchUtil.loadPhoneAreaCodeSearchFile()) {
            log.error("加载手机号区号搜索失败");
        }
        if (!DeviceUtil.loadCountryCodeFile()) {
            log.error("加载手机号区号国家代码失败");
        }
        boolean ipWhite = terminalService.getIpWhite();
        if (ipWhite) {
            log.info("刷新ip白名单成功");
        } else {
            log.error("刷新ip白名单失败");
        }
        if (!DeviceUtil.loadMccMncInfoFile()) {
            log.error("加载MccMnc代码失败");
        }
        //encodeLogs = new File(encodeLogsDir + "/" + DateUtil.now());
        //扫描whatsApp api到map
        apiContext.initAllApi("com.whatsapp.android.service.impl");
        if (os.startsWith("Linux")) {
            installFfmpeg = WhatsAppUtils.checkIsInstallFfmpeg();
            log.info("{} 是否安装了ffmpeg库{}", os, installFfmpeg);
            //centos系统安装完宝塔后，更新openssl与libstdc++.so.6
           /* libstdc++.so.6:
            下载centos7.6可用的http://www.vuln.cn/9154，然后用这个教程更新https://blog.csdn.net/xiehuanhuan1991/article/details/92410800*/
            /*
             * 升级openssl:
             * https://cloud.tencent.com/developer/article/1632995s
             */

            //ubuntu系统
            /*sudo apt install libssl-dev
            sudo apt-get install curl libcurl4-openssl-dev*/
            LibLoader.loadLib("libNoiseJni.so");
        } else if (os.startsWith("Windows")) {
            LibLoader.loadLib("libNoiseJni.dll");
        } else if (os.startsWith("Mac")) {
            installFfmpeg = WhatsAppUtils.checkIsInstallFfmpeg();
            log.info("{} 是否安装了ffmpeg库{}", os, installFfmpeg);
            LibLoader.loadLib("libNoiseJni.dylib");
        }
        environment = SpringUtils.getEnvironment();
        Constant.GCM_INFO = gcmProxyConstant;
        RegisterInfoConstant.RegisterInfo normalVersionRegisterInfo = registerInfoConstant.getNormalVersionRegisterInfo();
        RegisterInfoConstant.RegisterInfo businessVersionRegisterInfo = registerInfoConstant.getBusinessVersionRegisterInfo();
        if (normalVersionRegisterInfo != null) {
            Register.SetNormalVersion(normalVersionRegisterInfo.getVersion());
            Register.SetNormalMd5(normalVersionRegisterInfo.getClassesMd5Base64());
        }
        if (businessVersionRegisterInfo != null) {
            Register.SetBussinessVersion(businessVersionRegisterInfo.getVersion());
            Register.SetBussinessMd5(businessVersionRegisterInfo.getClassesMd5Base64());
        }
        while (true) {
            if (WhatsAppUtils.refreshRegisterVersionInfo()) {
                log.info("刷新注册版本信息成功");
                break;
            }
            log.info("刷新注册版本信息失败");
            ThreadUtil.sleep(3000);
        }
        CustomThreadPool.creatThreadPool(200);
        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start(ThreadPoolConfig.cronTaskThreadPoolExecutor);
        //每12小时，刷新下APNS域名
        new Thread(() -> {
            while (true) {
                try {
                    AppleBag.getBag(BAG_URL);
                } catch (Exception ignored) {
                }
                ThreadUtil.sleep(12 * 60 * 60 * 1000);
            }
        }).start();
        if (!serviceConstant.isDisConWs()) {
            //每3秒执行一次任务
            CronUtil.schedule("0/3 * * * * ?", (Task) webSocketClient::check);
            //每30秒到90秒执行一次终端上报
            new Thread(() -> {
                while (true) {
                    webSocketClient.terminalReport();
                    int delay = RandomUtil.randomInt(30 * 1000, 91 * 1000);
                    ThreadUtil.sleep(delay);
                }
            }).start();

        }
        //清理超过24小时的文件，每3小时校验一次
        CronUtil.schedule("0 0 */3 * * *", (Runnable) () -> {
            log.info("执行清理群消息文件夹");
            String groupMsgDir = System.getProperty("user.dir") + "/out/groupMsg";
            new Thread(() -> cleanGroupMsg(groupMsgDir)).start();
        });
        new Thread(() -> {
            while (true) {
                //随机14分钟到17分钟执行一次
                ThreadUtil.sleep(RandomUtil.randomInt(14 * 60 * 1000, 17 * 60 * 1000));
                log.info("Timed call gcm");
                ConcurrentMap<String, User> record = UserRecord.getRecord();
                ThreadPoolConfig.gcmAndGooglePoolExecutor.execute(() -> {
                    record.forEach((key, user) -> {
                        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
                        if (user.isOnline() && gorgeousEngine != null && !gorgeousEngine.getGcmOnline().get()) {
                            gorgeousEngine.scheduleNotifyService(ThreadPoolConfig.platformNotificationPoolExecutor);
                        }
                    });
                });
            }
        }).start();
        StartedUpRunner.run = true;
        log.info("启动完毕");
    }

    public void cleanGroupMsg(String groupMsgDir) {
        //清理前2天消息文件
        long delTime = System.currentTimeMillis() - 86400 * 2 * 1000;
        try (Stream<Path> paths = Files.walk(Paths.get(groupMsgDir))) {
            paths.filter(x -> {
                        if (!Files.isDirectory(x)) {
                            try {
                                return Files.getLastModifiedTime(x).toMillis() < delTime;
                            } catch (Exception ignored) {
                                return false;
                            }
                        }
                        return false;
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception ignored) {
                        }
                    });

        } catch (Exception e) {
            log.error("清理群消息文件异常", e);
        }
        long cleanChatRecordTime = delTime / 1000;
        //删除聊天记录
        UserRecord.getRecord().forEach((username, user) -> {
            try {
                KeyLockUtil.lock(username);
                user.getGorgeousEngine().axolotlManager_.chatHistoryStore.deleteExpiredMsg(cleanChatRecordTime);
            } catch (Exception ignored) {
            } finally {
                KeyLockUtil.unlock(username);
            }
        });
    }

    /**
     * 删除依赖冲突jar
     */
    private void delInvalidJar() {
        String nettyHandlerPath = "/usr/javawork/whatsapp/lib/netty-handler-4.1.50.Final.jar";
        String reactorCorePath = "/usr/javawork/whatsapp/lib/reactor-core-3.3.6.RELEASE.jar";
        boolean reactorCoreDeleted = deleteFileIfExist(reactorCorePath);
        boolean nettyHandlerDeleted = deleteFileIfExist(nettyHandlerPath);
        if (reactorCoreDeleted || nettyHandlerDeleted) {
            // 标记异常退出，让守护进程重新拉起来
            System.exit(1);
        }
    }

    private boolean deleteFileIfExist(String filePath) {
        if (filePath != null && FileUtil.exist(filePath)) {
            FileUtil.del(filePath);
            return true;
        }
        return false;
    }

    private void testDecode() {
        /*String content = "APgMLgb6/waRcgAAAWYDBfwHZGV2aWNlcwT7BTGEGRMgHhUJ/wUWJwVGEPgB+ASB/AtkZXZpY2VfaGFzaPwKMjppc1FaYThrUPgC+AX8BmRldmljZQz3AAH/BpFyAAABZvwJa2V5LWluZGV4FfgE/A5rZXktaW5kZXgtbGlzdPwCdHP8CjE2MjcwNTQ2MDL8VgoSCJ21+9oCEIrE64cGGAEiAgABEkBfpOVBJ6Q8FH25q9q9n6qA8NfThqpPFn/+EABWUtMtJ9ol5wgS2VPeLoXu6keMjjsmps6V6FdxIl5fWim7TdoA";
        byte[] decode = Base64.decode(content);
        ProtocolTreeNode node = ProtocolNodeJni.Decode(decode);
        System.out.println(node);*/
        String s = FileUtil.readString("/Users/sunnoc/Downloads/2021-07-24 12_40_36", CharsetUtil.UTF_8);
        String[] split = StrUtil.split(s, System.lineSeparator());
        for (int i = 0; i < split.length; i++) {
            byte[] decode = Base64.decode(split[i]);
            if (decode.length > 0) {
                ProtocolTreeNode node = ProtocolNodeJni.Decode(0, "52", decode);
                System.out.println(i);
                System.out.println(node);
            }

        }
    }
}
