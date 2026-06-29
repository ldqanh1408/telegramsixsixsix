package com.lede.telegrambots;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {

    @Test
    void domainDoesNotDependOnOuterLayersOrFrameworks() throws IOException {
        assertNoForbiddenImports("telegrambots-domain", List.of(
                "org.springframework.",
                "tools.jackson.",
                "com.fasterxml.jackson.",
                "org.springframework.data.",
                "com.lede.telegrambots.application.",
                "com.lede.telegrambots.infrastructure.",
                "com.lede.telegrambots.telegram.",
                "com.lede.telegrambots.github.",
                "com.lede.telegrambots.admin."
        ));
    }

    @Test
    void applicationDoesNotDependOnAdaptersOrFrameworks() throws IOException {
        assertNoForbiddenImports("telegrambots-application", List.of(
                "org.springframework.",
                "tools.jackson.",
                "com.fasterxml.jackson.",
                "org.springframework.data.",
                "com.lede.telegrambots.infrastructure.",
                "com.lede.telegrambots.telegram.",
                "com.lede.telegrambots.github.",
                "com.lede.telegrambots.admin."
        ));
    }

    @Test
    void webAndInfrastructureDoNotDependOnEachOther() throws IOException {
        assertNoForbiddenImports("telegrambots-web", List.of(
                "com.lede.telegrambots.infrastructure."
        ));
        assertNoForbiddenImports("telegrambots-infrastructure", List.of(
                "com.lede.telegrambots.telegram.",
                "com.lede.telegrambots.github.",
                "com.lede.telegrambots.admin."
        ));
    }

    private static void assertNoForbiddenImports(String module, List<String> forbiddenPrefixes) throws IOException {
        Path sourceRoot = projectRoot().resolve(module).resolve("src/main/java");
        List<String> violations;

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> forbiddenImports(path, forbiddenPrefixes).stream())
                    .toList();
        }

        assertTrue(violations.isEmpty(), () -> "Architecture boundary violations:\n" + String.join("\n", violations));
    }

    private static List<String> forbiddenImports(Path javaFile, List<String> forbiddenPrefixes) {
        try {
            return Files.readAllLines(javaFile).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .filter(line -> forbiddenPrefixes.stream().anyMatch(prefix -> line.startsWith("import " + prefix)))
                    .map(line -> projectRoot().relativize(javaFile) + " -> " + line)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + javaFile, e);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("telegrambots-domain"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate project root from " + System.getProperty("user.dir"));
        }
        return current;
    }
}
