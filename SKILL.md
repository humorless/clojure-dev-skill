---
name: clojure-dev
description: "REPL-driven development workflow for Clojure, Babashka, and EDN. Load when working with .clj, .cljs, .cljc, .bb, bb.edn, deps.edn, or project.clj files. Covers brepl usage, workflow, discovery, and project conventions."
---

# Clojure / Babashka REPL-Driven Development

## brepl — Evaluating Code

`brepl` is the nREPL client used for all code evaluation in this workflow.

### Always Use Heredoc

**Always use heredoc for all brepl evaluations.** This eliminates quoting issues and works for all cases.

```bash
brepl <<'EOF'
(your clojure code here)
EOF
```

Use `<<'EOF'` (with quotes), not `<<EOF` — this prevents shell variable expansion. Inside the heredoc, write Clojure code naturally with no escaping needed.

For simple one-liners, positional argument also works:

```bash
brepl '(+ 1 2 3)'
```

Heredoc is preferred for anything with quotes, multiple lines, or reader macros.

### Examples

**Multi-line with quotes:**

```bash
brepl <<'EOF'
(require '[clojure.string :as str])
(str/join ", " ["a" "b" "c"])
EOF
```

**Complex data structures:**

```bash
brepl <<'EOF'
(def config
  {:database {:host "localhost"
              :port 5432}
   :api      {:key "secret"}})
(println (:database config))
EOF
```

**Loading a file into the REPL:**

```bash
brepl -f src/myapp/core.clj
```

### Port

brepl auto-detects `.nrepl-port` in the project root. To specify explicitly:

```bash
brepl -p 1234 <<'EOF'
(+ 1 1)
EOF
```

## nREPL Connection

### Verify connection

```bash
brepl <<'EOF'
(+ 1 1)
EOF
```

If this fails, ask the user to start their nREPL server:

- Clojure (deps): `clj -M:nrepl` or check `deps.edn` for the nrepl alias
- Clojure (lein): `lein repl :headless`
- Babashka: `bb nrepl-server <port>`

### Port

brepl auto-detects `.nrepl-port` in the project root. To specify explicitly:

```bash
brepl -p 1234 <<'EOF'
(+ 1 1)
EOF
```

## Core Workflow

**Never write code without REPL validation.**

Before modifying any file:

1. `read` the target file and related files
2. Verify nREPL connection with `(+ 1 1)`
3. Explore unfamiliar functions via `doc` / `source` (see Discovery below)
4. Define and test functions in REPL before saving
5. Check edge cases: nil, empty collections, invalid inputs
6. Save only after validation
7. Run clj-kondo after saving

## Discovery

```bash
brepl <<'EOF'
(require '[clojure.repl :refer [doc source dir apropos find-doc]])

; function documentation
(doc map)

; list all public vars in a namespace
(dir clojure.string)

; search by name pattern
(apropos "split")

; search docstrings
(find-doc "regular expression")

; read source
(source filter)

; check arities
(:arglists (meta #'reduce))
EOF
```

## Linting

After saving a file, always lint it:

```bash
clj-kondo --lint <file>
```

To lint the entire project:

```bash
clj-kondo --lint src
```

Fix all warnings before proceeding. Common issues to watch:
- Unused namespaces in `require`
- Unused bindings
- Arity mismatches
- Shadowed vars

## Post-Edit Reload

After saving a file, always sync the REPL:

```bash
brepl <<'EOF'
(require '[my.namespace] :reload)
(my.namespace/my-fn test-input)
EOF
```

Use `:reload-all` when transitive dependencies have changed.

## Running Tests

```bash
brepl <<'EOF'
(require '[clojure.test :refer [run-tests]])
(require '[myapp.core-test] :reload)
(run-tests 'myapp.core-test)
EOF
```

## Babashka

Babashka nREPL is started with `bb nrepl-server <port>`. Most Clojure workflow patterns apply. Key differences:

- Project config is `bb.edn`, not `deps.edn`
- No AOT compilation
- Subset of Java interop available
- Scripts typically use `.bb` extension

## Working with EDN

brepl can read, inspect, and transform EDN data:

```bash
brepl <<'EOF'
(require '[clojure.edn :as edn])
(def config (edn/read-string (slurp "config.edn")))
(keys config)
EOF
```

## Fixing Bracket Errors

Use `brepl balance` — never fix manually:

```bash
# Fix in place
brepl balance src/myapp/core.clj

# Preview first
brepl balance src/myapp/core.clj --dry-run
```

Supports `.clj`, `.cljs`, `.cljc`, `.bb`, `.edn`.

## Validation Checklist

Before saving any code:

- [ ] Happy path tested in REPL
- [ ] clj-kondo reports no warnings
- [ ] nil input handled
- [ ] Empty collection handled
- [ ] Edge cases and boundary values covered
- [ ] Public functions have docstrings
- [ ] Naming follows conventions (see `references/idioms.md`)
- [ ] Lines under 80 characters
- [ ] Closing parens gathered on single line

## Idioms Reference

For threading macros, naming conventions, data structures, error handling, testing patterns, and anti-patterns, read:

[references/idioms.md](references/idioms.md)
