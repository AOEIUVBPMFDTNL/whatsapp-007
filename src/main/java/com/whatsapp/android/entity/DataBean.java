package com.whatsapp.android.entity;

import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-24 12:09
 */
@Data
public class DataBean {
    private BodyBean body;
    private long time;
    private String type;
    private String username;

    public DataBean() {
    }

    public DataBean(BodyBean body, long time, String type, String username) {
        this.body = body;
        this.time = time;
        this.type = type;
        this.username = username;
    }


}
