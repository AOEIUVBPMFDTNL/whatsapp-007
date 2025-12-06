package com.whatsapp.android.ws;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.config.ApiContext;
import com.whatsapp.android.constant.ServiceConstant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.terminal.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentMap;


/**
 * @author sunnoc
 * @date 2020-07-29 10:14
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WebsocketMsg {
    private final ApiContext apiContext;
    private final ServiceConstant serviceConstant;
    private final TerminalService terminalService;

    public void parse(String message) {
        if (serviceConstant.isGzip()) {
            message = ZipUtil.unGzip(Base64.decode(message), CharsetUtil.UTF_8);
        }
        log.info("收到消息：[{}]", message);
        JSONObject jsonObject = JSONObject.parseObject(message);
        String taskType = jsonObject.getString("type");
        if (TypeConstant.TASK.equals(taskType)) {
            JSONObject dataObject = jsonObject.getJSONObject("data");
            String type = dataObject.getString("type");
            String username = dataObject.getString("username");
            String taskId = dataObject.getString("taskId");
            WebSocketClient.sendMsg(username, Result.taskNotify(type, taskId, username).toJson());
            User user = null;
            if (!type.equals(TypeConstant.TaskType.LOGIN) && !type.equals(TypeConstant.TaskType.UPDATE_TERMINAL) &&
                    !type.equals(TypeConstant.TaskType.UPDATE_WEBSOCKET_ADDR) && !type.equals(TypeConstant.TaskType.CHECK_PHONE_EXIST) &&
                    !type.equals(TypeConstant.TaskType.SUBMIT_REGISTER) && !type.equals(TypeConstant.TaskType.CHECK_PHONE_BLOCKED)) {
                ConcurrentMap<String, User> record = UserRecord.getRecord();
                user = record.get(username);
                if (user == null) {
                    //用户不在线
                    WebSocketClient.sendMsg(username, Result.taskFail(type, taskId, username, StatusResult.fail("用户不在线")).toJson());
                    return;
                }
            }
            //拿到实例调用方法
            try {
                ApiStrategy apiStrategy = apiContext.getStrategyInstance(type);
                WebSocketClient.sendMsg(username, apiStrategy.execute(type, dataObject, taskId, user).toJson());
            } catch (IllegalArgumentException e) {
                WebSocketClient.sendMsg(username, Result.taskFail(type, taskId, username, StatusResult.fail("未找到此类型API")).toJson());
            } catch (Exception e) {
                log.error("用户：{}，接口调用异常", username, e);
                WebSocketClient.sendMsg(username, Result.taskFail(type, taskId, username, StatusResult.fail("接口调用异常")).toJson());
            }
        } else if (TypeConstant.REFRESH_IP_WHITE_LIST.equals(taskType)) {
            boolean ipWhite = terminalService.getIpWhite();
            if (ipWhite) {
                log.info("刷新ip白名单成功");
            } else {
                log.error("刷新ip白名单失败");
            }
        }
    }
}
