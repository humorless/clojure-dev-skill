# clj-native-agent: AI-Native Skills for Clojure Development

A collection of specialized Claude Code skills that help AI agents write, debug, and refactor Clojure code efficiently. These skills address the unique challenges of writing and maintaining Clojure in an AI-driven workflow.

## Why These Skills?

**The Problem:** AI agents working with Clojure often resort to inefficient patterns:
- Full-file reads to find a single function
- Grep-based searching that returns partial matches
- Manual REPL inspection for every question about code state
- Contextual overhead when understanding errors and stacktraces

**The Solution:** Structured skills that leverage Clojure's nature as a data-driven, structural language:
- Precise code location and extraction tools
- Static and runtime analysis for efficient exploration
- Systematic debugging without source modification
- Context-aware discovery for unfamiliar APIs

## Available Skills

### clj-discover
Automatically explore and gather context when writing Clojure code that uses unfamiliar APIs. This skill:

- Looks up documentation and source for Clojure functions and macros via `brepl`
- Searches for Clojure wrapper libraries before falling back to Java classes
- Prevents bugs by gathering sufficient API context before implementation

Use `/clj-discover` when encountering unfamiliar Clojure functions, macros, Java classes, or any API you're not completely confident about.

### clj-debug
Debug Clojure code without modifying source files. This skill:

- Uses the REPL for inline inspection of values
- Traces execution and tests hypotheses without adding logging
- Integrates with `brepl` for efficient debugging

Use `/clj-debug` when encountering bugs, test failures, or unexpected behavior. Load this **before** adding log statements or modifying code for debugging purposes.

### clj-lens
Multi-mode structural code reader for efficient Clojure inspection. This skill provides:

- **Coordinate Read**: Extract code at a specific file:line (when you know exactly where to look)
- **Symbol Lookup**: Find any symbol definition by name (e.g., `app.db/update-user`)
- **Pattern Search**: Locate all symbols matching a substring (e.g., all functions containing "update")
- **Error Context**: Extract source code from the last nREPL exception with analysis
- **Stacktrace Parsing**: Enrich stacktraces with actual source code from each frame

All modes return structured JSON for programmatic consumption, reducing token usage compared to full-file reads.

Use `/clj-lens` whenever you need to locate or understand Clojure code — avoid inefficient `grep`/`sed`/file-reading patterns. Integrates with clj-kondo (static analysis) and nREPL (runtime inspection) with graceful fallback.

### clj-refactor
Identify and improve code design by separating mechanism from policy. This skill:

- Scans files for opportunities to separate concerns
- Suggests refactoring patterns that improve maintainability

Use `/clj-refactor` to review code for design improvements.

### clj-skill-eval
Evaluate Clojure skills through rigorous real-project testing. This skill:

- Creates isolated test projects using Clojure Stack Lite (with/without skills)
- Designs realistic development tasks that showcase skill value
- Executes parallel subagent runs to ensure comparable conditions
- Compares behavioral differences: code quality, exploration patterns, debugging approach
- Analyzes context-logs to identify actual behavior changes vs hypothetical improvements
- Provides structured evaluation reports with quantitative and qualitative metrics

Use `/clj-skill-eval` when you've created or modified a Clojure skill and want to validate its effectiveness through real-world testing. This prevents vague evaluations and ensures skills actually improve development outcomes.

## Installation

Install via npx:

```bash
npx skills install humorless/clj-native-agent
```

This installs all five skills: `clj-discover`, `clj-debug`, `clj-lens`, `clj-refactor`, and `clj-skill-eval`.

## Quick Start

### 1. Install Optional Dependencies (Recommended)

For full functionality, install these optional tools:

```bash
# Install clj-kondo for static analysis (enables --symbol and --find modes)
npm install -g clj-kondo

# Install brepl for REPL interaction (enables /clj-debug and --last-error mode)
npm install -g brepl
```

### 2. Start nREPL (If Using Runtime Inspection)

Start an nREPL server before your Claude Code session:

```bash
# Clojure project
clj -M:nrepl

# Babashka project
bb nrepl-server 1667
```

Claude will auto-detect `.nrepl-port` and use it automatically.

### 3. Use the Skills

In Claude Code:

```
/clj-lens --symbol app.db/update-user     # Find a symbol definition
/clj-discover                              # Understand unfamiliar APIs
/clj-debug                                 # Debug without modifying code
/clj-refactor                              # Improve code design
```

## How They Work Together

**Typical workflow:**
1. Use `/clj-lens` to locate and extract code precisely
2. Use `/clj-discover` to understand unfamiliar dependencies
3. Use `/clj-debug` if behavior doesn't match expectations
4. Use `/clj-refactor` to improve design during refactoring

## Architecture Notes

### clj-lens: Structural Code Reading

The `clj-lens` tool is the foundation of efficient code inspection:

- **Static Analysis** (clj-kondo): Fast project-wide symbol discovery without executing code
- **Structural Parsing** (rewrite-clj): Extract only what you need using S-expression parsing
- **Runtime Context** (nREPL): Optional integration for examining running system state
- **Graceful Degradation**: All features work standalone; optional dependencies fail cleanly

All output is structured JSON, designed for programmatic consumption and reduced token usage.

## Design Philosophy

These skills follow key principles:

- **Structural, Not Textual**: Leverage Clojure's nature as code-as-data
- **Precision Over Breadth**: Extract exactly what's needed, not entire files
- **Optional Dependencies**: Core functionality works standalone; extras enhance capability
- **AI-Friendly**: Structured output, consistent interfaces, reduced context overhead

## Credits

- **brepl** by [@licht1stein](https://github.com/licht1stein) — https://github.com/licht1stein/brepl
- **rewrite-clj** for structural Clojure parsing — https://github.com/clj-commons/rewrite-clj
- **clj-kondo** for static analysis — https://github.com/clj-kondo/clj-kondo
- **Improve your code by separating mechanism from policy** by Arne Brasseur — https://lambdaisland.com/blog/2022-03-10-mechanism-vs-policy
- **"One Year of LLM Usage with Clojure"** by Ivan Willig — https://www.iwillig.me/blog/one-year-of-llm-usage-with-clojure/#skills-prompts-and-opencode
