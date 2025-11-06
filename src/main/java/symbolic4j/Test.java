package symbolic4j;

import java.nio.file.Path;
import java.util.Optional;

public class Test {

    public static void main(String[] args) {
        Path path = Path.of("external/resources/Electron.app.dSYM/Contents/Resources/DWARF/Electron.sym");

        SymCache symCache = SymCache.open(path);

        for (int i = 0; i < symCache.header().numOfRanges(); i++) {
            SymCache.Range range = symCache.ranges().get(i);

            SymCache.SourceLocation sourceLocation = symCache.sourceLocations().get(i);

            int lineNumber = sourceLocation.lineNumber();
            int fileIndex = sourceLocation.fileIndex();
            int functionIndex = sourceLocation.functionIndex();

            SymCache.File file = symCache.files().get(fileIndex);
            SymCache.Function function = symCache.functions().get(functionIndex);

            String functionName = symCache.stringTable().get(function.nameOffset());
            String fileName = Optional.ofNullable(file)
                    .map(it -> symCache.stringTable().get(it.nameOffset()))
                    .orElse("");

            System.out.printf("0x%016x -> %s (%s:%d)%n", range.startAddr(), functionName, fileName, lineNumber);
        }
    }
}
