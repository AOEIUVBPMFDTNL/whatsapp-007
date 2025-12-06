package com.imx.netty.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Result<E> {

    E getValue();

    void setValue(E value);

    Throwable getException();

    void setException(Throwable t);

    boolean hasException();

    Result<E> get() throws InterruptedException, ExecutionException;

    Result<E> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
