package com.whatsapp.android.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.config.ApiContext;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.service.ApiStrategy;
import com.whatsapp.android.util.DevUtil;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sunnoc
 * @date 2021-03-16 20:25
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {
    private final ApiContext apiContext;

    @RequestMapping(method = RequestMethod.POST, value = {"task", "task/{type}"}, consumes = "application/json")
    public Result task(@PathVariable(name = "type", required = false) String type, @RequestBody JSONObject jsonObject) {
        JSONObject dataObject = jsonObject.getJSONObject("data");
        if (StringUtils.isEmpty(type)) {
            type = dataObject.getString("type");
        }
        String username = dataObject.getString("username");
        String taskId = dataObject.getString("taskId");
        String key = dataObject.getString("key");
        if (!type.equals(TypeConstant.TaskType.CHECK_PHONE_BLOCKED) && !type.equals(TypeConstant.TaskType.CHECK_PHONE_EXIST) && !type.equals(TypeConstant.TaskType.SUBMIT_REGISTER) && !type.equals(TypeConstant.TaskType.SEND_SMS)) {
            if (!Constant.KEY.equals(key) && !"debug".equals(key)) {
                return Result.taskFail(type, taskId, username, StatusResult.fail("没有访问权限"));
            }
        }
        log.info("{} 接收到任务：{}", username, jsonObject.toJSONString());
        User user = null;
        if (!type.equals(TypeConstant.TaskType.LOGIN) && !type.equals(TypeConstant.TaskType.UPDATE_TERMINAL) &&
                !type.equals(TypeConstant.TaskType.UPDATE_WEBSOCKET_ADDR) && !type.equals(TypeConstant.TaskType.CHECK_PHONE_EXIST) &&
                !type.equals(TypeConstant.TaskType.SUBMIT_REGISTER) && !type.equals(TypeConstant.TaskType.SEND_SMS) && !type.equals(TypeConstant.TaskType.CHECK_PHONE_BLOCKED)) {
            ConcurrentMap<String, User> record = UserRecord.getRecord();
            user = record.get(username);
            if (user == null) {
                //用户不在线
                return Result.taskFail(type, taskId, username, StatusResult.fail("用户不在线"));
            }
        }
        //拿到实例调用方法
        Result execute = null;
        try {
            ApiStrategy apiStrategy = apiContext.getStrategyInstance(type);
            execute = apiStrategy.execute(type, dataObject, taskId, user);
        } catch (IllegalArgumentException e) {
            execute = Result.taskFail(type, taskId, username, StatusResult.fail("未找到此类型API"));
        } catch (Exception e) {
            log.error("用户：{}，接口调用异常", username, e);
            execute = Result.taskFail(type, taskId, username, StatusResult.fail("接口调用异常"));
        } finally {
            if (execute != null) {
                log.info("用户：{}，任务id：{}，回复内容：{}", username, taskId, execute.toJson());
            }
        }
        return execute;
    }

    @RequestMapping(method = RequestMethod.POST, value = "restart", consumes = "application/json")
    public StatusResult restart(@RequestBody JSONObject jsonObject) {
        JSONObject dataObject = jsonObject.getJSONObject("data");
        String key = dataObject.getString("key");
        if (!Constant.KEY.equals(key) && !"debug".equals(key)) {
            return StatusResult.fail("通信密匙错误");
        }
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(2000);
            log.info("执行程序重启命令");
            System.exit(1);
        });
        return StatusResult.ok();
    }

    @GetMapping(value = "getSystemStatusInfo")
    public DevUtil.SystemStatusInfo getSystemStatusInfo() {
        return DevUtil.getSystemStatusInfo();
    }

    /**
     * 主要是用来删除无用的jar包
     */
    @RequestMapping(method = RequestMethod.POST, value = "deleteFile", consumes = "application/json")
    public JSONObject deleteFile(@RequestBody JSONObject jsonObject) {
        JSONObject result = new JSONObject();
        String file = jsonObject.getString("file");
        boolean exist = FileUtil.exist(file);
        if (!exist) {
            result.put("code", 200);
            result.put("msg", "文件不存在");
            return result;
        }
        boolean del = FileUtil.del(file);
        if (!del) {
            result.put("code", 201);
            result.put("msg", "删除文件失败");
            return result;
        }
        result.put("code", 200);
        result.put("msg", "删除文件成功");
        return result;
    }

    @GetMapping("refreshRegisterVersionInfo")
    public String refreshRegisterVersionInfo() {
        if (WhatsAppUtils.refreshRegisterVersionInfo()) {
            return Constant.OK;
        }
        return Constant.FAIL;
    }

    @GetMapping("chatRecord")
    public void getChatRecord(@RequestParam(value = "username") String username, @RequestParam("msgId") String msgId, HttpServletResponse response) {
        String msgPath = System.getProperty("user.dir") + "/out/groupMsg/" + username + "_" + msgId;
        File file = new File(msgPath);
        if (!file.exists()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel fileChannel = fis.getChannel();
             WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream())) {
            fileChannel.transferTo(0, file.length(), outputChannel);
        } catch (Exception e) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }
}

