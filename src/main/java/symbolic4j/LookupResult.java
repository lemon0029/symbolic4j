package symbolic4j;

import java.util.List;

public record LookupResult(List<Symbol> symbols) {

    /**
     * Symbol information for a specific address.
     *
     * @param fileFullPath          The full path of the source file.
     * @param fileName              The name of the source file, just the file name without the path.
     * @param lineNumber            The line number in the source file.
     * @param functionName          The name of the function as it appears in the binary (mangled).
     * @param functionStartAddr     The starting address of the function in the binary.
     * @param demangledFunctionName The demangled name of the function.
     * @param inlined               Whether the function is inlined.
     * @param language              The programming language of the function:
     *                              Unknown = 0, C = 1, Cpp = 2,
     *                              D = 3, Go = 4, ObjC = 5, ObjCpp = 6,
     *                              Rust = 7, Swift = 8, CSharp = 9, VisualBasic = 10, FSharp = 11
     */
    public record Symbol(String fileName,
                         String fileFullPath,
                         String functionName,
                         String demangledFunctionName,
                         int functionStartAddr,
                         int lineNumber,
                         boolean inlined,
                         int language) {
    }
}
