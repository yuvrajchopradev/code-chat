package com.codechat.CodeChat.codebase;

import com.codechat.CodeChat.domain.CodeChunk;

import java.util.ArrayList;
import java.util.List;

public class Chunker {
    private final int chunkSize;
    private final int overlap;

    public Chunker(int chunkSize, int overlap) {
        if(chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive.");
        }
        if(overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Overlap must be non-negative and smaller than chunk size.");
        }

        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<CodeChunk> chunkFile(String filePath, String content) {
        String[] lines = content.split("\\R");
        List<CodeChunk> chunks = new ArrayList<>();

        int step = chunkSize - overlap;
        for(int start=0; start<lines.length; start += step) {
            int end = Math.min(start + chunkSize, lines.length);

            StringBuilder chunkContent = new StringBuilder();
            for(int i=start; i<end; i++) {
                if(i > start) {
                    chunkContent.append(System.lineSeparator());
                }
                chunkContent.append(lines[i]);
            }

            chunks.add(new CodeChunk(filePath, start+1, end, chunkContent.toString()));

            if(end == lines.length) {
                break;
            }
        }

        return chunks;
    }
}
