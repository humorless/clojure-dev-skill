---
title: clj-lens Multi-Mode Structural Code Reader
date: 2026-05-13
status: design
---

# clj-lens Multi-Mode Structural Code Reader

## Purpose

Enhance `clj-lens.bb` to reduce coding agent context usage by providing multiple precision-read modes. Currently agents use inefficient grep/sed/file-read patterns to locate and extract code. This tool provides direct, structured access to code snippets without requiring full-file reads.

**Key insight**: Coding agents need to locate and understand code efficiently. By providing specialized modes for common operations (symbol lookup, error context, stacktrace parsing), we reduce the number of context-heavy round-trips an agent must make.

## Architecture

### Data Flow

```
Input (mode + args)
    ↓
Dispatch to mode handler
    ↓
Query primary source (clj-kondo, nREPL, or file)
    ↓
Extract code via rewrite-clj (if needed)
    ↓
Return structured JSON
```

### Primary Data Sources (in order of precedence)

1. **clj-kondo export** (for symbol/find modes): Run `clj-kondo export --format json` to get project-wide symbol analysis
2. **File reading** (for all modes): Use rewrite-clj to extract full S-expressions at specific locations
3. **nREPL** (for last-error mode): Connect to `.nrepl-port` if available; fail gracefully otherwise

### Graceful Degradation

- If clj-kondo is unavailable: Fall back to file scanning
- If nREPL is unavailable: last-error mode returns `:error` status with helpful message
- All modes return JSON, enabling agents to handle failures programmatically

## Modes

### 1. Read (Coordinate with line + column precision)

**Purpose**: Extract the full, balanced S-expression at a specific line:column position.

**Usage**: `clj-lens.bb --read <line> <column> <file>`

**Example**:
```bash
clj-lens.bb --read 42 5 src/core.clj
```

**Output** (success):
```json
{
  "status": "ok",
  "mode": "read",
  "data": {
    "file": "src/core.clj",
    "line": 42,
    "column": 5,
    "form": "(defn get-name [user] (:name user))"
  }
}
```

**Notes**:
- Column number (0-indexed or 1-indexed) determines which form is returned when multiple forms exist on the same line
- Smaller column numbers may match outer forms, larger column numbers match inner forms
- This is a structural enhancement over simple line-based extraction (like `sed`), since it understands bracket matching

### 2. Symbol Lookup

**Purpose**: Find and extract the definition of a specific symbol (function, var, macro, etc.).

**Usage**: `clj-lens.bb --symbol <namespace/name>`

**Process**:
1. Query clj-kondo export for exact symbol match
2. If found: Read the file at that line using rewrite-clj
3. Return location + extracted form

**Example**:
```bash
clj-lens.bb --symbol app.db/update-user
```

**Output** (exact match):
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

**Output** (no exact match, suggestions):
```json
{
  "status": "suggestion",
  "message": "Exact match not found. Did you mean one of these?",
  "matches": [
    {"symbol": "app.db/update-user", "file": "src/app/db.clj", "line": 42},
    {"symbol": "app.ui/user-update-form", "file": "src/app/ui.clj", "line": 88}
  ]
}
```

### 3. Find (Pattern Search)

**Purpose**: Search for all definitions whose names contain a pattern.

**Usage**: `clj-lens.bb --find "partial-name"`

**Process**:
1. Query clj-kondo export for all definitions
2. Filter by substring match on symbol name
3. Return all matches with locations

**Example**:
```bash
clj-lens.bb --find "update"
```

**Output**:
```json
{
  "status": "ok",
  "mode": "find",
  "data": {
    "pattern": "update",
    "matches": [
      {"symbol": "app.db/update-user", "file": "src/app/db.clj", "line": 42},
      {"symbol": "app.ui/user-update-form", "file": "src/app/ui.clj", "line": 88},
      {"symbol": "app.events/on-update", "file": "src/app/events.clj", "line": 156}
    ]
  }
}
```

### 4. Last Error

**Purpose**: Extract source code context from the last nREPL exception.

**Usage**: `clj-lens.bb --last-error`

**Process**:
1. Connect to nREPL (via `.nrepl-port`)
2. Read `*e` (last exception)
3. Parse exception to find file + line
4. Extract source code at that location
5. Attempt to identify suspicious code (simple heuristics: nil checks, arity mismatches)

**Example**:
```bash
clj-lens.bb --last-error
```

**Output** (success):
```json
{
  "status": "ok",
  "mode": "last-error",
  "data": {
    "error": "java.lang.NullPointerException",
    "message": "Cannot invoke method on null",
    "location": {
      "file": "src/core.clj",
      "line": 42
    },
    "form": "(defn get-name [user] (:name user))",
    "analysis": {
      "suspect": "user",
      "reason": "possible-nil"
    }
  }
}
```

