package com.imx.netty.core;

import com.imx.apns.common.APNsState;

public interface FutureNotification<E> {
    void OnApnsConnectSuccess();

    void OnApnsNotify(E e);

    void OnApnsState(APNsState apnsState);

    void OnApnsClose(String reason, boolean force);

}
