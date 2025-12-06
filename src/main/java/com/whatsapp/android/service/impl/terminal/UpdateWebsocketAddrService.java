package com.whatsapp.android.service.impl.terminal;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;

import com.whatsapp.android.annotation.ApiType;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.terminal.UpdateTerminalPack;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.ws.WebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 更新终端地址
 *
 * @author sunnoc
 * @date 2020-10-19 12:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ApiType(TypeConstant.TaskType.UPDATE_WEBSOCKET_ADDR)
public class UpdateWebsocketAddrService implements ApiStrategy {
    private final WebSocketClient webSocketClient;

    @Override
    public Result execute(String type, JSONObject dataObject, String taskId, User user) {
        UpdateTerminalPack updateTerminalPack = dataObject.getObject("data", UpdateTerminalPack.class);
        if (!updateTerminalPack.getKey().equals(Constant.UPDATE_TERMINAL_KEY)) {
            log.warn("更新密匙错误：{}", updateTerminalPack.getKey());
            return Result.taskFail(type, taskId, null, StatusResult.fail("更新密匙错误"));
        }
        if (StringUtils.isEmpty(updateTerminalPack.getWs())) {
            log.warn("更新密匙错误：{}", updateTerminalPack.getKey());
            return Result.taskFail(type, taskId, null, StatusResult.fail("更新地址不能为空"));
        }
        if (webSocketClient.updateWebsocketAddr(updateTerminalPack.getWs())) {
            ThreadUtil.execute(() -> {
                ThreadUtil.sleep(3000);
                WebSocketClient.getWebSocketChatClient().close();
            });
            return Result.taskSuccess(type, taskId, null, StatusResult.ok("成功"));
        } else {
            return Result.taskFail(type, taskId, null, StatusResult.fail("更新地址不能为空"));
        }
    }
}
