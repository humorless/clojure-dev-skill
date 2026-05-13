---
name: clojure-fast-read
description: Structural Clojure code reading. Use this to get the exact code of a symbol using either REPL (source-fn) for runtime state or clj-lens for precise file source.
---

# Clojure Fast Read

Clojure is structural. Avoid `sed` or full-file reads for inspecting specific symbols. Instead, use these two "Precision Lenses":

## 1. The Runtime Lens (`source-fn`)
**When to use:** 
- Verifying if a change was successfully evaluated into the REPL.
- Debugging behavior in the current running system.
- **Tool:** `(clojure.repl/source-fn 'ns/name)` via REPL tools (e.g., `brepl` or `clj-nrepl-eval`).

## 2. The Source Lens (`clj-lens`)
**When to use:** 
- Preparing to edit a function (need exact file content for search/replace blocks).
- Reading code that hasn't been evaluated yet or exists only on disk.
- **Tool:** `./scripts/clj-lens.bb <file> <line>` (uses rewrite-clj to extract the full S-expression).

---

## Decision Logic

| Goal | Action |
| :--- | :--- |
| **I need to modify this code** | Get location via `(meta #'ns/name)`, then use **`clj-lens`** to get the perfect edit target. |
| **I just edited/evaled this code** | Use **`source-fn`** to confirm the REPL runtime state matches your edit. |
| **I'm lost in a long file** | Use **`clj-lens`** at the symbol's start line to isolate only that function's scope. |

## Implementation Examples

### Precise Source Reading
```bash
# 1. Get metadata (from REPL)
# (meta #'my.app/my-func) -> {:file "src/my/app.clj" :line 42}

# 2. Get the full, structural S-expression from disk
./scripts/clj-lens.bb src/my/app.clj 42
```

### Runtime Verification

```
;; Check what the JVM is actually running right now
(clojure.repl/source-fn 'my.app/my-func)
```

## New Multi-Mode Usage

### Symbol Lookup

```bash
# Find and extract a specific symbol
./scripts/clj-lens.bb --symbol app.db/update-user
# Output: JSON with file, line, and complete function definition
```

### Find Symbols by Pattern

```bash
# Search for all definitions containing "update"
./scripts/clj-lens.bb --find "update"
# Output: JSON with array of matching symbols and locations
```

### Extract Error Context

```bash
# Show source code at the last exception location (requires nREPL)
./scripts/clj-lens.bb --last-error
# Output: JSON with exception type, location, and source form
```

### Parse Stacktraces

```bash
# Extract code from each frame in a stacktrace
./scripts/clj-lens.bb --trace "java.lang.NullPointerException
	at app.core\$func (core.clj:42)
	at app.ui\$render (ui.clj:88)"
# Output: JSON with enriched frames including source code
```
