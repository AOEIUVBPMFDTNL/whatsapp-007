package com.imx.netty.core;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class Response<E>  implements Result<E> {

    private E result;

    private Throwable exception;

    @Override
    public E getValue() {
        return result;
    }

    public void setValue(E value) {
        this.result = value;
    }

    @Override
    public boolean hasException() {
        return Objects.nonNull(exception);
    }

    @Override
    public Result<E> get() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<E> get(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

}
