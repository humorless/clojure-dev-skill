---
name: clojure-fast-read
description: Smart Clojure code reading strategy. Use this whenever you need to inspect a Clojure symbol, function, or definition - whether to understand runtime state, debug a recent change, or explore source code. This skill helps you choose between fast REPL queries (source-fn, doc, meta) vs file reading, and decide whether to inspect runtime or source. Speeds up code navigation by avoiding unnecessary full-file reads when targeted REPL inspection is faster.
---

# Clojure Fast Read

When reading Clojure code, you have two strategies with different tradeoffs:

1. **Full file read** - slower but gives complete context (imports, functions, patterns)
2. **REPL inspection** - fast, focused, but requires knowing the symbol name

This skill helps you choose the right approach.

## Decision Framework

**Did you just change this code or are you debugging runtime behavior?** → Use REPL inspection

**Are you exploring a new codebase or trying to understand design?** → Read the file

### Path A: Runtime State (Debug / Verify)

Use REPL inspection when:
- You just changed this code (verify edit took effect)
- You're debugging why X behaves wrong (need actual behavior)

**Has this symbol been evaluated in your current REPL session?**
- **If YES:** Use `source-fn` to see runtime definition, `doc` for docstring, `meta` for file location
- **If NO or UNSURE:** Check the file first; it's more authoritative for unevaluated symbols

**Key insight:** Runtime can differ from source — only REPL inspection shows what's actually running.

### Path B: Source Code (Exploration / Understanding)

**If you know the symbol name + namespace:**
```bash
(meta #'your.namespace/function-name)
; Get file location, then use sed to extract context
sed -n '45,60p' src/my/app/core.clj
```

Otherwise, read the whole file.

## Implementation: Which Tool?

Both **brepl** and **clj-nrepl-eval** work. Choose whichever is already set up:

### Using brepl
```clojure
brepl <<'EOF'
(require '[clojure.repl :refer [source-fn doc meta]])
(source-fn 'your.namespace/function-name)
(doc your.namespace/function-name)
(meta #'your.namespace/function-name)
EOF
```

### Using clj-nrepl-eval
```bash
clj-nrepl-eval '(source-fn (quote your.namespace/function-name))'
clj-nrepl-eval '(doc your.namespace/function-name)'
```

## Common Pattern: I Just Modified a Function

**Goal:** Verify my change took effect  
**Action:** 
1. Reload the namespace if needed
2. Use `source-fn` to see the current runtime definition
3. If it matches your change, you're done; if not, the old code is still loaded

