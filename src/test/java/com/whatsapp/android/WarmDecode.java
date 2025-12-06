package com.whatsapp.android;

import Report.Record;
import Report.WAMOutputstream;
import cn.hutool.core.codec.Base64;

import java.nio.ByteBuffer;

public class WarmDecode {
    public static void main(String[] args) throws Exception {
        byte[] data = Base64.decode("V0FNBQEBAAAwCw+ADQhpUGhvbmUgN4APBDE0LjKAEQoyLjIzLjIzLjgyEBUgF1AvDitcZSBpKIMBOHkGBDh7BgIoaxgYpxyIkR4FMThCOTKIeSQEMTYuNBjtMxirOIj7PAk1Mzc5NDcyMzUpTgR2AQDQwcqhhRJAGS4LIgJSAXzaxwY2A/8ZLgsiAlIBpRRbAzYD/ylGBHICAADA06NglkByAwAAAE+PAnpAcgQAAIBgO9SOQHIBAAAAhMHqlUAmBVAvaCxcZYjrCgNydmEY+y4p9gEiASICFgcZLgsiAlIBmHBiCzYDHhkuCyICUgFY3IMCNgMHGS4LIgJSAbwcQgI2A1oZLgsyAgJSAaUUWwM2A/8ZLgsyAgJSAXzaxwY2A/8ZLgsiAlIBQ5DRDiYDUC9pLFxlKVADMgEEcgMAAAAA6MbePzYCAlAvaixcZSlQAzIBA3IDAAAAJBEocUA2AgI=");
        ByteBuffer bt = ByteBuffer.wrap(data, 8, data.length - 8);
        while (true) {
            if (!bt.hasRemaining()) {
                break;
            }
            Record record = WAMOutputstream.ByteBufferToRecord(bt);
            System.out.println(record);
        }
    }
}
