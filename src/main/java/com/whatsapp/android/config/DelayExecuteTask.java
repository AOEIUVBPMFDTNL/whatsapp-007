package com.whatsapp.android.config;

import axolotl.AxolotlManager;
import cn.hutool.core.io.FileUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.Register;
import com.whatsapp.android.entity.DelayTask;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


/**
 * 延时执行任务
 *
 * @author sunnoc
 * @date 2021-04-20 14:17
 */
@Slf4j
public class DelayExecuteTask {
    //设置每个格子是 10ms, 总共 512 个格子
    public static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);

    public static Timeout addDelayTask(DelayTask delayTask, long delay) {
        return hashedWheelTimer.newTimeout(timeout -> {
            String type = delayTask.getType();
            String username = delayTask.getUsername();
            ThreadPoolConfig.threadPoolExecutor.execute(() -> {
                if (type == null) {
                    String registerKey = delayTask.getRegisterKey();
                    removeRegisterEnv(registerKey);
                } else if (type.equals("sendReadTag")) {
                    String iqId = delayTask.getIqId();
                    String to = delayTask.getTo();
                    String participant = delayTask.getParticipant();
                    User user = UserRecord.getRecord().get(username);
                    if (user != null) {
                        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
                        if (gorgeousEngine != null && user.isOnline()) {
                            gorgeousEngine.sendReadTag(iqId, to, participant);
                        }
                    }

                }
            });

        }, delay, TimeUnit.MILLISECONDS);
    }

    public static void addDelayReLogin(String username, User user, long delay, TimeUnit timeUnit) {
        hashedWheelTimer.newTimeout(timeout -> {
            ThreadPoolConfig.loginThreadPool.execute(() -> {
                if (UserRecord.getRecord().containsKey(username)) {
                    user.reLogin();
                }
            });
        }, delay, timeUnit);
    }

    public static void removeRegisterEnv(String registerKey) {
        ConcurrentMap<String, Register> registerRecord = UserRecord.getRegisterRecord();
        Register register = registerRecord.get(registerKey);
        if (register != null) {
            try {
                AxolotlManager axolotlManager = register.getAxolotlManager();
                String registerDataDir = register.getRegisterDataDir();
                registerRecord.remove(registerKey);
                axolotlManager.Close();
                FileUtil.del(registerDataDir);
            } catch (Exception e) {
                log.error("移除环境异常", e);
            }
            log.info("移除：{}，注册环境", register.getUsername());
        }
    }
}
