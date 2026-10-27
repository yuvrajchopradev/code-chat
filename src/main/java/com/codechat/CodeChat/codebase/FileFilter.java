package com.codechat.CodeChat.codebase;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Set;

@Component
public class FileFilter {
    private final static Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            ".idea",
            "target",
            "node_modules",
            "build",
            "dist",
            ".vscode"
    );

    private final static Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java",
            ".py",
            ".js",
            ".ts",
            ".jsx",
            ".tsx",
            ".cpp",
            ".c",
            ".h",
            ".hpp",
            ".cs",
            ".go",
            ".rs",
            ".kt",
            ".kts",
            ".swift",
            ".php",
            ".rb",
            ".html",
            ".css",
            ".scss",
            ".xml",
            ".json",
            ".yaml",
            ".yml",
            ".properties",
            ".md",
            ".txt"
    );

    public boolean shouldInclude(Path rootPath, Path file) {
        Path relativePath = rootPath.relativize(file);
        for(Path part: relativePath) {
            if(IGNORED_DIRECTORIES.contains(part.toString())) {
                return false;
            }
        }

        String fileName = file.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if(dotIndex == -1) {
            return false;
        }

        String extension = fileName.substring(dotIndex);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}