**Output** (nREPL unavailable):
```json
{
  "status": "error",
  "error": "nrepl-unavailable",
  "message": "nREPL server not found. Start with: clj -M:nrepl or bb nrepl-server 1667"
}
```

### 5. Trace (Stacktrace Parsing)

**Purpose**: Parse a stacktrace string and extract source code for each frame.

**Usage**: `clj-lens.bb --trace "<stacktrace>"`

**Process**:
1. Parse stacktrace text (line by line)
2. Extract file + line number from each frame
3. Use rewrite-clj to read source code at each location
4. Return enriched frames with code snippets

**Example**:
```bash
clj-lens.bb --trace "java.lang.NullPointerException
  at app.core\$get_name (core.clj:42)
  at app.ui\$render (ui.clj:88)
  at ..."
```

**Output**:
```json
{
  "status": "ok",
  "mode": "trace",
  "data": {
    "frames": [
      {
        "class": "app.core",
        "method": "get_name",
        "file": "src/core.clj",
        "line": 42,
        "form": "(defn get-name [user] (:name user))"
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

## Output Format

All responses are JSON with a consistent envelope:

### Success
```json
{
  "status": "ok",
  "mode": "<mode-name>",
  "data": { ... mode-specific data ... }
}
```

### Suggestion (partial match)
```json
{
  "status": "suggestion",
  "message": "...",
  "matches": [ ... ]
}
```

### Error
```json
{
  "status": "error",
  "error": "<error-type>",
  "message": "Human-readable message",
  "details": { ... optional debug info ... }
}
```

## Error Handling

| Scenario | Status | Behavior |
|----------|--------|----------|
| Invalid mode | `error` | Clear message listing valid modes |
| Symbol not found | `suggestion` | Return partial matches if any exist |
| nREPL unavailable | `error` | Helpful message on how to start nREPL |
| File not readable | `error` | Message with file path + reason |
| clj-kondo unavailable | Fallback to file scan | Try to scan project anyway |
| Malformed stacktrace | `error` | Parse failure with details |

## Dependencies

### Required
- **rewrite-clj**: Already a dependency (used for coordinate reads)
- **Babashka**: Runtime environment

### Optional (with graceful degradation)
- **clj-kondo**: For efficient symbol indexing; falls back to file scanning if missing
- **nREPL**: For `--last-error` mode; that mode returns error if unavailable

## Implementation Notes

### clj-kondo Integration

Query clj-kondo analysis with:
```bash
clj-kondo export --format json
```

Parse the JSON output to build an in-memory symbol table for fast lookup/search.

### File Scanning Fallback

If clj-kondo is unavailable:
1. Walk the project directory (standard: `src/`, `test/`)
2. Parse each `.clj` file with rewrite-clj
3. Extract all top-level defs
4. Build the same symbol table

### nREPL Connection

Use the `.nrepl-port` file (created by Clojure/Babashka nREPL servers) to discover the port. Gracefully fail if the file doesn't exist.

## Testing Strategy

- **Unit tests**: Mock clj-kondo output, test mode handlers independently
- **Integration tests**: Real project with actual files, clj-kondo, and optional nREPL
- **Edge cases**: 
  - Empty projects
  - Missing files
  - Invalid stacktraces
  - Symbols in comments/strings (should not match)

---

## Future Enhancements

These observations are out of scope for the current iteration but worth exploring in future versions:

### 1. Cross-References Mode (`--refs`)
**Observation**: Agents often need to understand where a symbol is used, not just where it's defined. A `--refs <symbol>` mode could return all locations where a symbol is referenced, helping agents understand impact analysis and dependencies.

**Potential usage**: `clj-lens.bb --refs app.db/update-user` → all places that call this function

### 2. Namespace Overview Mode (`--ns`)
**Observation**: When entering a new namespace, agents need quick context: what are the public defs, what are the requires, what's the general structure? A `--ns <namespace>` mode could return a structured overview.

**Potential usage**: `clj-lens.bb --ns app.db` → list of all defs, requires, docstrings

### 3. Smart Analysis for last-error
**Observation**: The current `--last-error` analysis is basic (suspect variable detection). Future iterations could:
- Analyze function arities and detect arity mismatches
- Check for nil-returning paths
- Suggest common fixes based on error patterns

### 4. Context-Aware Symbol Lookup
**Observation**: When an agent encounters an unqualified symbol (e.g., `update-user` without namespace), it needs to know which namespace it should resolve to. Could add a `--symbol-in-context <file> <line>` mode that resolves symbols within a specific file's namespace context.

---

## Success Criteria

- ✅ All modes return consistent JSON format
- ✅ Agents can parse output reliably (no mixed formats)
- ✅ clj-kondo queries complete in < 100ms (single export)
- ✅ File reads with rewrite-clj work for all S-expression types
- ✅ Graceful fallback when optional dependencies unavailable
- ✅ Clear error messages guide users when something fails
