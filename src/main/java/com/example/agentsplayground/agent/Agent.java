package com.example.agentsplayground.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Component;

@Component
public class Agent {

    private static final String SYSTEM_PROMPT = """
            You are a careful, concise assistant.
            When a question involves arithmetic, you MUST call the provided
            calculator tools rather than computing the answer in your head.
            Give the final answer as a short sentence including the numeric result.
            """;

    private final ChatClient chatClient;

    public Agent(ChatClient.Builder chatClientBuilder, CalculatorTools calculatorTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(calculatorTools)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
