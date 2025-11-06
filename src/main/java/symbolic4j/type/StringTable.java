package symbolic4j.type;

import symbolic4j.BufferUtils;

import java.nio.ByteBuffer;

public record StringTable(ByteBuffer buffer) {

    public int size() {
        return buffer.remaining();
    }

    public String get(int offset) {
        if (offset < 0 || offset >= buffer.remaining()) {
            return "";
        }

        ByteBuffer duplicate = buffer.duplicate();

        int originalPosition = duplicate.position();
        duplicate.position(originalPosition + offset);

        int len = BufferUtils.readUnsignedLEB128(duplicate);
        byte[] bytes = new byte[len];

        duplicate.get(bytes);

        return new String(bytes);
    }
}
