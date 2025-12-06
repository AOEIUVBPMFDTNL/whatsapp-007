package com.imx.apns.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class APNsNotification {
    byte[] token;
    byte[] topicHash;
    byte[] payload;
    Integer messageId;
    Integer timestamp;
}
