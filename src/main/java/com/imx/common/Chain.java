package com.imx.common;

import java.util.List;

public class Chain<E> {

    public Chain<E> nextChain;

    public Chain<E> setNextChain(Chain<E> nextChain) {
        this.nextChain = nextChain;
        return nextChain;
    }

    public void process(E e) {
        if (nextChain == null) {
            return;
        }
        nextChain.process(e);
    }

    public void process(List<E>... e) {
        if (nextChain == null) {
            return;
        }
        nextChain.process(e);
    }

}
