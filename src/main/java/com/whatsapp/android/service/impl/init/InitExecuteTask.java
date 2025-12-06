package com.whatsapp.android.service.impl.init;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.config.ThreadPoolConfig;
import com.whatsapp.android.util.KeyLockUtil;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class InitExecuteTask {
    private static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);

    private final GorgeousEngine gorgeousEngine;
    private final TaskQueueManager taskQueueManager;
    private final String username;

    public InitExecuteTask(GorgeousEngine gorgeousEngine) {
        this.gorgeousEngine = gorgeousEngine;
        this.taskQueueManager = gorgeousEngine.axolotlManager_.taskQueueManager;
        this.username = gorgeousEngine.getUsername();
    }

    /**
     * 调度下一个任务
     */
    public void scheduleNextTask() {
        if (ObjectUtil.isNull(gorgeousEngine) || ObjectUtil.isNull(gorgeousEngine.axolotlManager_)) {
            return;
        }
        hashedWheelTimer.newTimeout(timeout -> {
            ThreadPoolConfig.xmppPoolExecutor.execute(this::executeTask);
        }, RandomUtil.randomInt(100, 200), TimeUnit.MILLISECONDS);
    }

    private void executeTask() {
        try {
            if (ObjectUtil.isNull(gorgeousEngine) || ObjectUtil.isNull(gorgeousEngine.axolotlManager_)) {
                return;
            }
            TaskQueueManager.Task task = getNextTask();
            if (task == null) {
                log.info("用户: {}, 初始化任务执行完毕", username);
                taskQueueManager.axolotlManager.initCompleted();
                return;
            }
            // 校验登录平台方法是否存在
            if (!checkMethodNameExist(task)) {
                ThreadPoolConfig.xmppPoolExecutor.execute(() -> {
                    markTaskAsExecuted(task);
                    executeTask();
                });
                return;
            }
            // 执行任务
            String methodName = task.getMethodName();
            TaskMethod.invokeTaskMethod(methodName, gorgeousEngine, success -> {
                if (success) {
                    if (log.isDebugEnabled()) {
                        log.debug("用户: {}, 执行初始化{}成功", username, methodName);
                    }
                    executeNextTask(task);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("用户: {}, 执行初始化{}失败", username, methodName);
                    }
                }
            });
        } catch (Exception e) {
            log.error("用户: {},调度任务异常", username, e);
        }
    }

    /**
     * 校验方法是否存在
     */
    private boolean checkMethodNameExist(TaskQueueManager.Task task) {
        String methodName = task.getMethodName();
        boolean iosLogin = gorgeousEngine.isIosLogin();
        if (iosLogin) {
            return TaskQueueManager.IOS_INIT_TASK.contains(methodName);
        } else {
            if (gorgeousEngine.isBusinessVersion()) {
                return TaskQueueManager.ANDROID_BUSINESS_INIT_TASK.contains(methodName);
            } else {
                return TaskQueueManager.ANDROID_PERSONAL_INIT_TASK.contains(methodName);
            }
        }
    }

    private TaskQueueManager.Task getNextTask() {
        try {
            KeyLockUtil.lock(username);
            return taskQueueManager.getNextTask();
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

    private void executeNextTask(TaskQueueManager.Task task) {
        markTaskAsExecuted(task);
        // 调度下一个任务
        scheduleNextTask();
    }

    private void markTaskAsExecuted(TaskQueueManager.Task task) {
        try {
            KeyLockUtil.lock(username);
            // 标记任务为已执行
            taskQueueManager.markTaskAsExecuted(task.getId());
        } finally {
            KeyLockUtil.unlock(username);
        }
    }

}
