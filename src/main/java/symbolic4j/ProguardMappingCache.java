package symbolic4j;

import com.google.common.primitives.Ints;
import kotlin.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static symbolic4j.BufferUtils.alignBuffer;
import static symbolic4j.BufferUtils.splitBuffer;


public record ProguardMappingCache(Header header,
                                   Classes classes,
                                   Members members,
                                   Members membersByParams,
                                   StringTable stringTable) {

    private static final int HEADER_SIZE = 24;
    private static final int CLASS_SIZE = 28;
    private static final int MEMBER_SIZE = 36;

    // 按理来说是 PRGC, 但是由于字节序的问题，这里需要反过来，不知道为什么 Magic Number 是大端序的，而其它数据是小端序的...
    private static final byte[] PRGCACHE_MAGIC = "CGRP".getBytes();
    private static final int PRGCACHE_VERSION = 1;

    public static ProguardMappingCache load(Path file) {

        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            Pair<Header, ByteBuffer> headerPair = loadHeader(buffer);

            Header header = headerPair.getFirst();

            if (!Arrays.equals(Ints.toByteArray(header.magic), PRGCACHE_MAGIC)) {
                throw new IllegalStateException("Invalid ProguardMappingCache file, magic number mismatch");
            }

            if (header.version != PRGCACHE_VERSION) {
                throw new IllegalStateException("Invalid ProguardMappingCache file, version mismatch");
            }

            Pair<Classes, ByteBuffer> classesPair = loadClasses(headerPair.getSecond(), header.numOfClasses);
            Pair<Members, ByteBuffer> membersPair = loadMembers(classesPair.getSecond(), header.numOfMembers);
            Pair<Members, ByteBuffer> membersByParamsPair = loadMembers(membersPair.getSecond(), header.numOfMembersByParams);

            if (membersByParamsPair.getSecond().remaining() < header.stringBytes) {
                throw new IllegalStateException("Invalid ProguardMappingCache file, string section too short");
            }

            Classes classes = classesPair.getFirst();
            Members members = membersPair.getFirst();
            Members membersByParams = membersByParamsPair.getFirst();
            ByteBuffer stBuffer = alignBuffer(membersByParamsPair.getSecond(), 8);
            StringTable stringTable = new StringTable(stBuffer);

            return new ProguardMappingCache(header, classes, members, membersByParams, stringTable);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ProguardRemapResult remapFrame(String className, String methodName, int line) {
        Class clazz = getClass(className);
        if (clazz == null) {
            return null;
        }

        String originalClassName = stringTable.get(clazz.originalNameOffset);

        List<Member> matchingMembers = getClassMembers(clazz, methodName);

        if (matchingMembers.isEmpty()) {
            return null;
        }

        List<ProguardRemapResult.Frame> frames = new ArrayList<>();

        for (Member member : matchingMembers) {
            if (member.endLine > 0 && (line < member.startLine || line > member.endLine)) {
                continue;
            }

            int sourceLine;

            if (member.originalStartLine == -1) {
                sourceLine = member.originalEndLine;
            } else if (member.originalEndLine == -1) {
                sourceLine = member.originalStartLine;
            } else if (member.originalStartLine == member.originalEndLine) {
                sourceLine = member.originalEndLine;
            } else {
                sourceLine = line - member.startLine + member.originalStartLine;
            }

            String sourceClass = member.originalClassNameOffset == -1 ?
                    originalClassName :
                    stringTable.get(member.originalClassNameOffset);

            String sourceFile = "";
            if (member.originalFileNameOffset != -1) {
                sourceFile = stringTable.get(member.originalFileNameOffset);

                if ("R8$$SyntheticClass".equals(sourceFile)) {
                    String[] parts = sourceClass.split(",");
                    String lastPart = parts[parts.length - 1];

                    sourceFile = lastPart.split("\\$")[0];
                }
            } else if (member.originalClassNameOffset != -1) {
                sourceFile = "";
            }

            String sourceMethod = stringTable.get(member.originalMethodNameOffset);

            if (sourceClass.contains("$$InternalSyntheticLambda") ||
                    sourceMethod.contains("$r8$lambda")) {
                frames.add(null);
                continue;
            }

            ProguardRemapResult.Frame frame = new ProguardRemapResult.Frame(sourceClass, sourceMethod, sourceFile, sourceLine,
                    (member.paramsOffset != -1) ? stringTable.get(member.paramsOffset) : "");

            frames.add(frame);
        }

        return new ProguardRemapResult(frames);
    }


    public Class getClass(String obfuscatedClassName) {
        int left = 0, right = header.numOfClasses - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Class clazz = classes.getClassAt(mid);
            String str = stringTable.get(clazz.obfuscatedNameOffset);
            int cmp = str.compareTo(obfuscatedClassName);
            if (cmp == 0) {
                return clazz;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return null;
    }

    public String remapClassName(String obfuscatedClassName) {
        Class clazz = getClass(obfuscatedClassName);
        if (clazz == null) {
            return null;
        }

        return stringTable.get(clazz.originalNameOffset);
    }

    public Pair<String, String> remapMethodName(String obfuscatedClassName, String obfuscatedMethodName) {
        Class clazz = getClass(obfuscatedClassName);
        if (clazz == null) {
            return null;
        }

        List<Member> matchingMembers = getClassMembers(clazz, obfuscatedMethodName);

        if (matchingMembers.isEmpty()) {
            return null;
        }

        // 可能会有多个匹配项（内联函数？）
        Member firstMatchedMember = matchingMembers.getFirst();

        if (matchingMembers.stream().anyMatch(it ->
                it.originalMethodNameOffset != firstMatchedMember.originalMethodNameOffset)) {
            return null;
        }

        String originalClassName = stringTable.get(clazz.originalNameOffset);
        String originalMethodName = stringTable.get(firstMatchedMember.originalMethodNameOffset);

        return new Pair<>(originalClassName, originalMethodName);
    }

    public List<Member> getClassMembers(Class clazz, String obfuscatedMethodName) {
        List<Member> classMembers = members.subList(clazz.membersOffset, clazz.numOfMembers);
        List<Member> matchingMembers = new ArrayList<>();

        for (Member member : classMembers) {
            String str = stringTable.get(member.obfuscatedNameOffset);
            if (str.equals(obfuscatedMethodName)) {
                matchingMembers.add(member);
            }
        }

        return matchingMembers;
    }

    public String findSourceFile(String className) {
        // TODO, 这里是假设传入的 className 为混淆后的类名，而实际需要根据原始类名查找
        Class clazz = getClass(className);
        if (clazz == null || clazz.fileNameOffset == -1) {
            return null;
        }

        return stringTable.get(clazz.fileNameOffset);
    }

    private static Pair<Header, ByteBuffer> loadHeader(ByteBuffer buffer) {
        Pair<ByteBuffer, ByteBuffer> pair = splitBuffer(buffer, HEADER_SIZE);

        pair.getFirst().order(ByteOrder.LITTLE_ENDIAN);

        int magic = pair.getFirst().getInt();
        int version = pair.getFirst().getInt();
        int numOfClasses = pair.getFirst().getInt();
        int numOfMembers = pair.getFirst().getInt();
        int numOfMembersByParams = pair.getFirst().getInt();
        int stringBytes = pair.getFirst().getInt();
        Header header = new Header(magic, version, numOfClasses, numOfMembers, numOfMembersByParams, stringBytes);

        return new Pair<>(header, pair.getSecond());
    }

    private static Pair<Classes, ByteBuffer> loadClasses(ByteBuffer buffer, int numOfClasses) {
        int expectedSize = CLASS_SIZE * numOfClasses;

        if (buffer.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid ProguardMappingCache file, class section too short");
        }

        ByteBuffer aligned = alignBuffer(buffer, 8);
        Pair<ByteBuffer, ByteBuffer> pair = splitBuffer(aligned, expectedSize);

        Classes classes = new Classes(pair.getFirst());

        return new Pair<>(classes, pair.getSecond());
    }

    private static Pair<Members, ByteBuffer> loadMembers(ByteBuffer buffer, int nums) {
        int expectedSize = MEMBER_SIZE * nums;

        if (buffer.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid ProguardMappingCache file, member section too short");
        }

        ByteBuffer aligned = alignBuffer(buffer, 8);
        Pair<ByteBuffer, ByteBuffer> pair = splitBuffer(aligned, expectedSize);

        Members members = new Members(pair.getFirst());

        return new Pair<>(members, pair.getSecond());
    }


    public record Classes(ByteBuffer buffer) {

        public int size() {
            return buffer.remaining() / CLASS_SIZE;
        }

        public Class getClassAt(int index) {
            var p = buffer.duplicate();
            int baseOffset = p.position();
            p.position(baseOffset + index * CLASS_SIZE);
            p.order(ByteOrder.LITTLE_ENDIAN);

            return new Class(p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt());
        }
    }

    public record Members(ByteBuffer buffer) {

        public int size() {
            return buffer.remaining() / MEMBER_SIZE;
        }

        public List<Member> subList(int start, int size) {
            List<Member> members = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                Member member = getMemberAt(start + i);
                members.add(member);
            }

            return members;
        }

        public Member getMemberAt(int index) {
            var p = buffer.duplicate();
            int baseOffset = p.position();
            p.position(baseOffset + index * MEMBER_SIZE);
            p.order(ByteOrder.LITTLE_ENDIAN);

            return new Member(p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt(), p.getInt());
        }
    }

    public record Header(
            // The file magic representing the file format and endianness.
            int magic,

            // The ProguardCache Format Version.
            int version,

            // The number of class entries in this cache.
            int numOfClasses,

            // The total number of member entries in this cache.
            int numOfMembers,

            // The total number of member-by-params entries in this cache.
            int numOfMembersByParams,

            // The number of string bytes in this cache.
            int stringBytes
    ) {
    }

    public record Class(
            // The obfuscated class name (offset into the string section).
            int obfuscatedNameOffset,

            // The original class name (offset into the string section).
            int originalNameOffset,

            // The file name (offset into the string section).
            int fileNameOffset,

            // The start of the class's member entries (offset into the member section).
            int membersOffset,

            // The number of member entries for this class.
            int numOfMembers,

            // The start of the class's member-by-params entries (offset into the member section).
            int membersByParamsOffset,

            // The number of member-by-params entries for this class.
            int numOfMembersByParams
    ) {
    }

    public record Member(
            // The obfuscated method name (offset into the string section).
            int obfuscatedNameOffset,

            // The start of the range covered by this entry (1-based).
            int startLine,

            // The end of the range covered by this entry (inclusive).
            int endLine,

            // The original class name (offset into the string section).
            int originalClassNameOffset,

            // The original file name (offset into the string section).
            int originalFileNameOffset,

            // The original method name (offset into the string section).
            int originalMethodNameOffset,

            // The original start line (1-based).
            int originalStartLine,

            // The original end line (inclusive).
            int originalEndLine,

            // The entry's parameter string (offset into the strings section).
            int paramsOffset
    ) {
    }
}
