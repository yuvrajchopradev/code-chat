package com.codechat.CodeChat.codebase;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileScanner {
    private final FileFilter fileFilter;

    public FileScanner(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public List<Path> scan(Path rootPath) throws IOException {
        try(Stream<Path> paths = Files.walk(rootPath)) {
            return  paths.
                    filter(Files::isRegularFile).
                    filter(file -> fileFilter.shouldInclude(rootPath, file)).
                    toList();
        }
    }
}
