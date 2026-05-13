# Clojure Skills for Coding Agent

A collection of Claude Code skills for robust Clojure, Babashka, and EDN development.

## Available Skills

### clojure-discovery
Automatically explore and gather context when writing Clojure code that uses unfamiliar APIs. This skill:

- Looks up documentation and source for Clojure functions and macros via `brepl`
- Searches for Clojure wrapper libraries before falling back to Java classes
- Prevents bugs by gathering sufficient API context before implementation

Use `/clojure-discovery` when encountering unfamiliar Clojure functions, macros, Java classes, or any API you're not completely confident about.

### clojure-repl-debugging
Debug Clojure code without modifying source files. This skill:

- Uses the REPL for inline inspection of values
- Traces execution and tests hypotheses without adding logging
- Integrates with `brepl` for efficient debugging

Use `/clojure-repl-debugging` when encountering bugs, test failures, or unexpected behavior. Load this **before** adding log statements or modifying code for debugging purposes.

### clj-lens
Multi-mode structural code reader for efficient Clojure inspection. This skill provides:

- **Coordinate Read**: Extract code at a specific file:line (when you know exactly where to look)
- **Symbol Lookup**: Find any symbol definition by name (e.g., `app.db/update-user`)
- **Pattern Search**: Locate all symbols matching a substring (e.g., all functions containing "update")
- **Error Context**: Extract source code from the last nREPL exception with analysis
- **Stacktrace Parsing**: Enrich stacktraces with actual source code from each frame

All modes return structured JSON for programmatic consumption, reducing token usage compared to full-file reads.

Use `/clj-lens` whenever you need to locate or understand Clojure code — avoid inefficient `grep`/`sed`/file-reading patterns. Integrates with clj-kondo (static analysis) and nREPL (runtime inspection) with graceful fallback.

### refactor-pm
Identify and improve code design by separating mechanism from policy. This skill:

- Scans files for opportunities to separate concerns
- Suggests refactoring patterns that improve maintainability

Use `/refactor-pm` to review code for design improvements.

### clj-skill-create-eval
Evaluate Clojure skills through rigorous real-project testing. This skill:

- Creates isolated test projects using Clojure Stack Lite (with/without skills)
- Designs realistic development tasks that showcase skill value
- Executes parallel subagent runs to ensure comparable conditions
- Compares behavioral differences: code quality, exploration patterns, debugging approach
- Analyzes context-logs to identify actual behavior changes vs hypothetical improvements
- Provides structured evaluation reports with quantitative and qualitative metrics

Use `/clj-skill-create-eval` when you've created or modified a Clojure skill and want to validate its effectiveness through real-world testing. This prevents vague evaluations and ensures skills actually improve development outcomes.

## Installation

Install via npx:

```bash
npx skills install humorless/clj-native-agent
```

This installs all five skills: `clojure-discovery`, `clojure-repl-debugging`, `clj-lens`, `refactor-pm`, and `clj-skill-create-eval`.

### Dependencies

- [brepl](https://github.com/licht1stein/brepl) — nREPL client for REPL-based inspection and evaluation

Install and ensure it's on your `PATH`.

## Setup

Start your nREPL server before the session:

```bash
# Clojure (deps.edn)
clj -M:nrepl

# Babashka
bb nrepl-server 1667
```

Claude will detect `.nrepl-port` automatically.

## Credits

- **brepl** by [@licht1stein](https://github.com/licht1stein) — https://github.com/licht1stein/brepl
- **Improve your code by separating mechanism from policy** by Arne Brasseur - https://lambdaisland.com/blog/2022-03-10-mechanism-vs-policy
- **"One Year of LLM Usage with Clojure"** by Ivan Willig — https://www.iwillig.me/blog/one-year-of-llm-usage-with-clojure/#skills-prompts-and-opencode
