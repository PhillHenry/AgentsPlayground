# AgentsPlayground

A Spring Boot + Spring AI playground for **building and evaluating agents**
against a **local llama.cpp server** (via its OpenAI-compatible API).

## What's in the box

- A simple tool-calling agent (`Agent` + `CalculatorTools`) wired to a local
  LLM through `spring-ai-starter-model-openai`, pointed at llama.cpp's
  OpenAI-compatible endpoint.
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
- A running [llama.cpp](https://github.com/ggml-org/llama.cpp) server on
  `http://localhost:8080` (or set `LLAMACPP_BASE_URL`).
- A GGUF model that supports tool calling (e.g. a Qwen2.5, Llama 3.1, or
  Mistral instruct GGUF). **Start the server with `--jinja`** so the model's
  chat template drives OpenAI-style tool calling:
  ```bash
  ./llama-server -m ./qwen2.5-7b-instruct.gguf --jinja -c 8192 --port 8080
  ```
  or for example
- ```bash
./llama-server -m /home/henryp/Downloads/Llama-3.2-1B-Instruct-F16.gguf -c 4096 --port 8080 --jinja -c 8192
```

The agent relies on tool calling. If the loaded model/template doesn't support
it, the `@Tool` methods won't be invoked and the arithmetic checks will fail.

## Configuration

Defaults live in `src/main/resources/application.yml`. You can override at
runtime with environment variables:

| Var                | Default                 | Meaning                                            |
|--------------------|-------------------------|----------------------------------------------------|
| `LLAMACPP_BASE_URL`| `http://localhost:8080` | llama.cpp server base URL (`/v1/...` is appended)  |
| `OPENAI_API_KEY`   | `not-needed`            | Ignored by llama.cpp, but must be non-blank        |
| `AGENT_MODEL`      | `local-model`           | Sent as the model field; llama.cpp ignores it      |
| `JUDGE_MODEL`      | `local-model`           | Model field for the relevance judge                |

Since llama.cpp serves a single loaded model and ignores the `model` field, the
agent and judge hit the same model by default. For an independent judge, run a
second llama.cpp server on another port and build the judge's `ChatClient`
against it in `AgentEvaluationTest`.

## Running the evaluation

```bash
mvn test
```

Each row in the dataset (see `AgentEvaluationTest`) is reported as a separate
JUnit case, so failures pinpoint exactly which questions the agent got wrong or
which responses the judge deemed irrelevant.

## How the wiring works

llama.cpp's `llama-server` exposes an OpenAI-compatible REST API at
`/v1/chat/completions`. Spring AI's OpenAI client
(`spring-ai-starter-model-openai`) talks to exactly that shape, so we just point
`spring.ai.openai.base-url` at the llama.cpp server instead of api.openai.com.
The `Agent` and evaluators are written against Spring AI's provider-agnostic
`ChatClient`, so nothing in the agent code is OpenAI- or llama.cpp-specific.

To use a different OpenAI-compatible runtime (LM Studio, vLLM, text-generation-webui),
just change `LLAMACPP_BASE_URL` to its endpoint.

## Extending the eval

- Add rows to the `@CsvSource` block in `AgentEvaluationTest` for more cases.
- Add more `@Tool`-annotated methods on `CalculatorTools` (or new components) to
  grow the agent's capabilities.
- Swap `RelevancyEvaluator` for `FactCheckingEvaluator` when you have reference
  documents — pass them via the `EvaluationRequest`'s `dataList`.
