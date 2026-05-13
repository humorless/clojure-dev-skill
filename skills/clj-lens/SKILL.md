---
name: clj-lens
description: Multi-mode structural Clojure code reader. Efficiently locate symbols, extract source code, and understand errors without full-file reads or inefficient grep patterns.
---

# clj-lens: Structural Code Reading for Clojure

Clojure code is structural. Reading it efficiently means using precision tools built for S-expressions, not generic text tools.

**clj-lens** is a multi-mode reader that answers common agent questions:
- "Extract code at this exact location" — Read mode returns raw code
- "Where is this symbol defined?" — Symbol mode with structural lookup
- "What does this function look like?" — Multi-mode extraction
- "Where is this error happening?" — Error context extraction
- "What code is in this stacktrace?" — Frame-by-frame analysis

Most modes return structured JSON; Read mode returns raw code for direct use.

---

## The 5 Modes

Choose the mode based on what you need to know:

### 1. **Read** — Extract code at file:line:column with bracket-aware precision

**When to use:** You know exactly where code is and want to extract just that form (from error messages, metadata, or earlier searches)

```bash
./scripts/clj-lens.bb --read 42 5 src/app/core.clj
```

**How it works:**
- Column number specifies which form to extract (like a structural enhancement to `sed`)
- Smaller column → outer forms; larger column → inner forms
- Understands bracket matching (unlike text-based tools)

**Output:**
```
(defn get-user [id] ...)
```

Direct code output (not JSON), ready to paste or pipe to other tools.

---

### 2. **Symbol Lookup** — Find a specific symbol by name

**When to use:** You have a symbol name (like `app.db/update-user`) and need to see its definition

```bash
./scripts/clj-lens.bb --symbol app.db/update-user
```

**How it works:**
1. Uses static analysis (clj-kondo) to locate the symbol definition
2. Extracts the complete S-expression from source
3. Returns file, line, and full code

**Output (exact match):**
```json
{
  "status": "ok",
  "mode": "symbol",
  "data": {
    "symbol": "app.db/update-user",
    "file": "src/app/db.clj",
    "line": 42,
    "form": "(defn update-user [id user] ...)"
  }
}
```

**Output (no exact match, suggestions):**
```json
{
  "status": "suggestion",
  "message": "Exact match not found. Did you mean one of these?",
  "matches": [
    {"symbol": "app.db/update-user", "namespace": "app.db", "file": "src/app/db.clj", "line": 42},
    {"symbol": "app.ui/user-update-form", "namespace": "app.ui", "file": "src/app/ui.clj", "line": 88}
  ]
}
```

---

### 3. **Find by Pattern** — Search for symbols matching a substring

**When to use:** You remember part of a function name but not the full namespace

```bash
./scripts/clj-lens.bb --find "update"
```

**Output:**
```json
{
  "status": "ok",
  "mode": "find",
  "data": {
    "pattern": "update",
    "matches": [
      {"name": "update-user", "namespace": "app.db", "file": "src/app/db.clj", "line": 42},
      {"name": "user-update-form", "namespace": "app.ui", "file": "src/app/ui.clj", "line": 88},
      {"name": "on-update", "namespace": "app.events", "file": "src/app/events.clj", "line": 156}
    ]
  }
}
```

---

### 4. **Last Error** — Extract code at the error location

**When to use:** Your REPL threw an exception and you need to see what code caused it (requires nREPL running)

```bash
./scripts/clj-lens.bb --last-error
```

**How it works:**
1. Connects to nREPL and reads the last exception (`*e`)
2. Parses the exception to find file and line number
3. Extracts source code at that location
4. Includes basic error analysis (suspect variable detection)

**Output:**
```json
{
  "status": "ok",
  "mode": "last-error",
  "data": {
    "error": "java.lang.NullPointerException",
    "location": {"file": "src/app/core.clj", "line": 42},
    "form": "(defn get-user [u] (:name u))",
    "analysis": {"suspect": "u", "reason": "possible-nil"}
  }
}
```

**Output (nREPL unavailable):**
```json
{
  "status": "error",
  "error": "nrepl-unavailable",
  "message": "nREPL server not found. Start with: clj -M:nrepl or bb nrepl-server 1667"
}
```

---

### 5. **Trace** — Parse stacktrace and extract code from each frame

**When to use:** You have a Java/Clojure stacktrace and want to see the code at each location

```bash
./scripts/clj-lens.bb --trace "java.lang.NullPointerException
  at app.core\$get_user (core.clj:42)
  at app.ui\$render (ui.clj:88)"
```

**How it works:**
1. Parses stacktrace text to extract file:line pairs
2. Reads source code at each location
3. Returns enriched frames with code snippets

**Output:**
```json
{
  "status": "ok",
  "mode": "trace",
  "data": {
    "frames": [
      {
        "class": "app.core",
        "method": "get_user",
        "file": "src/app/core.clj",
        "line": 42,
        "form": "(defn get-user [u] (:name u))"
      },
      {
        "class": "app.ui",
        "method": "render",
        "file": "src/app/ui.clj",
        "line": 88,
        "form": "(defn render [state] ...)"
      }
    ]
  }
}
```

---

## Quick Reference: Which Mode to Use

| Situation | Mode | Command |
|-----------|------|---------|
| You know file + line:column | Read | `clj-lens.bb --read 42 5 src/app.clj` |
| You have a symbol name | Symbol | `clj-lens.bb --symbol app.db/user` |
| You remember part of a name | Find | `clj-lens.bb --find "update"` |
| REPL threw an error | Last Error | `clj-lens.bb --last-error` |
| You have a stacktrace | Trace | `clj-lens.bb --trace "<stacktrace>"` |

---

## Key Features

- **Read Mode Direct Output:** Read mode returns raw code, ready to use (not JSON-wrapped)
- **Structured JSON for Metadata:** Symbol, Find, Last Error, Trace modes return JSON with location metadata
- **No Full-File Reads:** Extract only the code you need using structural S-expression parsing
- **Bracket-Aware:** Unlike sed/grep, understands Clojure's structural nature (parentheses, brackets)
- **Graceful Degradation:** Optional dependencies (clj-kondo, nREPL) fail cleanly with helpful error messages
- **Agent-Friendly:** Reduces token usage compared to reading entire files
- **Fast:** Queries complete in milliseconds

---

## Dependencies

**Required:**
- Babashka (runtime)
- rewrite-clj (already included)

**Optional (graceful fallback if unavailable):**
- **clj-kondo:** For `--symbol` and `--find` modes (static analysis). Install: `npm install -g clj-kondo`
- **nREPL:** For `--last-error` mode (runtime inspection). Start: `clj -M:nrepl`
