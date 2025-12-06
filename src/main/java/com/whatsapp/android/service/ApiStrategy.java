package com.whatsapp.android.service;

import com.alibaba.fastjson.JSONObject;
import com.whatsapp.android.entity.Result;
import com.whatsapp.android.entity.User;


/**
 * @author sunnoc
 * @date 2020-10-16 18:22
 */
public interface ApiStrategy {
    /**
     * 执行任务
     *
     * @param type       任务类型
     * @param dataObject data对象
     * @param taskId     任务id
     * @param user       ins用户信息
     * @return Result
     */
    Result execute(String type, JSONObject dataObject, String taskId, User user);
}
