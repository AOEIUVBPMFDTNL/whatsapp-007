package Wam;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class WAMOutputstream {
    public int curIndex;
    public int A01;
    public final WAByteArrayOutputStream waByteArrayOutputStream;

    public WAMOutputstream() {
        this.waByteArrayOutputStream = new WAByteArrayOutputStream();
        this.curIndex = -1;
    }

    public static int writeLong(long arg5, WAByteArrayOutputStream arg7) {
        if (arg5 == 0L) {
            return 1;
        }

        if (arg5 == 1L) {
            return 2;
        }

        arg7.write(((byte) (((int) arg5))));
        if (0xFFFFFFFFFFFFFF80L <= arg5 && arg5 <= 0x7FL) {
            return 3;
        }

        arg7.write(((byte) (((int) (arg5 >> 8)))));
        if (0xFFFFFFFFFFFF8000L <= arg5 && arg5 <= 0x7FFFL) {
            return 4;
        }

        arg7.write(((byte) (((int) (arg5 >> 16)))));
        arg7.write(((byte) (((int) (arg5 >> 24)))));
        if (0xFFFFFFFF80000000L <= arg5 && arg5 <= 0x7FFFFFFFL) {
            return 5;
        }

        arg7.write(((byte) (((int) (arg5 >> 0x20)))));
        arg7.write(((byte) (((int) (arg5 >> 40)))));
        arg7.write(((byte) (((int) (arg5 >> 0x30)))));
        arg7.write(((byte) (((int) (arg5 >> 56)))));
        return 6;
    }

    public static int writeLongAsInt(long arg3, WAByteArrayOutputStream arg5) {
        if (arg3 >= 0L && arg3 <= 0xFFFFFFFFL) {
            arg5.write(((byte) (((int) arg3))));
            if (arg3 <= 0xFFL) {
                return 1;
            }

            arg5.write(((byte) (((int) (arg3 >> 8)))));
            if (arg3 <= 0xFFFFL) {
                return 2;
            }

            arg5.write(((byte) (((int) (arg3 >> 16)))));
            arg5.write(((byte) (((int) (arg3 >> 24)))));
            return 4;
        }

        throw new IllegalArgumentException("Value is not an unsigned integer");
    }

    public static long ByteBufferToLong(int len, ByteBuffer arg8) {
        if (4 >= len) {
            long v5 = 0L;
            int v4;
            for (v4 = 0; v4 < len; ++v4) {
                v5 |= (((long) arg8.get()) & 0xFFL) << (v4 << 3);
            }

            return v5;
        }

        throw new IllegalArgumentException("Invalid number of bytes: ");
    }

    public static Record ByteBufferToRecord(ByteBuffer byteBuffer) throws Exception {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int at = byteBuffer.position();
        byte recordType = byteBuffer.get();
        int channel = recordType & 3;
        if (channel > 2) {
            throw new IllegalArgumentException("Invalid record type");
        }

        int v0 = 1;
        if ((recordType & 8) == 0) {
            v0 = 0;
        }

        int tag = (int) ((int) v0 == 0 ? WAMOutputstream.ByteBufferToLong(1, byteBuffer) : WAMOutputstream.ByteBufferToLong(2, byteBuffer));
        int valueType = recordType >> 4 & 15;
        if (valueType <= 10) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            switch (valueType) {
                case 0: {
                    return new Record(channel, tag, null);
                }
                case 1: {
                    return new Record(channel, tag, ((int) 0));
                }
                case 2: {
                    return new Record(channel, tag, ((int) 1));
                }
                case 3: {
                    return new Record(channel, tag, ((byte) byteBuffer.get()));
                }
                case 4: {
                    return new Record(channel, tag, ((short) byteBuffer.getShort()));
                }
                case 5: {
                    return new Record(channel, tag, ((int) byteBuffer.getInt()));
                }
                case 6: {
                    return new Record(channel, tag, ((long) byteBuffer.getLong()));
                }
                case 7: {
                    return new Record(channel, tag, ((double) byteBuffer.getDouble()));
                }
                case 8: {
                    return new Record(channel, tag, WAMOutputstream.ByteBufferToString(((int) WAMOutputstream.ByteBufferToLong(1, byteBuffer)), byteBuffer));
                }
                case 9: {
                    return new Record(channel, tag, WAMOutputstream.ByteBufferToString(((int) WAMOutputstream.ByteBufferToLong(2, byteBuffer)), byteBuffer));
                }
                case 10: {
                    break;
                }
                default: {
                    throw new Error("Invalid value type");
                }
            }

            return new Record(channel, tag, WAMOutputstream.ByteBufferToString(((int) WAMOutputstream.ByteBufferToLong(4, byteBuffer)), byteBuffer));
        }

        try {
            StringBuilder v1 = new StringBuilder();
            v1.append("Invalid value type ");
            v1.append(valueType);
            throw new IllegalArgumentException(v1.toString());
        } catch (IllegalArgumentException v3_1) {
            String tag2 = String.format(Locale.US, "%02X ", ((byte) recordType));
            StringBuilder v0_1 = new StringBuilder();
            v0_1.append(v3_1);
            v0_1.append(" at ");
            v0_1.append(at);
            v0_1.append(", tag: ");
            v0_1.append(tag2);
            throw new Exception(v0_1.toString());
        }

        // try {
        //     throw new IllegalArgumentException("Invalid record type");
        // }
        // catch(IllegalArgumentException v3_2) {
        //     String v1_2 = String.format(Locale.US, "%02X ", ((byte)recordType));
        //     StringBuilder v0_2 = new StringBuilder();
        //     v0_2.append(v3_2);
        //     v0_2.append(" at ");
        //     v0_2.append(at);
        //     v0_2.append(", tag: ");
        //     v0_2.append(v1_2);
        //     throw new Exception(v0_2.toString());
        // }
    }

    public static String ByteBufferToString(int arg2, ByteBuffer arg3) throws Exception {
        byte[] v2 = new byte[arg2];
        arg3.get(v2);
        try {
            return new String(v2, "UTF-8");
        } catch (UnsupportedEncodingException v2_1) {
            StringBuilder v0 = new StringBuilder("UnsupportedEncoding: ");
            v0.append(v2_1);
            throw new Exception(v0.toString());
        }
    }

    public void reset() {
        this.waByteArrayOutputStream.reset();
        this.curIndex = -1;
        this.A01 = 0;
    }

    public final void serialize(int channel, int tag, Object obj) {
        byte[] v4_1;
        int v0 = 0;
        WAByteArrayOutputStream waByteArrayOutputStream = this.waByteArrayOutputStream;
        this.curIndex = waByteArrayOutputStream.size();
        int flag = 0;
        waByteArrayOutputStream.write(0);
        int v1 = WAMOutputstream.writeLongAsInt(((long) tag), waByteArrayOutputStream);
        if (v1 != 1) {
            if (v1 == 2) {
                flag = 1;
                // goto label_17;
            }

            // throw new Error("Id too big to fit in 2 bytes");
        }

        // label_17:
        if (obj == null) {
            v0 = 0;
        } else {
            if ((obj instanceof Boolean)) {
                v0 = WAMOutputstream.writeLong(((Boolean) obj).booleanValue() ? 1L : 0L, waByteArrayOutputStream);
                waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
                ++this.A01;
                return;
            }

            if ((obj instanceof Long)) {
                v0 = WAMOutputstream.writeLong(((Number) obj).longValue(), waByteArrayOutputStream);
                waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
                ++this.A01;
                return;
            }

            if ((obj instanceof Number)) {
                double v4 = ((Number) obj).doubleValue();
                long v2 = (long) v4;
                if (((double) v2) == v4) {
                    v0 = WAMOutputstream.writeLong(v2, waByteArrayOutputStream);
                } else {
                    waByteArrayOutputStream.write(((byte) (((int) Double.doubleToRawLongBits(v4)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 8)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 16)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 24)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 0x20)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 40)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 0x30)))));
                    waByteArrayOutputStream.write(((byte) (((int) (Double.doubleToRawLongBits(v4) >> 56)))));
                    v0 = 7;
                    waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
                    ++this.A01;
                    return;
                    // label_87:

                }
            }

            if (obj instanceof String) {
                String v13 = (String) obj;
                try {
                    v4_1 = v13.getBytes("UTF-8");
                } catch (UnsupportedEncodingException v1_1) {
                    throw new Error(v1_1);
                }

                if (v4_1.length > 0x400) {
                    // Log.w(String.format(Locale.US, "wam/serialize: string length is limited to %d UTF-8 bytes", ((int)0x400)));
                }

                int v2_1 = Math.min(v4_1.length, 0x400);
                int v1_2 = WAMOutputstream.writeLongAsInt(((long) v2_1), waByteArrayOutputStream);
                waByteArrayOutputStream.write(v4_1, 0, v2_1);
                if (v1_2 != 1) {
                    if (v1_2 != 2) {
                        if (v1_2 == 4) {
                            v0 = 10;
                            waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
                            ++this.A01;
                            return;
                        }

                        throw new Error("Impossible");
                    }

                    v0 = 9;
                    waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
                    ++this.A01;
                    return;
                }

                v0 = 8;
            }
        }

        waByteArrayOutputStream.getBytes()[this.curIndex] = (byte) (channel | (v0 << 4 | flag << 3));
        ++this.A01;
        // label_139:
        // throw new IllegalArgumentException("Expected class Boolean, Number, or String, got ");
    }
}