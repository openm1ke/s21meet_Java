# Graphify Local Workflow

Project default is local-only Graphify.

## Routine Update

Run after supported source changes:

```bash
scripts/graphify_local_update.sh
```

This does:

```bash
graphify update .
graphify cluster-only . --no-label
```

The script unsets cloud LLM environment variables for the child process, so it does not use Gemini/OpenAI/Claude/DeepSeek/Kimi even if keys exist in the shell.

## Local Labels

Labels are optional human-readable community names. They make `GRAPH_REPORT.md` and `graphify query` easier for agents to scan, but they are not required for the AST graph itself.

Generate labels only with a local Ollama model:

```bash
brew install ollama
scripts/graphify_local_label.sh
```

The script starts `ollama serve` temporarily when needed, pulls the configured model if it is missing, runs Graphify, and stops only the Ollama process it started. If Ollama is already running, the script reuses it and leaves it running.

By default it refreshes all community labels. For a faster run that only fills missing labels:

```bash
GRAPHIFY_LABEL_MISSING_ONLY=1 scripts/graphify_local_label.sh
```

Use another installed local model:

```bash
GRAPHIFY_LOCAL_LABEL_MODEL=<model> scripts/graphify_local_label.sh
```

## Local Semantic Refresh

Use this rarely, only when the knowledge base needs deeper documentation/config semantic links or wiki regeneration without cloud APIs:

```bash
scripts/graphify_local_extract.sh
```

This runs `graphify extract` through local Ollama only. It can be slow and should not replace routine updates.

The default local token budget is intentionally conservative (`1500`) so smaller local models can keep Graphify's required JSON response format. Tune it when using a larger or longer-context local model:

```bash
GRAPHIFY_LOCAL_EXTRACT_TOKEN_BUDGET=3000 scripts/graphify_local_extract.sh
```

The script has the same lifecycle behavior as labels: start Ollama if needed, pull the configured model if missing, run Graphify, then stop only the temporary Ollama process it started.

By default the script uses `graphify-qwen2.5-coder:7b`, a local Ollama variant tuned for strict JSON responses. It is based on `qwen2.5-coder:7b` with low temperature and a larger context window.

Use another installed local model:

```bash
GRAPHIFY_LOCAL_EXTRACT_MODEL=<model> scripts/graphify_local_extract.sh
```

## External Backends

Do not run `graphify extract` or `graphify label` with Gemini/OpenAI/Claude by default. External semantic extraction is useful only for explicit, larger knowledge-base refreshes because it can infer semantic documentation/config relationships and generate wiki content, but it spends API quota and can send project context to an external service.

## Exclusions

`.graphifyignore` keeps the graph focused on source navigation. CI workflows, generated Swagger dumps, Grafana dashboards, build output, and IDE metadata are excluded from Graphify because they are noisy for method discovery and can destabilize local semantic extraction. For CI/Docker/deployment changes, inspect the current files directly with `rg` before editing.
