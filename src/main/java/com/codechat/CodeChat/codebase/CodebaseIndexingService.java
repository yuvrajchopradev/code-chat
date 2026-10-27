package com.codechat.CodeChat.codebase;

import com.codechat.CodeChat.db.IndexDatabase;
import com.codechat.CodeChat.domain.CodeChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CodebaseIndexingService {
    private final FileScanner fileScanner;
    private final HashService hashService;
    private final Chunker chunker;

    public CodebaseIndexingService(FileScanner fileScanner, HashService hashService, Chunker chunker) {
        this.fileScanner = fileScanner;
        this.hashService = hashService;
        this.chunker = chunker;
    }

    public IndexingResult index(Path codebasePath) throws Exception {
        List<Path> files = fileScanner.scan(codebasePath);

        Path codeChatDirectory = Path.of(
                System.getProperty("user.home"),
                ".codechat"
        );
        Files.createDirectories(codeChatDirectory);

        Path databasePath = codeChatDirectory.resolve("index.db");
        String projectId = codebasePath.toAbsolutePath().normalize().toString();

        int newFiles = 0, changedFiles = 0, unchangedFiles = 0, savedChunks = 0;
        try(IndexDatabase database = new IndexDatabase(databasePath.toString())) {
            for(Path file: files) {
                String relativePath = codebasePath.relativize(file).toString();
                String currentHash = hashService.calculateHash(file);
                String storedHash = database.getStoredHash(projectId, relativePath);

                boolean isNew = storedHash == null;
                boolean isChanged = !isNew && !storedHash.equals(currentHash);
                if(isNew) {
                    newFiles++;
                    System.out.println("[NEW] " + relativePath);
                }
                else if(isChanged) {
                    changedFiles++;
                    System.out.println("[CHANGED] " + relativePath);
                }
                else {
                    unchangedFiles++;
                    System.out.println("[UNCHANGED] " + relativePath);
                    continue;
                }

                // update metadata for new or changed files
                database.saveFileMetaData(
                        projectId,
                        relativePath,
                        currentHash,
                        Files.size(file),
                        Files.getLastModifiedTime(file).toMillis()
                );

                // remove old chunks
                if(isChanged) {
                    database.deleteChunksForFile(projectId, relativePath);
                }

                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    List<CodeChunk> fileChunks = chunker.chunkFile(relativePath, content);
                    ChunkIdGenerator idGenerator = new ChunkIdGenerator();

                    for(CodeChunk chunk: fileChunks) {
                        String chunkId = idGenerator.generate(
                                projectId,
                                chunk.filePath(),
                                chunk.startLine(),
                                chunk.endLine(),
                                chunk.content()
                        );

                        database.saveChunk(
                                chunkId,
                                projectId,
                                chunk.filePath(),
                                chunk.startLine(),
                                chunk.endLine(),
                                chunk.content(),
                                currentHash
                        );
                        savedChunks++;
                    }
                }
                catch (IOException e) {
                    System.out.println("Could not read " + relativePath + ": " + e.getMessage());
                }
            }

            Set<String> currentFilePath = files.stream()
                    .map(file -> codebasePath.relativize(file).toString())
                    .collect(Collectors.toSet());
            database.deleteMissingFiles(projectId, currentFilePath);
        }

        return new IndexingResult(newFiles, changedFiles, unchangedFiles, savedChunks);
    }

    public record IndexingResult(
            int newFiles,
            int changedFiles,
            int unchangedFiles,
            int savedChunks
    ) {}
}
