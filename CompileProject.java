import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Small zero-dependency build helper for running FarmLink on Windows/macOS/Linux.
 * It passes Path objects directly to javac, avoiding the Windows-backslash issue
 * in javac @argument files. Requires JDK 17+ (not only a Java Runtime).
 */
public class CompileProject {
    public static void main(String[] args) throws Exception {
        Path sourceDir = Path.of("src", "main", "java");
        Path classesDir = Path.of("build", "classes");
        Path databaseJar = Path.of("lib", "hsqldb.jar");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("ERROR: Java compiler is missing. Install a full JDK 17+, not a JRE.");
            System.exit(1);
        }
        if (!Files.isDirectory(sourceDir) || !Files.isRegularFile(databaseJar)) {
            System.err.println("ERROR: Project files are missing. Extract the entire ZIP first.");
            System.exit(1);
        }
        Files.createDirectories(classesDir);
        Files.createDirectories(Path.of("data"));

        List<Path> sources;
        try (Stream<Path> allFiles = Files.walk(sourceDir)) {
            sources = allFiles
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        if (sources.isEmpty()) {
            System.err.println("ERROR: No Java files found in src/main/java.");
            System.exit(1);
        }

        List<String> options = List.of(
                "--release", "17",
                "-encoding", "UTF-8",
                "-cp", databaseJar.toAbsolutePath().toString(),
                "-d", classesDir.toAbsolutePath().toString());

        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                null, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> sourceFiles = manager.getJavaFileObjectsFromPaths(sources);
            boolean passed = Boolean.TRUE.equals(compiler.getTask(
                    new PrintWriter(System.err, true), manager, null,
                    options, null, sourceFiles).call());
            if (!passed) {
                System.err.println("ERROR: Java compilation failed. Read the errors above.");
                System.exit(1);
            }
        }
        System.out.println("Compiled " + sources.size() + " Java source files successfully.");
    }
}
