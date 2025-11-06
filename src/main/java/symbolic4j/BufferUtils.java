package symbolic4j;

import symbolic4j.type.Pair;

import java.nio.ByteBuffer;

public class BufferUtils {

    /**
     * 读取无符号 LEB128 编码
     *
     * @param buffer ByteBuffer对象
     * @return 解码后的值
     */
    public static int readUnsignedLEB128(ByteBuffer buffer) {
        int result = 0;
        int shift = 0;
        byte b;

        do {
            b = buffer.get();
            result |= (b & 0x7f) << shift;   // 读取低7位并左移到对应位置
            shift += 7;
        } while ((b & 0x80) != 0);          // 检查最高位是否为1

        return result;
    }

    public static ByteBuffer alignBuffer(ByteBuffer buffer, int alignment) {
        int padding = buffer.position() % alignment;

        if (padding != 0) {
            ByteBuffer dup = buffer.duplicate();
            dup.position(buffer.position() + alignment - padding);

            int newLimit = buffer.limit() + alignment - padding;
            if (newLimit > buffer.capacity()) {
                newLimit = buffer.capacity();
            }

            dup.limit(newLimit);
            return dup;
        }

        return buffer;
    }

    /**
     * Split a buffer into two buffers at the given offset.
     *
     * @param buffer [0, offset)
     * @param offset [offset, buffer.limit())
     */
    public static Pair<ByteBuffer, ByteBuffer> splitBuffer(ByteBuffer buffer, int offset) {

        int originalPosition = buffer.position();

        ByteBuffer left = buffer.duplicate();
        left.limit(offset + originalPosition);

        ByteBuffer right = buffer.duplicate();
        right.position(offset + originalPosition);

        return new Pair<>(left, right);
    }
}
