package com.whatsapp.android;

import cn.hutool.core.util.HexUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import org.microg.gms.gcm.mcs.Mcs;

/**
 * @author sunnoc
 * @date 2023-05-24 11:53
 */
public class ParseGcmDataTest {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        String hex = "10011a003a04080d120050046000";
        byte[] bytes = HexUtil.decodeHex(hex);
        Mcs.IqStanza iqStanza = Mcs.IqStanza.parseFrom(bytes);
        System.out.println(iqStanza);
        /*Mcs.DataMessageStanza dataMessageStanza = Mcs.DataMessageStanza.parseFrom(bytes);
        System.out.println(dataMessageStanza);*/
    }
}
