# clojure-dev skill for Claude Code

A Claude Code skill that gives Claude a REPL-driven development workflow for Clojure, Babashka, and EDN projects.

When active, Claude will:

- Evaluate code through the nREPL via `brepl` before writing any file
- Lint with `clj-kondo` after every save
- Enforce namespace docstrings and single-responsibility namespaces
- Prefer functions over macros; require docstrings with usage examples on every macro
- Define all cross-namespace map shapes as named Malli schemas in `myapp.schema`
- Follow idiomatic Clojure conventions (threading macros, naming, layout)
- Run a validation checklist before finalising any code

## Installation

Claude Code skills live in `~/.claude/skills/`. Clone this repo there:

```bash
git clone https://github.com/humorless/clojure-dev ~/.claude/skills/clojure-dev
```

Claude Code picks up skills automatically from that directory — no further configuration needed.

### Dependencies

- [brepl](https://github.com/licht1stein/brepl) — the nREPL client used for all REPL evaluation
- [clj-kondo](https://github.com/clj-kondo/clj-kondo) — linter run after every file save

Install both and make sure they are on your `PATH`.

## Usage

The skill activates automatically when Claude Code detects Clojure project files (`.clj`, `.cljs`, `.cljc`, `.bb`, `bb.edn`, `deps.edn`, `project.clj`).

You can also load it explicitly with:

```
/clojure-dev
```

Start your nREPL server before the session:

```bash
# Clojure (deps.edn)
clj -M:nrepl

# Babashka
bb nrepl-server 1667
```

Claude will detect `.nrepl-port` automatically.

## Credits

Most of the ideas here are borrowed from:

- **brepl** by [@licht1stein](https://github.com/licht1stein) — https://github.com/licht1stein/brepl
- **"One Year of LLM Usage with Clojure"** by Ivan Willig — https://www.iwillig.me/blog/one-year-of-llm-usage-with-clojure/#skills-prompts-and-opencode
