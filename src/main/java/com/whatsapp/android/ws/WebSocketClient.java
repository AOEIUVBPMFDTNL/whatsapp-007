package com.whatsapp.android.ws;


import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.SendFailMsg;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.run.StartedUpRunner;
import com.whatsapp.android.service.ApiService;
import com.whatsapp.android.terminal.entity.InsTerminalVersion;
import com.whatsapp.android.terminal.entity.MachineCode;
import com.whatsapp.android.terminal.entity.Terminal;
import com.whatsapp.android.terminal.service.DefaultAddrService;
import com.whatsapp.android.terminal.service.MachineCodeService;
import com.whatsapp.android.terminal.service.TerminalService;
import com.whatsapp.android.terminal.service.TerminalVersionService;
import com.whatsapp.android.util.DevUtil;
import com.whatsapp.android.util.InetUtils;
import com.whatsapp.android.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author sunnoc
 * @date 2020-07-28 10:33
 */
@Component
@Slf4j
public class WebSocketClient {
    private static WebSocketChatClient webSocketChatClient;
    @Autowired
    DefaultAddrService defaultAddrService;

    @Autowired
    TerminalService terminalService;

    @Autowired
    MachineCodeService machineCodeService;

    @Autowired
    WebsocketMsg websocketMsg;

    @Autowired
    ApiService fbApiService;

    @Autowired
    TerminalVersionService terminalVersionService;

    @Autowired
    ServiceConstant serviceConstant;
    @Value("${terminal.version}")
    private String terminalVersion;
    /**
     * 是否压缩消息
     */
    private static boolean gzip;
    /**
     * 公网ip
     */
    private String publicIp;
    /**
     * 内网ip
     */
    public static String terminalIp;
    /**
     * 机器码
     */
    private String uuid;
    /**
     * 消息发送失败存储队列
     */
    private static ConcurrentLinkedDeque<SendFailMsg> sendMsgFailQueue = new ConcurrentLinkedDeque<>();

