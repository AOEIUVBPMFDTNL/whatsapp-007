package Report;

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
        return "Record{" +
                "channel=" + channel +
                ", tag=" + tag +
                ",\tvalue=" + value +  ":" + value.getClass().getName() +
                '}';
    }
}
