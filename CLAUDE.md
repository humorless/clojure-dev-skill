# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

This is a Claude Code skill repository for REPL-driven Clojure/Babashka development. It contains:

- `SKILL.md` — the skill definition loaded when `/clojure-dev` is invoked
- `references/idioms.md` — Clojure idioms reference linked from the skill

## Skill Architecture

The skill is triggered by the `clojure-dev` skill name and loaded via Claude Code's skill system. The `SKILL.md` front matter defines the skill's `name` and `description`. The body is the full prompt injected into context when the skill activates.

`references/idioms.md` is a supplementary reference document linked from `SKILL.md` — it is not auto-loaded, but the skill instructs Claude to read it when needed.

## Editing the Skill

When modifying `SKILL.md`:

- Keep the YAML front matter (`name`, `description`) intact — these fields are parsed by the skill loader
- The `description` field determines when the skill auto-triggers (file extensions, project files)
- Constraints in the Validation Checklist must stay consistent with rules described elsewhere in the file (e.g., the namespace docstring rule is both in "Namespace Docstring" and the checklist)
- `references/idioms.md` is a standalone Markdown file; update it separately if idioms change

## Key Conventions Enforced by This Skill

- `brepl` with heredoc (`<<'EOF'`) is the required evaluation pattern
- Every namespace must have a docstring describing a single responsibility
- `clj-kondo` lint after every file save
- Validation checklist must pass before saving any code
- Lines ≤ 80 characters, 2-space indentation, closing parens on same line
