package com.example.agentsplayground.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluates the agent end-to-end against a small Q&A dataset.
 *
 * <p>Each row checks two things:
 * <ol>
 *   <li>A deterministic check that the expected fact appears in the answer.</li>
 *   <li>An LLM-as-judge relevance score via {@link RelevancyEvaluator}.</li>
 * </ol>
 *
 * <p>Requires a running Ollama (see README).
 */
@SpringBootTest
class AgentEvaluationTest {

    @Autowired
    private Agent agent;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Value("${agent.evaluation.judge-model}")
    private String judgeModel;

    private RelevancyEvaluator relevancyEvaluator;

    @BeforeEach
    void setUpJudge() {
        // Build a separate ChatClient for the judge so we can pin a (potentially
        // stronger) model that's different from the one driving the agent.
        ChatClient.Builder judgeChatClientBuilder = ChatClient.builder(ollamaChatModel)
                .defaultOptions(OllamaOptions.builder()
                        .model(judgeModel)
                        .temperature(0.0)
                        .build());

        this.relevancyEvaluator = RelevancyEvaluator.builder()
                .chatClientBuilder(judgeChatClientBuilder)
                .build();
    }

    @ParameterizedTest(name = "[{index}] {0} -> contains \"{1}\"")
    @CsvSource(textBlock = """
            'What is 12 multiplied by 7?'          , '84'
            'Add 25 and 17, what do you get?'      , '42'
            'Divide 100 by 4.'                     , '25'
            'What is 9 times 9?'                   , '81'
            'Subtract 50 from 123.'                , '73'
            """)
    void agent_produces_correct_and_relevant_answers(String question, String expectedFact) {
        String answer = agent.ask(question);

        assertThat(answer)
                .as("Expected the answer to contain the correct numeric result. Got: %s", answer)
                .contains(expectedFact);

        EvaluationRequest evaluationRequest = new EvaluationRequest(
                question,
                List.of(),
                answer);

        EvaluationResponse evaluation = relevancyEvaluator.evaluate(evaluationRequest);

        assertThat(evaluation.isPass())
                .as("Judge ruled the response not relevant. Answer: %s | Feedback: %s",
                        answer, evaluation.getFeedback())
                .isTrue();
    }
}
