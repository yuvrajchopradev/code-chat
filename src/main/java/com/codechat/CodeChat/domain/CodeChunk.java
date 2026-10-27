package com.codechat.CodeChat.domain;

public record CodeChunk(
    String filePath,
    int startLine,
    int endLine,
    String content
) {}
