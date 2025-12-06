package Report;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WAByteArrayOutputStream extends ByteArrayOutputStream {
    public final ByteBuffer getByteBuffer() {
        ByteBuffer v1 = ByteBuffer.wrap(this.buf, 0, this.size());
        v1.order(ByteOrder.LITTLE_ENDIAN);
        return v1;
    }

    public final byte[] getBytes() {
        return this.buf;
    }
}
