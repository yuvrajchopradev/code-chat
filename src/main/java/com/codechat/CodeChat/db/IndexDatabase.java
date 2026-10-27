package com.codechat.CodeChat.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IndexDatabase implements AutoCloseable {
    private final Connection connection;

    public IndexDatabase(String databasePath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        initialize();
    }

    private void initialize() throws SQLException {
        String createFilesTable = """
                    CREATE TABLE IF NOT EXISTS files (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project_id TEXT NOT NULL,
                        file_path TEXT NOT NULL,
                        file_hash TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        last_modified INTEGER NOT NULL,
                        UNIQUE(project_id, file_path)
                    )
                    """;

        try(Statement statement = connection.createStatement()) {
            statement.execute(createFilesTable);
        }

        String createChunksTable = """
                CREATE TABLE IF NOT EXISTS chunks (
                    id TEXT PRIMARY KEY,
                    project_id TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    start_line INTEGER NOT NULL,
                    end_line INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    file_hash TEXT NOT NULL,
                    UNIQUE(project_id, file_path, start_line, end_line)
                )
                """;
        try(Statement statement = connection.createStatement()) {
            statement.execute(createChunksTable);
        }
    }

    public void saveFileMetaData(
            String projectId,
            String filePath,
            String fileHash,
            long fileSize,
            long lastModified
    ) throws SQLException {
        String sql = """
                INSERT INTO files (project_id, file_path, file_hash, file_size, last_modified)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (project_id, file_path)
                DO UPDATE SET
                file_hash = excluded.file_hash,
                file_size = excluded.file_size,
                last_modified = excluded.last_modified
                """;

        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId);
            statement.setString(2, filePath);
            statement.setString(3, fileHash);
            statement.setLong(4, fileSize);
            statement.setLong(5, lastModified);

            statement.executeUpdate();
        }
    }

    public String getStoredHash(String projectId, String filePath) throws SQLException {
        String sql = """
                SELECT file_hash
                FROM files
                WHERE project_id = ? AND file_path = ?
                """;

        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId);
            statement.setString(2, filePath);

            try(ResultSet resultSet = statement.executeQuery()) {
                if(resultSet.next()) {
                    return resultSet.getString("file_hash");
                }

                return null;
            }
        }
    }

    public void deleteMissingFiles(String projectId, Set<String> currentFilePaths) throws SQLException {
        String selectSql = """
                SELECT file_path
                FROM files
                WHERE project_id = ?
                """;

        try(PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setString(1, projectId);

            try(ResultSet resultSet = select.executeQuery()) {
                List<String> pathsToDelete = new ArrayList<>();
                while (resultSet.next()) {
                    String storedPath = resultSet.getString("file_path");

                    if(!currentFilePaths.contains(storedPath)) {
                        pathsToDelete.add(storedPath);
                    }
                }

                String deleteFilesSql = """
                    DELETE FROM files
                    where project_id = ? AND file_path = ?
                    """;
                String deleteChunksSql = """
                    DELETE FROM chunks
                    where project_id = ? AND file_path = ?
                    """;

                try(
                        PreparedStatement deleteChunks = connection.prepareStatement(deleteChunksSql);
                        PreparedStatement deleteFiles = connection.prepareStatement(deleteFilesSql);
                        ) {
                    for(String path: pathsToDelete) {
                        deleteChunks.setString(1, projectId);
                        deleteChunks.setString(2, path);
                        deleteChunks.addBatch();

                        deleteFiles.setString(1, projectId);
                        deleteFiles.setString(2, path);
                        deleteFiles.addBatch();
                    }

                    deleteChunks.executeBatch();
                    deleteFiles.executeBatch();
                    if(!pathsToDelete.isEmpty()) {
                        System.out.println("Removed " + pathsToDelete.size() + " deleted file records.");
                    }
                }
            }
        }
    }

    public void saveChunk(
            String id,
            String projectId,
            String filePath,
            int startLine,
            int endLine,
            String content,
            String fileHash
    ) throws SQLException {
        String sql = """
                INSERT INTO chunks (id, project_id, file_path, start_line, end_line, content, file_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                content = excluded.content,
                file_hash = excluded.file_hash,
                start_line = excluded.start_line,
                end_line = excluded.end_line
                """;
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, projectId);
            statement.setString(3, filePath);
            statement.setInt(4, startLine);
            statement.setInt(5, endLine);
            statement.setString(6, content);
            statement.setString(7, fileHash);

            statement.executeUpdate();
        }
    }

    public void deleteChunksForFile(String projectId, String filePath) throws SQLException {
        String sql = """
                DELETE FROM chunks
                WHERE project_id = ? AND file_path = ?
                """;
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId);
            statement.setString(2, filePath);
            statement.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }
}
