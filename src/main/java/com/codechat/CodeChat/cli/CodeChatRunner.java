package com.codechat.CodeChat.cli;

import com.codechat.CodeChat.codebase.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@Component
@Profile("!test")
public class CodeChatRunner implements CommandLineRunner {
    private final CodebaseIndexingService indexingService;

    public CodeChatRunner(CodebaseIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println();
        System.out.println("CodeChat started.");
        System.out.println("=============");
        System.out.println();

        Scanner sc = new Scanner(System.in);
        System.out.print("CodeBase Path: ");
        String input = sc.nextLine().trim();

        Path codebasePath = Paths.get(input).toAbsolutePath().normalize();
        if(!Files.exists(codebasePath)) {
            System.out.println("Error: Path does not exist.");
            return;
        }
        if(!Files.isDirectory(codebasePath)) {
            System.out.println("Error: Path is not a directory.");
            return;
        }

        System.out.println();
        System.out.println("Scanning codebase...");
        try {
            CodebaseIndexingService.IndexingResult result = indexingService.index(codebasePath);

            System.out.println();
            System.out.println("Chunks saved to SQLite: " + result.savedChunks());
            System.out.println("File scan summary:");
            System.out.println("New:       " + result.newFiles());
            System.out.println("Changed:   " + result.changedFiles());
            System.out.println("Unchanged: " + result.unchangedFiles());
        }
        catch (Exception e) {
            System.out.println("Error saving file metadata: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Selected: " + codebasePath);
    }
}
