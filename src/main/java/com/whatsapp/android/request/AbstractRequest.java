package com.whatsapp.android.request;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.io.FileUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.response.message.SendMessageResult;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author sunnoc
 * @date 2021-03-09 10:15
 */
@Slf4j
public abstract class AbstractRequest<T> {
    /**
     * 任务id
     */
    public String taskId;
    @Getter
    @Setter
    public User user;

    /**
     * 执行方法名
     *
     * @return 方法名
     */
    public abstract String funcName();

    /**
     * 请求超时时间
     */
    public long timeOut() {
        return 15;
    }

    public String getTaskId() {
        return taskId;
    }

    /**
     * 是否显示接口日志
     */
    public boolean showLogs() {
        return true;
    }

    /**
     * 是否需要删除文件
     */
    public boolean needDeleteFile() {
        return false;
    }

    /**
     * 删除文件
     */
    public String deleteFile() {
        return null;
    }

    /**
     * 请求
     *
     * @return t
     */
    public abstract boolean request();

    /**
     * 是否需要登录
     */
    public boolean requiresLogin() {
        return true;
    }

    /**
     * 是否只请求
     *
     * @return boolean
     */
    public boolean onlyRequest() {
        return false;
    }

    /**
     * 初始化操作
     */
    public void init() {

    }

    public T execute() {
        init();
        ProtocolTreeNode eventContent;
        if (!StringUtils.hasLength(this.taskId)) {
            this.taskId = getTaskId();
            if (!StringUtils.hasLength(taskId)) {
                GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
                if (gorgeousEngine == null) {
                    eventContent = ProtocolTreeNode.fail(Constant.FAIL);
                    return parseResult(eventContent);
                }
                this.taskId = gorgeousEngine.GenerateIqId();
            }
        }
        try {
            user.getTaskNotify().createEvent(taskId);
            log.info("用户：{}，执行方法：{}，任务id：{}", user.getLoginPack().getUsername(), funcName(), taskId);
            if (onlyRequest()) {
                boolean request = request();
                ProtocolTreeNode node;
                user.getTaskNotify().closeEvent(taskId);
                if (request) {
                    node = ProtocolTreeNode.success(Constant.OK);
                } else {
                    node = ProtocolTreeNode.fail(Constant.FAIL);
                }
                if (showLogs()) {
                    log.info("用户：{}，反馈方法：{}，反馈内容：{}", user.getLoginPack().getUsername(), funcName(), node);
                } else {
                    log.info("用户：{}，反馈方法：{}", user.getLoginPack().getUsername(), funcName());
                }
                return parseResult(node);
            }
            boolean request = request();
            if (!request) {
                user.getTaskNotify().closeEvent(taskId);
                eventContent = ProtocolTreeNode.fail(Constant.FAIL);
            } else {
                eventContent = user.getTaskNotify().getEventContent(taskId, timeOut());
                if (eventContent == null || eventContent.isEmpty()) {
                    eventContent = ProtocolTreeNode.fail(Constant.FAIL, "请求超时");
                }
            }
        } catch (Throwable throwable) {
            log.error("用户：{}，执行任务异常", user.getLoginPack().getUsername(), throwable);
            user.getTaskNotify().closeEvent(taskId);
            eventContent = ProtocolTreeNode.fail(Constant.FAIL, "执行异常");
        } finally {
            if (needDeleteFile()) {
                String deleteFile = deleteFile();
                if (StringUtils.hasLength(deleteFile)) {
                    FileUtil.del(deleteFile);
                    FileUtil.del(deleteFile + ".jpg");
                    FileUtil.del(deleteFile + ".opus");
                }
            }
        }
        if (showLogs()) {
            log.info("用户：{}，反馈方法：{}，反馈内容：{}", user.getLoginPack().getUsername(), funcName(), eventContent);
        } else {
            log.info("用户：{}，反馈方法：{}", user.getLoginPack().getUsername(), funcName());
        }
        return parseResult(eventContent);
    }

