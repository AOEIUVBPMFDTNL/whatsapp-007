package Wam;

import cn.hutool.core.util.ObjectUtil;

public class Record {
    public final int tag;
    public final int channel;
    public final Object value;

    public Record(int channel, int tag, Object value) {
        this.channel = channel;
        this.tag = tag;
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Record{\tchannel= ").append(channel).append("\ttag= ").append(tag).append(",\tvalue= ").append(value);
        if (ObjectUtil.isNotNull(value)) {
            stringBuilder.append(": ").append(value.getClass().getName());
        }
        return stringBuilder.toString();
    }

    public Object getProtobufValue() {
        if (ObjectUtil.isNotNull(value)) {
            if (value instanceof String) {
                return value;
            } else if (value instanceof Byte) {
                return ((Byte) value).longValue();
            } else if (value instanceof Short) {
                return ((Short) value).longValue();
            } else if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Double) {
                return ((Double) value).longValue();
            } else {
                return value;
            }
        }
        return null;
    }
}
