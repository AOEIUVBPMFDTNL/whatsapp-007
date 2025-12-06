package com.imx.netty.core;

import com.imx.apns.common.APNsState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@Builder
public class APNsClientContext {
    private APNsState apNsState;
    private List<String> topics;
    private FutureNotification<PayloadResult> futureResult;
}
