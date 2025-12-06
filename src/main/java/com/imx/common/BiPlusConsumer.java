package com.imx.common;

@FunctionalInterface
public interface BiPlusConsumer<T, U, O> {

    void accept(T t, U u, O o);

}
