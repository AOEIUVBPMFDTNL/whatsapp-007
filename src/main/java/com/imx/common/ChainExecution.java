package com.imx.common;

public interface ChainExecution<E> {

    void execute(E e) throws Exception;

}
