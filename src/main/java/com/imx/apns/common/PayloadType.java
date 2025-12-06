package com.imx.apns.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum PayloadType {

    TOKEN(8),
    NOTIFICATION(10),
    KEEPALIVE(13),
    SUBSCRIPTION(14),
    ;

    final int type;

    static final Map<Integer, PayloadType> TYPES =
            Arrays.stream(values()).collect(Collectors.toMap(PayloadType::getType, Function.identity()));

    public static PayloadType byType(int type) {
        return TYPES.get(type);
    }

}
