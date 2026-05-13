# clj-native-agent: AI-Native Skills for Clojure Development

A collection of specialized Claude Code skills that help AI agents write, debug, and refactor Clojure code efficiently. These skills address the unique challenges of writing and maintaining Clojure in an AI-driven workflow.

## Why These Skills?

**The Problem:** AI agents working with Clojure often face friction:
- Manual exploration of unfamiliar APIs and their documentation
- Debugging sessions that require modifying code and adding logging
- Code modifications that fail because formatting differs from the source
- Lack of systematic approaches to understand and improve code design

**The Solution:** Structured skills that leverage Clojure's nature as a data-driven, structural language:
- Context-aware discovery for unfamiliar APIs and dependencies
- Systematic debugging using REPL inspection without modifying source
- Format-insensitive structural code replacement
- Mechanism-policy separation analysis for code maintainability

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

### clj-replace
Format-insensitive code replacement based on S-expression equivalence. This skill:

- Parses code into S-expression trees for structural comparison
- Matches based on semantics, ignoring whitespace, indentation, and line breaks
- Preserves the original file's formatting while replacing matched nodes
- Provides detailed error reporting with location information

Use `/clj-replace` when Claude Code's `str_replace` fails due to formatting differences, or when you need reliable structural replacement of Clojure code.

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

This installs all five skills: `clj-discover`, `clj-debug`, `clj-replace`, `clj-refactor`, and `clj-skill-eval`.

## Quick Start

### 1. Install Optional Dependencies (Recommended)

For full REPL-based debugging, install brepl:

```bash
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
/clj-discover                              # Understand unfamiliar APIs
/clj-debug                                 # Debug without modifying code
/clj-replace file.clj "old" "new"         # Structural replacement (format-insensitive)
/clj-refactor                              # Improve code design
```

## How They Work Together

**Typical workflow:**
1. Use `/clj-discover` to understand unfamiliar dependencies
2. Use `/clj-debug` if behavior doesn't match expectations
3. Use `/clj-refactor` to improve design during refactoring

## Design Philosophy

These skills follow key principles:

- **Structural, Not Textual**: Leverage Clojure's nature as code-as-data
- **Precision Over Breadth**: Extract exactly what's needed, not entire files
- **Optional Dependencies**: Core functionality works standalone; extras enhance capability
- **AI-Friendly**: Structured output, consistent interfaces, reduced context overhead

## Credits

- **brepl** by [@licht1stein](https://github.com/licht1stein) — https://github.com/licht1stein/brepl
- **rewrite-clj** for structural Clojure parsing — https://github.com/clj-commons/rewrite-clj
- **Improve your code by separating mechanism from policy** by Arne Brasseur — https://lambdaisland.com/blog/2022-03-10-mechanism-vs-policy
- **"One Year of LLM Usage with Clojure"** by Ivan Willig — https://www.iwillig.me/blog/one-year-of-llm-usage-with-clojure/#skills-prompts-and-opencode
