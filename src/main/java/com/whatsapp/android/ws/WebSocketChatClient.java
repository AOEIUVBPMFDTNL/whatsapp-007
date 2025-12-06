package com.whatsapp.android.ws;

import com.whatsapp.android.terminal.entity.Terminal;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardThreadExecutor;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author sunnoc
 * @date 2019-10-11 17:25
 */

@Slf4j
public class WebSocketChatClient extends WebSocketClient {
    /**
     * 是否重连中
     */
    public static boolean reconnect;

    /**
     * 是否在线
     */
    public static boolean online;
    /**
     * 是否已经初始化
     */
    public static boolean initialization;
    /**
     * 终端信息
     */
    private static Terminal terminal;

    private StandardThreadExecutor threadPoolExecutor;

    private WebsocketMsg websocketMsg;

    public void setWebsocketMsg(WebsocketMsg websocketMsg) {
        this.websocketMsg = websocketMsg;
    }

    public void setThreadPoolExecutor(StandardThreadExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public static Terminal getTerminal() {
        return terminal;
    }

    public static void setTerminal(Terminal terminal) {
        WebSocketChatClient.terminal = terminal;
    }

    public WebSocketChatClient(URI serverUri) {
        super(serverUri);
    }


    public Boolean start() {
        try {
            super.addHeader("Clientname", terminal.getName());
            online = super.connectBlocking(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            online = false;
        }
        if (online) {
            log.info("终端连接成功[{}]", super.getURI().toString());
            return true;
        }
        return false;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        threadPoolExecutor.execute(() -> websocketMsg.parse(message));
    }

    @Override
    public void onMessage(ByteBuffer bytes) {

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        online = false;
        log.info("终端断开连接,Reason:[{}],code[{}],remote[{}]", reason, code, remote);
    }

    @Override
    public void onError(Exception ex) {
    }
}


