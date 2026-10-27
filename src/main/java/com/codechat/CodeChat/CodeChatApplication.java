package com.codechat.CodeChat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.codechat.CodeChat.config.CodeChatProperties;

@SpringBootApplication
@EnableConfigurationProperties(CodeChatProperties.class)
public class CodeChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeChatApplication.class, args);
	}

}
