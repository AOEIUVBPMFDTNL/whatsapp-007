package com.imx.apns.common;

import com.imx.common.Pair;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class APNsState implements Serializable {

    List<String> topics;
    Pair<byte[], byte[]> pair;
    byte[] token;
}
