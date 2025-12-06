package Wam;

import Wam.proto.WamEvent;
import Wam.proto.WamRecord;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * w:stats 编解码
 *
 * @author Rocky
 */
@Slf4j
public class Wam {

    public static WamRecord.Builder deserializer(String stats) {
        // 存放wamEvent事件
        Event event = new Event();
        byte[] data = Base64.decode(stats);
        ByteBuffer buffer = ByteBuffer.wrap(data, 8, data.length - 8);
        // 存放事件
        List<Event> eventList = new ArrayList<>();
        // 存放默认参数
        List<Record> recordList = new ArrayList<>();
        WamRecord.Builder recordBuilder = WamRecord.newBuilder();
        while (true) {
            if (!buffer.hasRemaining()) {
                break;
            }
            try {
                Record record = WAMOutputstream.ByteBufferToRecord(buffer);
                System.out.println(record);
                int channel = record.channel;
                switch (channel) {
                    case 0:
                        // 为基础参数, 非事件内容
                        recordList.add(record);
                        continue;
                    case 1:
                        if (!event.getEventValue().isEmpty()) {
                            eventList.add(event);
                        }
                        event = new Event(record.tag);
                        continue;
                    case 2:
                        event.addEventValue(record);
                        continue;
                    default:
                        log.error("非法的Wam-channel值: {}", channel);
                }
            } catch (Exception e) {
                log.error("Wam日志解码异常", e);
                break;
            }
        }
        if (!event.getEventValue().isEmpty()) {
            eventList.add(event);
        }
        Descriptors.Descriptor recordDescriptor = WamRecord.getDescriptor();
        // 先解码基础参数
        for (Record r : recordList) {
            int tag = r.tag;
            Descriptors.FieldDescriptor recordFieldNumber = recordDescriptor.findFieldByNumber(tag);
            if (ObjectUtil.isNotNull(recordFieldNumber)) {
                if (ObjectUtil.isNotNull(r.value)) {
                    recordBuilder.setField(recordFieldNumber, r.getProtobufValue());
                }
            } else {
                log.warn("WamRecord 找不到tag为{}的含义, 请补充wamRecord参数, record: {}", tag, r);
            }
        }
        // 解码WamEvent事件
        Descriptors.Descriptor EventDescriptor = WamEvent.getDescriptor();
        for (Event e : eventList) {
            int tag = e.getTag();
            Descriptors.FieldDescriptor eventDescriptor = EventDescriptor.findFieldByNumber(tag);
            if (ObjectUtil.isNull(eventDescriptor)) {
                log.warn("WamEvent 找不到wam事件tag为 {} 的含义, 请补充WamEvent事件", tag);
            } else {
                WamEvent.Builder eventBuilder = WamEvent.newBuilder();
                Descriptors.Descriptor messageType = eventDescriptor.getMessageType();
                DynamicMessage.Builder subEventBuilder = DynamicMessage.newBuilder(messageType);
                for (Record r : e.getEventValue()) {
                    int eventSubRecordTag = r.tag;
                    Descriptors.FieldDescriptor eventSubRecordDescriptor = messageType.findFieldByNumber(eventSubRecordTag);
                    if (ObjectUtil.isNotNull(eventSubRecordDescriptor)) {
                        if (ObjectUtil.isNotNull(r.getProtobufValue())) {
                            subEventBuilder.setField(eventSubRecordDescriptor, r.getProtobufValue());
                        }
                    } else {
                        log.warn("WamEvent tag: {}, 事件: {} , 找不到子参数含义, 子参数tag为: {}", tag, eventDescriptor.getFullName(), eventSubRecordTag);
                    }
                }
                eventBuilder.setField(eventDescriptor, subEventBuilder.build());
                recordBuilder.addEvents(eventBuilder);
            }
        }
        return recordBuilder;
    }

