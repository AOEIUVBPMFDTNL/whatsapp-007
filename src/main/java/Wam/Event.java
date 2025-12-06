package Wam;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * w:stats中的事件
 *
 * @author Rocky
 */
public class Event {
    @Getter
    private int tag;
    @Getter
    private List<Record> eventValue;

    public Event(int tag) {
        this.tag = tag;
        this.eventValue = new ArrayList<>();
    }

    public Event() {
        this.eventValue = new ArrayList<>();
    }

    public void addEventValue(Record record) {
        this.eventValue.add(record);
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \nWamEvent: tag=").append(tag + "\n").append("Records: \n");
        for (Record record : eventValue) {
            stringBuilder.append(record.toString()).append("\n");
        }
        stringBuilder.append("\n}");
        return stringBuilder.toString();
    }
}
