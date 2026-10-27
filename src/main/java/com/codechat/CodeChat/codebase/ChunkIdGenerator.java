package com.codechat.CodeChat.codebase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class ChunkIdGenerator {
    public String generate(
            String projectId,
            String filePath,
            int startLine,
            int endLine,
            String content
    ) {
        String value = projectId + "\n"
                + filePath + "\n"
                + startLine + "\n"
                + endLine + "\n"
                + content;

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable: " + e.getMessage());
        }
    }
}