    public T execute(String taskId) {
        this.taskId = taskId;
        return execute();
    }

    /**
     * 解析结果
     *
     * @param node 内容
     * @return t
     */
    public abstract T parseResult(ProtocolTreeNode node);

    @SneakyThrows
    public <U> U checkResult(ProtocolTreeNode node, Class<U> clazz) {
        if (node == null || node.isEmpty()) {
            StatusResult statusResult = (StatusResult) clazz.newInstance();
            statusResult.setStatus(Constant.FAIL);
            statusResult.setMessage("超时");
            return (U) statusResult;
        } else if (Constant.FAIL.equals(node.GetTag())) {
            String message = node.GetAttributeValue("message");
            StatusResult statusResult = (StatusResult) clazz.newInstance();
            statusResult.setStatus(Constant.FAIL);
            statusResult.setMessage(message);
            return (U) statusResult;
        } else {
            return null;
        }
    }

    /**
     * 解析发送消息返回结果
     *
     * @param node node
     * @return sendMessageResult
     */
    public SendMessageResult parseSendMessageResult(ProtocolTreeNode node) {
        SendMessageResult checkResult = checkResult(node, SendMessageResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        if ("ack".equals(node.GetTag())) {
            String messageId = node.IqId();
            if (StringUtils.hasLength(messageId)) {
                SendMessageResult sendMessageResult;
                String error = node.GetAttributeValue("error");
                if (StringUtils.hasLength(error)) {
                    if ("420".equals(error)) {
                        sendMessageResult = new SendMessageResult(StatusResult.fail("管理员已禁止发言"));
                    } else if ("401".equals(error)) {
                        sendMessageResult = new SendMessageResult(StatusResult.fail("账号不在目标群中"));
                    } else if ("463".equals(error)) {
                        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
                        String timeLockEndTime = gorgeousEngine.getTimeLockEndTime();
                        if (StringUtils.hasLength(timeLockEndTime)) {
                            long remainingLockTime = gorgeousEngine.axolotlManager_.getRemainingLockTime(timeLockEndTime);
                            sendMessageResult = new SendMessageResult(StatusResult.fail("账号被官方限制，锁定时间剩余：" + remainingLockTime + "秒"));
                        } else {
                            sendMessageResult = new SendMessageResult(StatusResult.fail("账号被官方限制，请稍后再试"));
                        }
                    } else {
                        if (Constant.STATUS_CODE.containsKey(error)) {
                            sendMessageResult = new SendMessageResult(StatusResult.fail((String) Constant.STATUS_CODE.get(error)));
                        } else {
                            sendMessageResult = new SendMessageResult(StatusResult.fail("发送失败"));
                        }
                    }
                } else {
                    sendMessageResult = new SendMessageResult(StatusResult.ok());
                }
                sendMessageResult.setMsgId(messageId);
                return sendMessageResult;
            }
        }
        ProtocolTreeNode errorNode = node.getOneChildren("error");
        if (errorNode != null) {
            String desc = errorNode.GetAttributeValue("desc");
            if (StringUtils.hasLength(desc)) {
                if (Constant.STATUS_CODE.containsKey(desc)) {
                    return new SendMessageResult(StatusResult.fail((String) Constant.STATUS_CODE.get(desc)));
                } else {
                    return new SendMessageResult(StatusResult.fail(desc));
                }
            }
        }
        return new SendMessageResult(StatusResult.fail(node.toString()));
    }

    /**
     * 解析基本校验结果
     */
    public StatusResult parseBaseResult(ProtocolTreeNode node) {
        StatusResult checkResult = checkResult(node, StatusResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        String type = node.type();
        if (StringUtils.hasLength(type) && "error".equals(type)) {
            ProtocolTreeNode errorNode = node.getOneChildren("error");
            if (errorNode != null) {
                return StatusResult.fail(errorNode.toString());
            }
            return StatusResult.fail();
        }
        return StatusResult.ok();
    }
}
