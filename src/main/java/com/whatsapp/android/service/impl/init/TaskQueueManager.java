package com.whatsapp.android.service.impl.init;

import axolotl.AxolotlManager;
import com.whatsapp.android.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class TaskQueueManager {
    /**
     * ios 初始化任务
     */
    public static final List<String> IOS_INIT_TASK = Collections.unmodifiableList(
            Arrays.asList("sendPresenceUnavailable", "sendXmlnsW", "sendGetGroups", "sendXmlnsWB", "getInitPicture", "setIntStatus", "sendGet2Fa", "getStatusPrivacy", "getPrivacy", "getDisappearingMode", "sendGetNotice", "getAccept", "sendGetGroups", "getBlockList", "removeAllCompanionDevice", "sendCleanGroups", "sendCleanAccountSync", "initDigest", "accountIOSInitSync", "getContactPicture", "sendPresenceName", "sendGetEmail", "sendPresenceAvailable", "sendNewsletterAddons", "sendGetGroupParticipating")
    );

    /**
     * android 个人版 2.25.26.74 初始化
     */
    public static final List<String> ANDROID_PERSONAL_INIT_TASK = Collections.unmodifiableList(
            Arrays.asList("sendXmlnsW", "sendGetNotice", "sendAndroidTrackable20401220One", "sendAndroidTrackable20401220Hundred", "sendAndroidTrackable20601216One", "sendAndroidTrackable20601216Hundred", "sendAndroidTrackable20601217One", "sendAndroidTrackable20601217Hundred", "sendAndroidTrackable20601218One", "sendAndroidTrackable20601218Hundred", "sendAndroidTrackable20900727One", "sendAndroidTrackable20900727Hundred", "sendAndroidTrackable20610203One", "sendAndroidTrackable20610203Hundred", "sendAndroidTrackable20610204One", "sendAndroidTrackable20610204Hundred", "sendAndroidTrackable20230901One", "sendAndroidTrackable20230901Hundred", "sendAndroidTrackable20240216One", "sendAndroidTrackable20240216Hundred", "sendAndroidTrackable20230902One", "sendAndroidTrackable20230902Hundred", "sendAndroidTrackable20240729One", "sendAndroidTrackable20240729Hundred", "sendAndroidTrackable20241016One", "sendAndroidTrackable20241016Hundred", "sendAndroidTrackable20250304One", "sendAndroidTrackable20250304Hundred", "sendAndroidTrackable20250501One", "sendAndroidTrackable20250501Hundred", "sendPasskeyExistResponseQuery", "sendQueryParticipatingGroups", "sendXmlnsWB", "sendNewsletterAddons", "sendNewsletterSubscribed", "getStatusPrivacy", "getInitPicture", "sendGetDisclosureStageById", "sendGetUserDisclosures", "initDigest", "sendAndroidTrackable20610101One", "sendAndroidTrackable20610101Hundred", "sendAndroidTrackable20610210One", "sendAndroidTrackable20610210Hundred", "sendAndroidTrackable20610220One", "sendAndroidTrackable20610220Hundred", "sendGetDynamicRegistrationUpsells", "sendPresenceName", "sendContactsBackupQuery", "getPrivacy", "getDisappearingMode", "sendGetEmail", "getBlockList", "sendQueryBlockingStatus", "sendGetInviteInfo", "sendGetPreRegAddRequests", "sendGetOptOutList", "sendUsyncQuery", "sendSelfContactsQuery")
    );

    /**
     * android 商业版 2.25.26.74 初始化
     */
    public static final List<String> ANDROID_BUSINESS_INIT_TASK = Collections.unmodifiableList(
            Arrays.asList("sendPresenceAvailable", "sendGetWBizBusinessProfile", "initDigest", "getInitPicture", "getPrivacy", "getBlockList", "sendQueryBlockingStatus", "sendGetOptOutList", "sendDeleteAllData", "sendWBiz139", "getInitPictureUrl", "sendGetUserDisclosures", "sendGetDisclosureStageById", "sendThriftIq118", "sendCleanAccountSync", "sendAndroidTrackable20250331One", "sendAndroidTrackable20250331Hundred", "sendThriftIqCatKit", "sendGetDynamicRegistrationUpsells", "sendPasskeyExistResponseQuery", "sendGetUserDisclosures", "sendPresenceName", "sendSetVerifiedName", "sendSetBusinessCategory", "sendContactsBackupQuery", "getDisappearingMode", "sendXmlnsWB", "sendWBiz109", "getStatusPrivacy", "sendUsyncQuery", "sendBusinessProfileMyself", "sendSelfContactsQuery", "sendGetEmail", "sendThriftIq118", "sendSMAXId42Request", "sendGetInviteInfo", "sendGetPreRegAddRequests", "sendAndroidTrackable20601218One", "sendAndroidTrackable20601218Hundred", "sendAndroidTrackable20601227One", "sendAndroidTrackable20601227Hundred", "sendAndroidTrackable20601228One", "sendAndroidTrackable20601228Hundred", "sendQueryParticipatingGroups", "sendCleanGroups")
    );

    public final java.sql.Connection connection;
    public final AxolotlManager axolotlManager;

    public TaskQueueManager(AxolotlManager axolotlManager, Connection connection) {
        this.axolotlManager = axolotlManager;
        this.connection = connection;
        initializeDatabase();
    }

    /**
     * 初始化数据库表
     */
    private void initializeDatabase() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS init_task_queue ( id INTEGER PRIMARY KEY AUTOINCREMENT, method_name TEXT NOT NULL, platform INTEGER NOT NULL, status INTEGER NOT NULL )");
        } catch (Exception e) {
            log.error("初始化任务表异常", e);
        }
    }

    /**
     * 添加初始化任务
     */
    public void addInitTask(boolean ios, boolean businessVersion) {
        deleteAllTasks();
        List<Task> tasks = new ArrayList<>();
        if (ios) {
            for (String method : IOS_INIT_TASK) {
                tasks.add(new Task(method, Platform.IOS));
            }
        } else {
            if (businessVersion) {
                for (String method : ANDROID_BUSINESS_INIT_TASK) {
                    tasks.add(new Task(method, Platform.ANDROID));
                }
            } else {
                for (String method : ANDROID_PERSONAL_INIT_TASK) {
                    tasks.add(new Task(method, Platform.ANDROID));
                }
            }

        }
        addTasks(tasks);
    }

    /**
     * 批量添加任务
     */
    public void addTasks(List<Task> tasks) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("INSERT INTO init_task_queue (method_name, platform, status) VALUES (?, ?, 0)");
            for (Task task : tasks) {
                preparedStatement.setString(1, task.getMethodName());
                preparedStatement.setInt(2, task.getPlatform().getCode());
                // 添加到批处理
                preparedStatement.addBatch();
            }
            // 执行批处理
            preparedStatement.executeBatch();
        } catch (Exception ignored) {
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 获取一个未执行的任务
     */
    public Task getNextTask() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("SELECT id, method_name, platform FROM init_task_queue WHERE status = ? LIMIT 1");
            preparedStatement.setInt(1, 0);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String methodName = resultSet.getString("method_name");
                    int platformCode = resultSet.getInt("platform");
                    Platform platform = Platform.fromCode(platformCode);
                    return new Task(id, methodName, platform);
                }
            }
        } catch (Exception ignored) {
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
        return null;
    }

    /**
     * 标记任务为已执行
     */
    public void markTaskAsExecuted(int taskId) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("UPDATE init_task_queue SET status = 1 WHERE id = ?");
            preparedStatement.setInt(1, taskId);
            preparedStatement.executeUpdate();

        } catch (Exception ignored) {
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 删除所有任务
     */
    public void deleteAllTasks() {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = axolotlManager.GetPreparedStatement("DELETE FROM init_task_queue");
            preparedStatement.executeUpdate();
        } catch (Exception ignored) {
        } finally {
            axolotlManager.closePreparedStatement(preparedStatement);
        }
    }

    /**
     * 内部类：任务
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private int id;
        private String methodName;
        private Platform platform;

        public Task(String methodName, Platform platform) {
            this.methodName = methodName;
            this.platform = platform;
        }
    }
}
