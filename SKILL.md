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

## Core Workflow

**Never write code without REPL validation.**

Before modifying any file:

1. `read` the target file and related files
2. Verify nREPL connection: `(+ 1 1)`
3. Explore unfamiliar functions via `doc` / `source` (see Discovery below)
4. Balance brackets before editing: `brepl balance <file> --dry-run`
5. Define and test functions in REPL before saving
6. Check edge cases: nil, empty collections, invalid inputs
7. Save the file
8. Lint: `clj-kondo --lint <file>` — fix all warnings before proceeding
9. Reload and verify: `(require '[myapp.core] :reload)`
10. Complete the Pre-Save Checklist (see below)

## Discovery

**Clojure functions**
```bash
brepl <<'EOF'
(require '[clojure.repl :refer [doc source dir apropos find-doc]])
(doc map)                        ; function documentation
(dir clojure.string)             ; list all public vars in a namespace
(apropos "split")                ; search by name pattern
(find-doc "regular expression")  ; search docstrings
(source filter)                  ; read source
(:arglists (meta #'reduce))      ; check arities
EOF
```

**Macros**
```bash
brepl <<'EOF'
(macroexpand-1 '(your-macro args))                    ; expand one level
(macroexpand '(your-macro args))                      ; expand fully
(clojure.walk/macroexpand-all '(your-macro args))     ; expand all nested forms
EOF
```

**Java interop**
```bash
brepl <<'EOF'
(require '[clojure.reflect :as r])
(r/reflect SomeJavaClass)   ; list all methods and fields

; filter to just method names
(->> (r/reflect SomeJavaClass)
     :members
     (filter #(instance? clojure.reflect.Method %))
     (map :name)
     sort)
EOF
```

## Bracket Balancing

Use `brepl balance` before and after edits — never fix brackets manually.

```bash
# Preview before editing
brepl balance src/myapp/core.clj --dry-run

# Fix in place
brepl balance src/myapp/core.clj
```

Supports `.clj`, `.cljs`, `.cljc`, `.bb`, `.edn`.

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
(require '[myapp.core] :reload)
(myapp.core/my-fn test-input)
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

### Testing Strategy

- **New features / refactoring**: Verify interactively via brepl. Do not write tests unless asked.
- **Bug fixes**: Write a failing test that reproduces the bug first, then fix the code and confirm it passes.
- **Pre-commit**: Before finalizing a task or preparing a commit, proactively ask if tests should be added or updated for the changed logic.

---

## Pre-Save Checklist

This checklist covers items that **cannot be verified by tools alone**. Complete it after linting and reloading pass.

### Namespace Docstring

Every namespace must have a docstring that describes its **single responsibility**:

```clojure
(ns myapp.auth
  "Handles authentication and session management.")
```

Before saving, verify the docstring is still accurate:

```bash
brepl <<'EOF'
(require '[myapp.auth])
(:doc (meta (find-ns 'myapp.auth)))
EOF
```

Judgment criteria:
- If the docstring requires "and" or "or" to describe what the namespace does → split the namespace
- If new functions were added that fall outside the stated responsibility → update the docstring or extract those functions

### Macro Docstrings

Prefer functions over macros. Only use macros when evaluation control or syntax manipulation is strictly necessary.

Every macro must have a docstring with a usage example:

```clojure
(defmacro with-db-conn
  "Opens a database connection and binds it to `conn`.
   Usage: (with-db-conn [conn db-spec] (query conn ...))"
  [binding & body] ...)
```

Judgment criteria:
- Is the macro actually necessary, or can a function do the job?
- Does the docstring include a concrete usage example?

### Malli Schema

Every map that crosses a namespace boundary must have a named schema defined in `myapp.schema`.

#### Where to define schemas

Define all schemas in a dedicated `myapp.schema` namespace. Never define schemas inline at the call site.

```clojure
(ns myapp.schema
  "Single source of truth for all cross-namespace data shapes.")

(def User
  [:map
   [:id :uuid]
   [:email :string]
   [:role [:enum :admin :viewer]]])

(def Order
  [:map
   [:id :uuid]
   [:user-id :uuid]
   [:items [:vector :uuid]]])
```

#### Validation at namespace boundaries

Validate at the entry point of a namespace, not deep inside. Use `ex-info` with `malli.error/humanize` for actionable error messages:

```clojure
(ns myapp.order
  (:require [malli.core :as m]
            [malli.error :as me]
            [myapp.schema :as schema]))

(defn create-order [order]
  (when-not (m/validate schema/Order order)
    (throw (ex-info "Invalid order shape"
                    {:error (me/humanize (m/explain schema/Order order))
                     :input order})))
  ...)
```

#### Workflow for data shapes

Before writing any function that accepts or returns a map:

1. Check `myapp.schema` — does a schema exist for this shape?
2. If yes, use it. Do NOT redefine or assume the shape locally.
3. If no, define it in `myapp.schema` first, then write the function.
4. After modifying a schema, search for all namespaces that require `myapp.schema` and verify they are still consistent.

### General

- [ ] Public functions have docstrings; if the return value is a map, the docstring states its shape or references the schema
- [ ] Naming follows conventions (see `references/idioms.md`)
- [ ] Lines under 80 characters
- [ ] Closing parens gathered on single line

---

## Babashka

Babashka nREPL is started with `bb nrepl-server <port>`. Most Clojure workflow patterns apply. Key differences:

- Project config is `bb.edn`, not `deps.edn`
- No AOT compilation
- Subset of Java interop available
- Scripts typically use `.bb` extension

## Working with EDN

```bash
brepl <<'EOF'
(require '[clojure.edn :as edn])
(def config (edn/read-string (slurp "config.edn")))
(keys config)
EOF
```

## Idioms Reference

For threading macros, naming conventions, data structures, error handling, testing patterns, and anti-patterns, read:

[references/idioms.md](references/idioms.md)
