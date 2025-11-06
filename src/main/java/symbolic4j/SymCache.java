package symbolic4j;


import com.google.common.primitives.Ints;
import kotlin.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public record SymCache(Header header,
                       Files files,
                       Functions functions,
                       SourceLocations sourceLocations,
                       Ranges ranges, StringTable stringTable) {

    private static final int HEADER_SIZE = 80;
    private static final int FILE_SIZE = 12;
    private static final int FUNCTION_SIZE = 16;
    private static final int SOURCE_LOCATION_SIZE = 16;
    private static final int RANGE_SIZE = 4;

    private static final byte[] SYMCACHE_MAGIC = "SYMC".getBytes();

    /**
     * The latest version of the file format.
     */
    private static final int SYMCACHE_VERSION = 8;

    public static SymCache open(Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            Pair<Header, ByteBuffer> headerPair = readHeader(buffer);
            Header header = headerPair.getFirst();

            if (header.magic != Ints.fromByteArray(SYMCACHE_MAGIC)) {
                throw new IllegalStateException("Invalid SymCache file, magic number mismatch");
            }

            if (header.version != SYMCACHE_VERSION) {
                throw new IllegalStateException("Invalid SymCache file, version mismatch");
            }

            Pair<Files, ByteBuffer> filesPair = readFiles(headerPair.getSecond(), header.numOfFiles);
            Pair<Functions, ByteBuffer> functionsPair = readFunctions(filesPair.getSecond(), header.numOfFunctions);
            Pair<SourceLocations, ByteBuffer> sourceLocationsPair = readSourceLocations(functionsPair.getSecond(), header.numOfSourceLocations);
            Pair<Ranges, ByteBuffer> rangesPair = readRanges(sourceLocationsPair.getSecond(), header.numOfRanges);

            Files files = filesPair.getFirst();
            Functions functions = functionsPair.getFirst();
            SourceLocations sourceLocations = sourceLocationsPair.getFirst();
            Ranges ranges = rangesPair.getFirst();

            ByteBuffer stBuffer = BufferUtils.alignBuffer(rangesPair.getSecond(), 8);
            StringTable stringTable = new StringTable(stBuffer);

            return new SymCache(header, files, functions, sourceLocations, ranges, stringTable);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Lookup the given address in the SymCache.
     *
     * @param addr The address to lookup.
     * @return The lookup result, or null if not found.
     */
    public LookupResult lookup(long addr) {
        // uint32 address space
        if (addr < 0 || addr > 0xFFFFFFFFL) {
            return null;
        }

        int rangeIdx = searchRange(addr);

        // addr is less than the first range
        if (rangeIdx == -1) {
            return null;
        }

        int sourceLocationIdx = rangeIdx + (header.numOfSourceLocations - header.numOfRanges);

        SourceLocation sourceLocation = sourceLocations.get(sourceLocationIdx);

        if (sourceLocation == null) {
            return null;
        }

        List<SourceLocation> lookupSourceLocations = new ArrayList<>();
        lookupSourceLocations.add(sourceLocation);

        Set<SourceLocation> inlinedLocations = new HashSet<>();

        // Traverse inlined locations
        int inlineIdx = sourceLocation.inlinedIntoIndex();
        while (inlineIdx != -1) {
            SourceLocation inlineLocation = sourceLocations.get(inlineIdx);

            if (inlineLocation == null) {
                break;
            }

            inlinedLocations.add(inlineLocation);

            lookupSourceLocations.add(inlineLocation);
            inlineIdx = inlineLocation.inlinedIntoIndex();
        }

        List<LookupResult.Symbol> symbols = new ArrayList<>();

        for (SourceLocation sl : lookupSourceLocations) {
            File file = files.get(sl.fileIndex);
            Function function = functions.get(sl.functionIndex);

            String fileName = "";
            String fileFullPath = "";
            if (file != null) {
                String fileDir = stringTable.get(file.directoryOffset());
                String fileCompDir = stringTable.get(file.compFileOffset());

                fileName = stringTable.get(file.nameOffset());

                if (fileName.startsWith("<") && fileName.endsWith(">")) {
                    // Handle special file names like <built-in>, <compiler-generated> etc.
                    fileFullPath = fileName;
                } else {
                    fileFullPath = Path.of(fileCompDir)
                            .resolve(fileDir)
                            .resolve(fileName)
                            .normalize()
                            .toString();
                }
            }

            String functionName = "";
            String demangledFunctionName = "";
            int functionStartAddr = -1;
            int language = -1;
            if (function != null) {
                functionName = stringTable.get(function.nameOffset());
                demangledFunctionName = functionName;
                functionStartAddr = function.entryPc;
                language = function.lang;
            }

            if (!functionName.isBlank()) {
                // TODO 这里需要判断是否需要做 demangle
            }

            boolean inlined = inlinedLocations.contains(sl);

            LookupResult.Symbol symbol = new LookupResult.Symbol(
                    fileName,
                    fileFullPath,
                    functionName,
                    demangledFunctionName,
                    functionStartAddr,
                    sl.lineNumber,
                    inlined,
                    language
            );

            symbols.add(symbol);
        }

        return new LookupResult(symbols);
    }

    private int searchRange(long addr) {
        int left = 0;
        int right = header.numOfRanges() - 1;

        if (addr < ranges.get(left).startAddr) {
            return -1;
        }

        if (addr >= ranges.get(right).startAddr) {
            return right;
        }

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Range range = ranges.get(mid);

            if (addr == range.startAddr) {
                return mid;
            } else if (addr < range.startAddr) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        // At the end of the loop, left is the first index with range.startAddr > addr
        return left - 1;
    }

    private static Pair<Ranges, ByteBuffer> readRanges(ByteBuffer second, int numOfRanges) {
        int expectedSize = numOfRanges * RANGE_SIZE;

        if (second.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid SymCache file, insufficient data for ranges");
        }

        ByteBuffer aligned = BufferUtils.alignBuffer(second, 8);
        Pair<ByteBuffer, ByteBuffer> pair = BufferUtils.splitBuffer(aligned, expectedSize);

        Ranges ranges = new Ranges(pair.getFirst());

        return new Pair<>(ranges, pair.getSecond());
    }

    private static Pair<SourceLocations, ByteBuffer> readSourceLocations(ByteBuffer second, int numOfSourceLocations) {
        int expectedSize = numOfSourceLocations * SOURCE_LOCATION_SIZE;

        if (second.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid SymCache file, insufficient data for source locations");
        }

        ByteBuffer aligned = BufferUtils.alignBuffer(second, 8);
        Pair<ByteBuffer, ByteBuffer> pair = BufferUtils.splitBuffer(aligned, expectedSize);

        SourceLocations sourceLocations = new SourceLocations(pair.getFirst());

        return new Pair<>(sourceLocations, pair.getSecond());
    }

    private static Pair<Functions, ByteBuffer> readFunctions(ByteBuffer second, int numOfFunctions) {
        int expectedSize = numOfFunctions * FUNCTION_SIZE;

        if (second.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid SymCache file, insufficient data for functions");
        }

        ByteBuffer aligned = BufferUtils.alignBuffer(second, 8);
        Pair<ByteBuffer, ByteBuffer> pair = BufferUtils.splitBuffer(aligned, expectedSize);

        Functions functions = new Functions(pair.getFirst());

        return new Pair<>(functions, pair.getSecond());
    }

    private static Pair<Files, ByteBuffer> readFiles(ByteBuffer buffer, int numOfFiles) {
        int expectedSize = numOfFiles * FILE_SIZE;

        if (buffer.remaining() < expectedSize) {
            throw new IllegalStateException("Invalid SymCache file, insufficient data for files");
        }

        ByteBuffer aligned = BufferUtils.alignBuffer(buffer, 8);
        Pair<ByteBuffer, ByteBuffer> pair = BufferUtils.splitBuffer(aligned, expectedSize);

        Files files = new Files(pair.getFirst());

        return new Pair<>(files, pair.getSecond());
    }

    private static Pair<Header, ByteBuffer> readHeader(MappedByteBuffer buffer) {
        Pair<ByteBuffer, ByteBuffer> pair = BufferUtils.splitBuffer(buffer, HEADER_SIZE);

        ByteBuffer current = pair.getFirst();
        current.order(ByteOrder.BIG_ENDIAN);

        int magic = current.getInt();

        current.order(ByteOrder.LITTLE_ENDIAN);
        int version = current.getInt();

        byte[] debugIdBytes = new byte[32];
        current.get(debugIdBytes);
        String debugId = HexFormat.of()
                .withLowerCase()
                .formatHex(debugIdBytes)
                .substring(0, 32);

        int archCode = current.getInt();
        int numOfFiles = current.getInt();
        int numOfFunctions = current.getInt();
        int numOfSourceLocations = current.getInt();
        int numOfRanges = current.getInt();
        int stringBytes = current.getInt();
        byte[] reserved = new byte[16];
        current.get(reserved);

        Header header = new Header(
                magic,
                version,
                debugId,
                archCode,
                numOfFiles,
                numOfFunctions,
                numOfSourceLocations,
                numOfRanges,
                stringBytes,
                reserved
        );

        return new Pair<>(header, pair.getSecond());
    }

    public record Header(
            int magic,
            int version,
            String debugId,
            int archCode,
            int numOfFiles,
            int numOfFunctions,
            int numOfSourceLocations,
            int numOfRanges,
            int stringBytes,
            byte[] reserved
    ) {
    }

    public static class Files extends Pods<File> {

        public Files(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected File get(int index) {

            if (index == -1) {
                return null;
            }

            ByteBuffer buffer = buffer(index * FILE_SIZE);

            if (buffer.remaining() < FILE_SIZE) {
                return null;
            }

            return new File(buffer.getInt(), buffer.getInt(), buffer.getInt());
        }
    }

    public static class Functions extends Pods<Function> {

        public Functions(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected Function get(int index) {
            ByteBuffer buffer = buffer(index * FUNCTION_SIZE);

            if (buffer.remaining() < FUNCTION_SIZE) {
                return null;
            }

            return new Function(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
        }
    }

    public static class SourceLocations extends Pods<SourceLocation> {

        public SourceLocations(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected SourceLocation get(int index) {

            if (index == -1) {
                return null;
            }

            ByteBuffer buffer = buffer(index * SOURCE_LOCATION_SIZE);

            if (buffer.remaining() < SOURCE_LOCATION_SIZE) {
                return null;
            }

            return new SourceLocation(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
        }
    }

    public static class Ranges extends Pods<Range> {

        public Ranges(ByteBuffer buffer) {
            super(buffer);
        }

        @Override
        protected Range get(int index) {
            ByteBuffer buffer = buffer(index * RANGE_SIZE);

            if (buffer.remaining() < RANGE_SIZE) {
                return null;
            }

            int addr = buffer.getInt();
            return new Range(Integer.toUnsignedLong(addr));
        }
    }

    /**
     * Serialized Function metadata in the SymCache.
     *
     * @param nameOffset    The functions name (reference to a [`String`]).
     * @param compDirOffset The compilation directory (reference to a [`String`]).
     *                      This is retained for binary compatibility; all path information is contained in [`File`].
     * @param entryPc       The first address covered by this function.
     * @param lang          The language of the function.
     */
    public record Function(
            int nameOffset,
            int compDirOffset,
            int entryPc,
            int lang
    ) {
    }

    /**
     * Serialized File in the SymCache.
     *
     * @param compFileOffset  The optional compilation directory prefix (reference to a [`String`]).
     * @param directoryOffset The optional directory prefix (reference to a [`String`]).
     * @param nameOffset      The file path (reference to a [`String`]).
     */
    public record File(
            int compFileOffset,
            int directoryOffset,
            int nameOffset
    ) {
    }

    /**
     * A location in a source file, comprising a file, a line, a function, and
     * the index of the source location this was inlined into, if any.
     * <br/>
     * Note that each time a function is inlined, as well as the non-inlined
     * version of the function, is represented by a distinct `SourceLocation`.
     * These `SourceLocation`s will all point to the same file, line, and function,
     * but have different inline information.
     *
     * @param fileIndex        The optional source file (reference to a [`File`]).
     * @param lineNumber       The line number.
     * @param functionIndex    The function (reference to a [`Function`]).
     * @param inlinedIntoIndex The caller source location in case this location was inlined (reference to another [`SourceLocation`]).
     */
    public record SourceLocation(
            int fileIndex,
            int lineNumber,
            int functionIndex,
            int inlinedIntoIndex
    ) {
    }

    /**
     * A representation of a code range in the SymCache.
     * <br/>
     * We only save the start address, the end is implicitly given by the next range's start.
     *
     * @param startAddr The start address of the range.
     */
    public record Range(long startAddr) {
    }
}