    public static byte[] serialize(WamRecord wamRecord) {
        Report.WAMOutputstream wam = new Report.WAMOutputstream();
        List<Descriptors.FieldDescriptor> recordField = wamRecord.getDescriptorForType().getFields();
        for (Descriptors.FieldDescriptor fieldDescriptor : recordField) {

            if (!fieldDescriptor.isRepeated()) {
                // 普通常规参数
                if (wamRecord.hasField(fieldDescriptor)) {
                    Object value = wamRecord.getField(fieldDescriptor);
                    wam.serialize(0, fieldDescriptor.getNumber(), value);
                }
            } else {
                List<WamEvent> eventList = (List<WamEvent>) wamRecord.getField(fieldDescriptor);
                // 写入event事件
                for (WamEvent wamEvent : eventList) {
                    for (Descriptors.FieldDescriptor eventFieldDescriptor : wamEvent.getDescriptorForType().getFields()) {
                        if (wamEvent.hasField(eventFieldDescriptor)) {
                            if (eventFieldDescriptor.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)) {
                                // 写入event事件id
                                wam.serialize(1, eventFieldDescriptor.getNumber(), -1);
                                Message eventMessage = (Message) wamEvent.getField(eventFieldDescriptor);
                                for (Descriptors.FieldDescriptor eventSubFieldDescriptor : eventMessage.getDescriptorForType().getFields()) {
                                    if (eventMessage.hasField(eventSubFieldDescriptor)) {
                                        // event事件下的值
                                        Object subFieldValue = eventMessage.getField(eventSubFieldDescriptor);
                                        wam.serialize(2, eventSubFieldDescriptor.getNumber(), subFieldValue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ByteBuffer buffer = wam.waByteArrayOutputStream.getByteBuffer();
        byte[] result = new byte[wam.waByteArrayOutputStream.size()];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return result;
    }

    public static void main(String[] args) {
        String s = "V0FNBQEGAABQLwVU+miIeRGUVlZXLDd3Ziw0b1IsMnBDLDZ0LGp5LEJFLEhvLHFBLDJqLDh6LDFGLEg5LDJJLGEsRE4sYlcsMiwyLDFhLDE2Qyw2QSwyUSwyZyxXMiw3UCw5LDJoLDF1LEVrLDIsNjMsMVAsRHMsNFIsMmwsNmksMiwzMSwyWCwzcCwxNCwxSixDLDIzLEExLDF4LEgsOXcscSwyZjh5BgQ4ewYCEBWAEQoyLjI1LjI2Ljc0GKs4gA0Mc2Ftc3VuZy1hMzR4mKUTEwE2MzE4Nyw3MzQyNCw2Mzg4Myw3MzUyMiw3MzQyMiw3MzQyMCw3ODI4Miw4NDkzNiw3Nzc1MCw4Njc0OCw4NTAxNyw4NTg5OCw4MjEwMSw4NTc3Nyw4MjU2NSw4MjEwMyw3ODQ0OCw4NTE1Niw4NDI4Nyw4Mzg3MSw4NDI4OSw2ODM5MCw4NjU4Myw2OTAyNCw3MDIyOSw3MDI2NSw4MDkwMiw2ODk0Nyw2NDk4Nyw3MDA4Nyw3ODEzMiw4NTkxNSw3MTA5NCw4MDg5Myw4MzcwMCw1OTkxMCw4MDQzNCw4MTA2OSw4NDYzMyw4NTAyOSw0OTA0OCw4NDg3MCw4NDQ3Niw4MTE4Nyw4NjUzMSw2ODIyMRj7LiinHBAFSI8CAAEQAyAXIGkoaxiADwIxMzALDRjtM0ixArUOSDkK3weIHwEHc2Ftc3VuZ4ghAQdzYW1zdW5nKKE7iOsKA3ZsbIjvAQRhMzR4SPEu8QEoDyg54Ar/EgGCAhJjbGlja190b19jaGF0X2xpbmtSBAVU+mg=";
        WamRecord.Builder deserializer = deserializer(s);
        System.out.println(deserializer);
    }

}
