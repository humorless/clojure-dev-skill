# Clojure Skills for Claude Code

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

### refactor-pm
Identify and improve code design by separating mechanism from policy. This skill:

- Scans files for opportunities to separate concerns
- Suggests refactoring patterns that improve maintainability

Use `/refactor-pm` to review code for design improvements.

### clojure-fast-read
Choose the right strategy for reading Clojure code efficiently. This skill:

- Decides when to use runtime inspection (REPL) vs file reading based on intent
- Uses `source-fn`, `doc`, and `meta` for fast, targeted exploration
- Leverages `meta` to bridge runtime state and source file locations
- Recommends `grep` and `sed` for targeted code extraction without reading full files
- Distinguishes between understanding runtime behavior vs source design

Use `/clojure-fast-read` whenever you need to inspect Clojure code — whether modifying a function, debugging a production issue, exploring a new codebase, or understanding dependencies. This skill ensures you read efficiently instead of opening entire files.

## Installation

Install via npx:

```bash
npx skills install humorless/clojure-dev
```

This installs all four skills: `clojure-discovery`, `clojure-repl-debugging`, `clojure-fast-read`, and `refactor-pm`.

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