    public void check() {
        if (WebSocketChatClient.reconnect || WebSocketChatClient.online) {
            return;
        }
        WebSocketChatClient.reconnect = true;
        gzip = serviceConstant.isGzip();
        ThreadPoolConfig.threadPoolExecutor.execute(() -> {
            String defaultAddr;
            try {

                if (StringUtils.isEmpty(terminalIp)) {
                    terminalIp = InetUtils.getSelfIP();
                    if (StringUtils.isEmpty(terminalIp)) {
                        WebSocketChatClient.reconnect = false;
                        log.error("获取内网ip失败");
                        return;
                    }
                }
                if (StringUtils.isEmpty(publicIp)) {
                    publicIp = DevUtil.getPublicIp();
                    if (StringUtils.isEmpty(publicIp)) {
                        publicIp = terminalIp;
                    }
                }
                Terminal insTerminalInfo = WebSocketChatClient.getTerminal();
                if (insTerminalInfo == null) {
                    TerminalResult terminalResult = getTerminalResult();
                    boolean requestSuccess = terminalResult.isRequestSuccess();
                    if (!requestSuccess) {
                        WebSocketChatClient.reconnect = false;
                        log.error("获取终端信息失败");
                        return;
                    }
                    Terminal terminalInfo = terminalResult.getTerminalInfo();
                    if (terminalInfo == null) {
                        //终端第一次上线，需要判断终端是否是最新版，不是最新版直接安装最新版终端
                        InsTerminalVersion newestVersion = terminalVersionService.getNewestVersion();
                        String version = newestVersion.getVersion();
                        if (!StringUtils.isEmpty(version)) {
                            if (!version.equals(this.terminalVersion)) {
                                log.info("需要更新终端为版本：{}", version);
                                //更新终端版本到最新
                                if (FileUtil.exist(Constant.JAR_PATH)) {
                                    if (fbApiService.downTerminal(newestVersion.getUpdateKey())) {
                                        ThreadUtil.sleep(1000);
                                        RuntimeUtil.execForStr("nohup java -jar " + Constant.JAR_PATH + Constant.UPDATE_JAR_PACK_NAME + " &");
                                        log.info("更新最新版终端");
                                        return;
                                    }
                                }
                            }
                        }
                        defaultAddr = defaultAddrService.getDefaultAddr();
                        if (StringUtils.isEmpty(defaultAddr)) {
                            WebSocketChatClient.reconnect = false;
                            return;
                        }
                        MachineCode machineCode = machineCodeService.saveMachineCode(uuid);
                        if (machineCode == null || machineCode.getId() <= 0) {
                            WebSocketChatClient.reconnect = false;
                            return;
                        }
                        Terminal terminal = new Terminal();
                        terminal.setWs(defaultAddr);
                        terminal.setPublicIp(publicIp);
                        terminal.setIp(terminalIp);
                        terminal.setName(Constant.TERMINAL_PREFIX + machineCode.getId());
                        terminal.setVersion(terminalVersion);
                        terminal.setUuid(uuid);
                        terminal.setPlatformType(Constant.PLATFORM_TYPE);
                        terminal.setCountryCode(StartedUpRunner.countryCode);
                        terminalService.saveTerminalInfo(terminal);
                        WebSocketChatClient.setTerminal(terminal);
                    } else {
                        WebSocketChatClient.setTerminal(terminalInfo);
                        defaultAddr = terminalInfo.getWs();
                    }
                } else {
                    defaultAddr = insTerminalInfo.getWs();
                }
            } catch (Exception ignored) {
                WebSocketChatClient.reconnect = false;
                return;
            }
            try {
                int environment = SpringUtils.getEnvironment();
                if (StringUtils.hasLength(StartedUpRunner.countryCode) && !"sg".equals(StartedUpRunner.countryCode)) {
                    //处理websocket连接
                    if ("ws://47.241.71.202:9999".equals(defaultAddr) ||
                            "ws://172.31.0.95:9998".equals(defaultAddr) ||
                            (environment != 3 && "ws://127.0.0.1:9998".equals(defaultAddr))) {
                        String domainName = serviceConstant.getDomainName();
                        defaultAddr = StrUtil.replace(domainName, "http://", "wss://") + "/ws";
                    }
                }
                URI uri = new URI(defaultAddr);
                //关闭未销毁的连接
                if (webSocketChatClient != null) {
                    boolean open = webSocketChatClient.isOpen();
                    if (open) {
                        try {
                            webSocketChatClient.close();
                        } catch (Exception ignored) {
                        }
                        ThreadUtil.sleep(3000);
                    }
                }
                webSocketChatClient = new WebSocketChatClient(uri);
                webSocketChatClient.setThreadPoolExecutor(ThreadPoolConfig.threadPoolExecutor);
                webSocketChatClient.setWebsocketMsg(websocketMsg);
                webSocketChatClient.setConnectionLostTimeout(30);
                Boolean start = webSocketChatClient.start();
                if (start) {
                    if (!WebSocketChatClient.initialization) {
                        log.info("当前终端版本为：{}", terminalVersion);
                        //如果是首次运行，则需要把之前登录的账号重新上线
                        fbApiService.initializeUserOnline();
                        WebSocketChatClient.initialization = true;
                    }
                    //发送初始化
                    Terminal terminal = WebSocketChatClient.getTerminal();
                    fbApiService.initialize(terminal.getUuid(), terminal.getName(), Constant.KEY, TypeConstant.INITIALIZE, true);
                    updateDevInfo();
                    //发送终端不在线导致发送失败的消息
                    if (!sendMsgFailQueue.isEmpty()) {
                        //延时2秒后再推送
                        ThreadUtil.sleep(2000);
                        while (!sendMsgFailQueue.isEmpty()) {
                            SendFailMsg sendFailMsg = sendMsgFailQueue.poll();
                            boolean sendMsg = sendRetryMsg(sendFailMsg);
                            if (!sendMsg) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("websocket连接异常", e);
            } finally {
                WebSocketChatClient.reconnect = false;
            }
        });
    }

    public void terminalReport() {
        //判断websocket是否在线
        if (!WebSocketChatClient.online) {
            return;
        }
        ThreadPoolConfig.threadPoolExecutor.execute(() -> {
            fbApiService.initialize(WebSocketChatClient.getTerminal().getUuid(), WebSocketChatClient.getTerminal().getName(), Constant.KEY, TypeConstant.PING, false);
            updateDevInfo();
        });
    }

    public static boolean sendRetryMsg(SendFailMsg sendFailMsg) {
        if (WebSocketChatClient.online) {
            log.info("用户：{}，重试发送，回复内容：{}", sendFailMsg.getUsername(), sendFailMsg.getMessage());
            if (gzip) {
                send(Base64.encode(ZipUtil.gzip(sendFailMsg.getMessage(), CharsetUtil.UTF_8)));
            } else {
                send(sendFailMsg.getMessage());
            }
            return true;
        } else {
            sendMsgFailQueue.addFirst(new SendFailMsg(sendFailMsg.getUsername(), sendFailMsg.getMessage()));
            log.info("用户：{}，重试发送失败，等待再次发送，回复内容：{}", sendFailMsg.getUsername(), sendFailMsg.getMessage());
            return false;
        }
    }

    public static void sendMsg(String username, String msg) {
        if (WebSocketChatClient.online) {
            log.info("用户：{}，回复内容：{}", username, msg);
            if (gzip) {
                send(Base64.encode(ZipUtil.gzip(msg, CharsetUtil.UTF_8)));
            } else {
                send(msg);
            }
        } else {
            sendMsgFailQueue.add(new SendFailMsg(username, msg));
            log.info("长连接不在线，用户：{}，回复内容：{}", username, msg);
        }
    }

    /**
     * 初始化，定时上报
     */
    public static void sendMsg(String msg, boolean print) {
        if (WebSocketChatClient.online) {
            if (print) {
                log.info("回复内容：{}", msg);
            }
            if (gzip) {
                send(Base64.encode(ZipUtil.gzip(msg, CharsetUtil.UTF_8)));
            } else {
                send(msg);
            }
        }
    }

    private static void send(String text) {
        try {
            webSocketChatClient.send(text);
        } catch (Exception e) {
            log.error("发送websocket异常", e);
        }
    }

    public static WebSocketChatClient getWebSocketChatClient() {
        webSocketChatClient.close();
        return webSocketChatClient;
    }


    private void updateDevInfo() {
        try {
            DevUtil.SystemStatusInfo systemStatusInfo = DevUtil.getSystemStatusInfo();
            Terminal terminal = new Terminal();
            terminal.setCpu(systemStatusInfo.getCpuUsePercent());
            terminal.setMemory(systemStatusInfo.getMemoryUsePercent());
            terminal.setSystemInfo(systemStatusInfo.getSystemInfo());
            terminal.setVersion(terminalVersion);
            terminal.setNum(UserRecord.getRecord().size());
            terminal.setUuid(WebSocketChatClient.getTerminal().getUuid());
            terminal.setPlatformType(Constant.PLATFORM_TYPE);
            terminal.setIp(terminalIp);
            terminal.setPublicIp(publicIp);
            terminal.setCountryCode(StartedUpRunner.countryCode);
            terminalService.updateTerminalInfo(terminal);
        } catch (Exception e) {
            log.error("更新终端信息错误", e);
        }
    }

    public boolean updateWebsocketAddr(String websocketAddr) {
        Terminal terminal1 = WebSocketChatClient.getTerminal();
        Terminal terminal = new Terminal();
        terminal.setUuid(terminal1.getUuid());
        terminal.setWs(websocketAddr);
        terminal.setPlatformType(Constant.PLATFORM_TYPE);
        if (terminalService.updateTerminalInfo(terminal)) {
            terminal1.setWs(websocketAddr);
            return true;
        }
        return false;
    }

    /**
     * 获取终端信息
     */
    private TerminalResult getTerminalResult() {
        String machineCodePath = System.getProperty("user.dir") + "/machineCode/code.txt";
        boolean exist = FileUtil.exist(machineCodePath);
        boolean localExistMachineCode = false;
        uuid = null;
        if (exist) {
            uuid = FileUtil.readString(machineCodePath, StandardCharsets.UTF_8);
        }
        if (StringUtils.isEmpty(uuid)) {
            uuid = DevUtil.getMachineCode();
        } else {
            localExistMachineCode = true;
        }
        String body = terminalService.getTerminalInfo(uuid);
        if (StringUtils.isEmpty(body)) {
            return new TerminalResult(false, null);
        }
        JSONObject jsonObject = JSONObject.parseObject(body);
        if (jsonObject.getIntValue("code") != 200) {
            return new TerminalResult(false, null);
        }
        Terminal terminalInfo = jsonObject.getObject("data", Terminal.class);
        if (terminalInfo == null) {
            //说明是新机器
            if (!localExistMachineCode) {
                uuid = IdUtil.simpleUUID();
                FileUtil.writeString(uuid, machineCodePath, StandardCharsets.UTF_8);
            }
            return new TerminalResult(true, null);
        } else {
            //开发版本或者ip相同则终端信息符合
            if (StartedUpRunner.environment == 3 || terminalIp.equals(terminalInfo.getIp())) {
                return new TerminalResult(true, terminalInfo);
            } else {
                if (!localExistMachineCode) {
                    uuid = IdUtil.simpleUUID();
                    FileUtil.writeString(uuid, machineCodePath, StandardCharsets.UTF_8);
                }
                return new TerminalResult(true, null);
            }
        }
    }

    public static class TerminalResult {
        /**
         * 请求是否成功
         */
        private boolean requestSuccess;
        /**
         * 终端信息
         */
        private Terminal terminalInfo;

        public TerminalResult() {
        }

        public TerminalResult(boolean requestSuccess, Terminal terminalInfo) {
            this.requestSuccess = requestSuccess;
            this.terminalInfo = terminalInfo;
        }

        public boolean isRequestSuccess() {
            return requestSuccess;
        }

        public void setRequestSuccess(boolean requestSuccess) {
            this.requestSuccess = requestSuccess;
        }

        public Terminal getTerminalInfo() {
            return terminalInfo;
        }

        public void setTerminalInfo(Terminal terminalInfo) {
            this.terminalInfo = terminalInfo;
        }
    }
}
