package com.whatsapp.android.entity;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.run.StartedUpRunner;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-24 11:43
 */
@Data
public class Result {
    private String type;
    /**
     * 商户id
     */
    private String mchId;
    private int platformType = Constant.PLATFORM_TYPE;
    private String countryCode = StartedUpRunner.countryCode;
    private DataBean data;

    public Result() {
    }

    private Result(String type, DataBean data) {
        this.type = type;
        this.data = data;
    }

    private Result(String type, DataBean data, String mchId) {
        this.type = type;
        this.data = data;
        this.mchId = mchId;
    }

    public static <T> Result taskSuccess(String type, String taskId, String username, T data) {
        DataBean dataBean = new DataBean(BodyBean.success(type, taskId, data), System.currentTimeMillis(), TypeConstant.WM_TASK, username);
        return new Result(TypeConstant.NOTIFY, dataBean);
    }

    public static <T> Result taskFail(String type, String taskId, String username, T data) {
        DataBean dataBean = new DataBean(BodyBean.fail(type, taskId, data), System.currentTimeMillis(), TypeConstant.WM_TASK, username);
        return new Result(TypeConstant.NOTIFY, dataBean);
    }

    public static Result taskNotify(String type, String taskId, String username) {
        DataBean dataBean = new DataBean(BodyBean.notify(type, taskId), System.currentTimeMillis(), TypeConstant.WM_TASK, username);
        return new Result(TypeConstant.NOTIFY, dataBean);
    }

    public static <T> Result callback(String type, String taskId, String username, T data) {
        DataBean dataBean = new DataBean(BodyBean.success(type, taskId, data), System.currentTimeMillis(), TypeConstant.CALLBACK, username);
        return new Result(TypeConstant.NOTIFY, dataBean);
    }

    public static <T> Result callback(String type, String taskId, String username, T data, long time) {
        DataBean dataBean = new DataBean(BodyBean.success(type, taskId, data), time == 0 ? System.currentTimeMillis() : time, TypeConstant.CALLBACK, username);
        return new Result(TypeConstant.NOTIFY, dataBean);
    }

    public static <T> Result callback(String type, String taskId, String username, T data, long time, String mchId) {
        DataBean dataBean = new DataBean(BodyBean.success(type, taskId, data), time == 0 ? System.currentTimeMillis() : time, TypeConstant.CALLBACK, username);
        return new Result(TypeConstant.NOTIFY, dataBean, mchId);
    }

    public String toJson() {
        return JSONObject.toJSONString(this);
    }
}
