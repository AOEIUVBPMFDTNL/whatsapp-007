package com.imx.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class Pair<K, V> implements Serializable {
    private final K key;
    private final V value;
}
