package com.imx.netty.core;

import com.imx.apns.common.APNsNotification;
import com.imx.apns.common.PayloadType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayloadResult {
    private APNsNotification notification;
}
