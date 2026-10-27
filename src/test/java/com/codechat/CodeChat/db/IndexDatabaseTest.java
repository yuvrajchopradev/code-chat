package com.codechat.CodeChat.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndexDatabaseTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndRetrieveFileHash() throws Exception {
        Path databasePath = tempDir.resolve("test.db");

        try (IndexDatabase database =
                     new IndexDatabase(databasePath.toString())) {

            database.saveFileMetaData(
                    "project-1",
                    "src/Main.java",
                    "hash123",
                    500,
                    1000
            );

            String storedHash =
                    database.getStoredHash("project-1", "src/Main.java");

            assertEquals("hash123", storedHash);
        }
    }

    @Test
    void shouldReturnNullForUnknownFile() throws Exception {
        Path databasePath = tempDir.resolve("test.db");

        try (IndexDatabase database =
                     new IndexDatabase(databasePath.toString())) {

            assertNull(
                    database.getStoredHash("project-1", "missing.java")
            );
        }
    }

    @Test
    void shouldDeleteChunksForFile() throws Exception {
        Path databasePath = tempDir.resolve("test.db");

        try (IndexDatabase database =
                     new IndexDatabase(databasePath.toString())) {

            database.saveChunk(
                    "chunk-1",
                    "project-1",
                    "src/Main.java",
                    1,
                    10,
                    "class Main {}",
                    "hash123"
            );

            database.deleteChunksForFile(
                    "project-1",
                    "src/Main.java"
            );

            String sql = """
                    SELECT COUNT(*)
                    FROM chunks
                    WHERE project_id = ? AND file_path = ?
                    """;

            try (PreparedStatement statement =
                         database.getConnection().prepareStatement(sql)) {

                statement.setString(1, "project-1");
                statement.setString(2, "src/Main.java");

                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(0, resultSet.getInt(1));
                }
            }
        }
    }

    @Test
    void shouldDeleteMetadataAndChunksForMissingFiles() throws Exception {
        Path databasePath = tempDir.resolve("test.db");

        try (IndexDatabase database =
                     new IndexDatabase(databasePath.toString())) {

            database.saveFileMetaData(
                    "project-1",
                    "src/Deleted.java",
                    "hash123",
                    100,
                    1000
            );

            database.saveChunk(
                    "chunk-1",
                    "project-1",
                    "src/Deleted.java",
                    1,
                    5,
                    "class Deleted {}",
                    "hash123"
            );

            database.deleteMissingFiles("project-1", Set.of());

            assertNull(
                    database.getStoredHash("project-1", "src/Deleted.java")
            );

            String sql = """
                    SELECT COUNT(*)
                    FROM chunks
                    WHERE project_id = ? AND file_path = ?
                    """;

            try (PreparedStatement statement =
                         database.getConnection().prepareStatement(sql)) {

                statement.setString(1, "project-1");
                statement.setString(2, "src/Deleted.java");

                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(0, resultSet.getInt(1));
                }
            }
        }
    }
}
