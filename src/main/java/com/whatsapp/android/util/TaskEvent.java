package com.whatsapp.android.util;

import ProtocolTree.ProtocolTreeNode;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author sunnoc
 * @date 2019-06-19 14:20
 */
@Slf4j
public class TaskEvent {
    private static ConcurrentMap<CountDownLatch, ProtocolTreeNode> record = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, CountDownLatch> recordObject = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Consumer<ProtocolTreeNode>> consumerObject = new ConcurrentHashMap<>();

    /**
     * 创建事件
     *
     * @return 返回创建的事件任务id
     */
    public static String createEvent() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String taskId = IdUtil.simpleUUID().toUpperCase();
        record.put(countDownLatch, new ProtocolTreeNode(""));
        recordObject.put(taskId, countDownLatch);
        return taskId;
    }

    /**
     * 创建事件
     *
     * @param taskId taskId
     */
    public static void createEvent(String taskId) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        record.put(countDownLatch, new ProtocolTreeNode(""));
        recordObject.put(taskId, countDownLatch);
    }

    /**
     * 添加消费者
     *
     * @param taskId   任务id
     * @param consumer 消费者
     */
    public static void addConsumer(String taskId, Consumer<ProtocolTreeNode> consumer) {
        CountDownLatch countDownLatch = recordObject.get(taskId);
        if (countDownLatch == null) {
            return;
        }
        consumerObject.put(taskId, consumer);
    }

    /**
     * 事件通知
     *
     * @param taskId  任务id
     * @param content 通知内容
     */
    public static void setEventContent(String taskId, ProtocolTreeNode content) {
        CountDownLatch countDownLatch = recordObject.get(taskId);
        if (countDownLatch == null) {
            return;
        }
        record.put(countDownLatch, content);
        Consumer<ProtocolTreeNode> nodeConsumer = consumerObject.get(taskId);
        if (ObjectUtil.isNotNull(nodeConsumer)) {
            nodeConsumer.accept(content);
            consumerObject.remove(taskId);
        }
        countDownLatch.countDown();
    }

    /**
     * 获取事件内容，默认超时20秒
     *
     * @param taskId 事件任务id
     * @return 事件内容
     */
    public static ProtocolTreeNode getEventContent(String taskId) {
        ProtocolTreeNode content;
        CountDownLatch countDownLatch = recordObject.get(taskId);
        if (countDownLatch == null) {
            return null;
        }
        try {
            countDownLatch.await(20, TimeUnit.SECONDS);
            content = record.get(countDownLatch);
            return content;
        } catch (InterruptedException e) {
            log.error("等待事件通知出现异常", e);
            return null;
        } finally {
            closeEvent(taskId, countDownLatch);
        }
    }

    /**
     * 获取事件内容
     *
     * @param taskId  事件任务id
     * @param timeout 超时值，单位秒
     * @return 事件内容
     */
    public static ProtocolTreeNode getEventContent(String taskId, long timeout) {
        ProtocolTreeNode content = null;
        CountDownLatch countDownLatch = recordObject.get(taskId);
        if (countDownLatch == null) {
            return null;
        }
        try {
            countDownLatch.await(timeout, TimeUnit.SECONDS);
            content = record.get(countDownLatch);
            return content;
        } catch (InterruptedException e) {
            log.error("等待事件通知出现异常", e);
            return null;
        } finally {
            closeEvent(taskId, countDownLatch);
        }
    }

    /**
     * 关闭事件
     *
     * @param taskId 事件id
     */
    public static void closeEvent(String taskId) {
        CountDownLatch countDownLatch = recordObject.get(taskId);
        recordObject.remove(taskId);
        record.remove(countDownLatch);
        consumerObject.remove(taskId);
    }

    /**
     * 关闭事件
     *
     * @param taskId         任务id
     * @param countDownLatch 事件对象
     */
    private static void closeEvent(String taskId, CountDownLatch countDownLatch) {
        record.remove(countDownLatch);
        recordObject.remove(taskId);
        consumerObject.remove(taskId);
    }

    /**
     * 获取当前正在执行的事件任务数
     */
    public static int getTaskNum() {
        return record.size();
    }
}
