package com.whatsapp.android;

import Env.DeviceEnv;
import cn.hutool.core.codec.Base64;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author sunnoc
 * @date 2022-12-17 06:49
 */
public class DeviceParseTest {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        String content = "Cgw5MTg4NTM0MTM5NzASRAogSgudyj1ghOp37WH7p910/NMZ+tgk008QLZJc5LIKkWwSIOjQHbMuvbXokSSN6pEH7RP6N5GgFu0Ftvk49WmFwnNPGgAiIKiVr0rbTaKaoENgoF2E3OI5klClndUb9kEzG0MmKSsGKhRiOGI0NmY0YS00ZDc3LTRiZWQtYjIkYjhiNDZmNGEtNGQ3Ny00YmVkLWI4N2EtZDYxY2RmOTQ2MGJhOgQIAggFQgJmYkpoCAwSCAgCEBYYFSBNGgMyNjAiAzAwMyoGMTIuMC4xMgVBcHBsZToJaVBob25lIFhSQgYxNkE0MDRKJGFiYzRkM2JjLTcyNWUtNDc4ZC1iN2U1LTQzM2VhZGI4MDQ5MFAAWgJwbGICUExoJA==";
        byte[] decode = Base64.decode(content);
        DeviceEnv.AndroidEnv.Builder builder = DeviceEnv.AndroidEnv.parseFrom(decode).toBuilder();
        System.out.println(builder);
    }
}
