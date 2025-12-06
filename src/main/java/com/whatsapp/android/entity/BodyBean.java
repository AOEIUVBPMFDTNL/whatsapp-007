package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-24 12:10
 */
@Data
public class BodyBean<T> {
    private int code;
    private String type;
    private String taskId;
    private T msg;

    public BodyBean() {
    }

    public BodyBean(int code, String type, String taskId, T msg) {
        this.code = code;
        this.type = type;
        this.taskId = taskId;
        this.msg = msg;
    }

    public static <T> BodyBean<T> notify(String type, String taskId) {
        return new BodyBean<>(0, type, taskId, null);
    }

    public static <T> BodyBean<T> success(String type, String taskId, T msg) {
        return new BodyBean<>(1, type, taskId, msg);
    }

    public static <T> BodyBean<T> fail(String type, String taskId, T msg) {
        return new BodyBean<>(-1, type, taskId, msg);
    }
}
