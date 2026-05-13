# clj-native-agent: AI-Native Skills for Clojure Development

A collection of specialized Claude Code skills that help AI agents write, debug, and refactor Clojure code efficiently. These skills address the unique challenges of writing and maintaining Clojure in an AI-driven workflow.

## Why These Skills?

These skills **change how Claude approaches Clojure development** — they're not just informational guides, but behavioral redirects validated through real-world testing.

**The Problem:** Without these skills, Claude falls into inefficient patterns:
- **API exploration**: Trial-and-error instead of systematic discovery
- **Debugging**: Adding println/logging statements instead of interactive inspection
- **Code modification**: Whitespace-fragile text matching instead of structural replacement
- **Code design**: Accepting as-is instead of separating concerns

**The Solution:** Structured skills that leverage Clojure's nature as a data-driven, structural language:
- **clj-discover** redirects from random exploration → systematic API discovery
- **clj-debug** redirects from logging statements → REPL-based inspection
- **clj-replace** redirects from text-based matching → structural S-expression comparison
- **clj-refactor** redirects from accepting code as-is → separating mechanism and policy

## Available Skills

### clj-discover
**Instead of** trial-and-error exploration, systematically discover the right API. This skill:

- Looks up documentation and source for Clojure functions and macros via `brepl`
- Searches for Clojure wrapper libraries before falling back to Java classes
- Gathers sufficient API context before implementation to prevent bugs

Use `/clj-discover` when encountering unfamiliar Clojure functions, macros, Java classes, or any API you're not completely confident about.

### clj-debug
**Instead of** adding debug logs, use interactive REPL inspection. This skill:

- Inspects state and traces execution directly in the REPL without modifying code
- Tests hypotheses interactively for faster iteration
- Integrates with `brepl` for seamless debugging

Use `/clj-debug` when encountering bugs, test failures, or unexpected behavior. Load this **before** adding log statements or modifying code for debugging purposes.

### clj-replace
**Instead of** whitespace-fragile text matching, use structural S-expression comparison. This skill:

- Matches based on code structure (S-expressions) instead of literal text
- Succeeds regardless of formatting differences (indentation, line breaks, spacing)
- Preserves the original file's formatting while replacing matched nodes
- Provides detailed error reporting when matches are ambiguous

Use `/clj-replace` when Claude Code's `str_replace` fails due to formatting differences, or when you need reliable structural replacement of Clojure code.

### clj-refactor
**Instead of** accepting code as-is, identify opportunities to improve design. This skill:

- Scans for mechanism-policy coupling that reduces maintainability
- Suggests refactoring patterns that separate concerns
- Guides toward more flexible and testable code

Use `/clj-refactor` to review code for design improvements.

### clj-skill-eval
**For skill authors:** Validate that skills actually change behavior through rigorous testing. This meta-skill:

- Creates isolated test projects (with/without skills) running identical tasks in parallel
- Designs realistic development scenarios that showcase skill value
- Measures behavioral differences: code quality, exploration patterns, debugging approach
- Provides quantitative metrics (tokens, time, pass rates) and qualitative analysis

Use `/clj-skill-eval` when you've created or modified a Clojure skill and want to validate its effectiveness. This prevents vague claims and ensures skills measurably improve development outcomes. **Not for everyday development.**

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
4. Use `/clj-replace` when modifying code after formatting changes

## Validation

These skills have been validated through real-world evaluation using the `clj-skill-eval` framework:

- **clj-discover**: Systematically finds correct APIs instead of trial-and-error (100% vs 0% on discovery tasks)
- **clj-debug**: Uses REPL inspection instead of adding logs (100% vs 50% on debugging tasks)
- **clj-replace**: Handles format changes that str_replace fails on (100% vs 50% on refactoring tasks)

Each skill demonstrably changes how Claude approaches Clojure development, not just providing information.

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
