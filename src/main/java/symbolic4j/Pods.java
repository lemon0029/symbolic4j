package symbolic4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Pods<T> {
    private final ByteBuffer buffer;

    public Pods(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    protected abstract T get(int index);

    protected ByteBuffer buffer(int offset) {
        ByteBuffer p = this.buffer.duplicate();
        int baseOffset = p.position();
        p.position(baseOffset + offset);
        p.order(ByteOrder.LITTLE_ENDIAN);

        return p;
    }
}
