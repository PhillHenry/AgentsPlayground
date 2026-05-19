# AgentsPlayground

A Spring Boot + Spring AI playground for **building and evaluating agents**
against a **local Ollama** endpoint.

## What's in the box

- A simple tool-calling agent (`Agent` + `CalculatorTools`) wired to a local
  Ollama chat model via `spring-ai-starter-model-ollama`.
- An evaluation harness (`AgentEvaluationTest`) that runs the agent against a
  small Q&A dataset and scores each response two ways:
  1. A deterministic check that the expected numeric answer appears.
  2. An **LLM-as-judge** relevance score via Spring AI's `RelevancyEvaluator`.

The judge runs on its own `ChatClient` so you can point it at a different
(ideally stronger) model than the one driving the agent — reduces self-grading
bias.

## Prerequisites

- JDK 21
- Maven 3.9+
- A running [Ollama](https://ollama.com/) on `http://localhost:11434`
  (or set `OLLAMA_BASE_URL`).
- A pulled model that supports tool calling, e.g.
  ```bash
  ollama pull llama3.1
  ```
  Other good choices: `qwen2.5`, `mistral`, `llama3.2` (3B for low-RAM boxes).

## Configuration

Defaults live in `src/main/resources/application.yml`. You can override at
runtime with environment variables:

| Var               | Default                  | Meaning                              |
|-------------------|--------------------------|--------------------------------------|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama endpoint                      |
| `AGENT_MODEL`     | `llama3.1`               | Model used by the agent under test   |
| `JUDGE_MODEL`     | `llama3.1`               | Model used by the relevance judge    |

## Running the evaluation

```bash
mvn test
```

Each row in the dataset (see `AgentEvaluationTest`) is reported as a separate
JUnit case, so failures pinpoint exactly which questions the agent got wrong or
which responses the judge deemed irrelevant.

## Wrapping a local LLM in Ollama

If your model is in GGUF format, create a `Modelfile`:

```
FROM ./my-model.gguf
PARAMETER temperature 0.1
```

then:

```bash
ollama create my-model -f Modelfile
ollama run my-model            # smoke-test
AGENT_MODEL=my-model mvn test  # evaluate
```

If your runtime already exposes an OpenAI-compatible API (llama.cpp server,
LM Studio, vLLM), it's simpler to swap the starter to `spring-ai-starter-model-openai`
and point `spring.ai.openai.base-url` at it — no Ollama needed.

## Extending the eval

- Add rows to the `@CsvSource` block in `AgentEvaluationTest` for more cases.
- Add more `@Tool`-annotated methods on `CalculatorTools` (or new components) to
  grow the agent's capabilities.
- Swap `RelevancyEvaluator` for `FactCheckingEvaluator` when you have reference
  documents — pass them via the `EvaluationRequest`'s `dataList`.
