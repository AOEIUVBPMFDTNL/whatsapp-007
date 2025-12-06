package com.whatsapp.android.entity;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.constant.Constant;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-24 11:06
 */
@Data
public class StatusResult {
    private String status = Constant.OK;
    private String message;

    public StatusResult() {
    }

    public StatusResult(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static StatusResult ok() {
        return new StatusResult(Constant.OK, "成功");
    }

    public static StatusResult ok(String message) {
        return new StatusResult(Constant.OK, message);
    }

    public static StatusResult fail() {
        return new StatusResult(Constant.FAIL, "失败");
    }

    public static StatusResult fail(String message) {
        return new StatusResult(Constant.FAIL, message);
    }

    public static String failStr(String message) {
        return JSONObject.toJSONString(new StatusResult(Constant.FAIL, message));
    }
}
