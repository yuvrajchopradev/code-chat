package com.codechat.CodeChat.config;

import com.codechat.CodeChat.codebase.Chunker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChunkingConfiguration {

    @Bean
    public Chunker chunker(CodeChatProperties properties) {
        return new Chunker(properties.getChunkSize(), properties.getOverlap());
    }
}
