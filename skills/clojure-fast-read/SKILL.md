---
name: clojure-fast-read
description: Smart Clojure code reading strategy. Use this whenever you need to inspect a Clojure symbol, function, or definition - whether to understand runtime state, debug a recent change, or explore source code. This skill helps you choose between fast REPL queries (source-fn, doc, meta) vs file reading, and decide whether to inspect runtime or source. Speeds up code navigation by avoiding unnecessary full-file reads when targeted REPL inspection is faster.
---

# Clojure Fast Read

When reading Clojure code, you have two strategies with different tradeoffs:

1. **Full file read** - slower but gives complete context (imports, other functions, design patterns)
2. **REPL inspection** - fast, focused, but requires knowing the symbol name and whether it's been evaluated in the current session

This skill helps you choose the right approach and execute it efficiently.

## Decision Framework

### Step 1: Clarify Your Intent

Before reading, ask yourself: **What's my actual goal?**

| Goal | Best Strategy | Why |
|------|---------------|-----|
| **Debug runtime state** - verify a recent change, confirm current definition, trace a value | REPL inspection | Runtime reflects actual state; you already know what you're looking for |
| **Understand source design** - first time seeing this code, how do functions relate, what imports are there | Full file read | Need context and patterns |
| **Verify symbol exists** - is this function defined, what namespace | Meta + grep | Lowest cost check |

### Step 2: Choose Your Path

#### Path A: You Want Runtime State (Debug / Verify)

**Question:** Has this symbol been evaluated in your current REPL session?

**If YES:**
- Use **source-fn** to see the actual runtime definition
- Use **doc** to see docstring
- Use **meta** to see metadata + file location (filename, line number)

**If NO or UNSURE:**
- Check the file first (it's likely more authoritative for unevaluated symbols)
- Or evaluate it, then use source-fn

**Key insight:** Runtime can differ from source — your recent edits, repl-driven changes, or loaded code may be different from the file. Only REPL inspection shows what's actually running.

#### Path B: You Want Source Code (Exploration / Understanding)

**Question:** Do you know the symbol name?

**If YES (know name + namespace):**
1. Use **meta** to get `{:file "..." :line N}` — low cost, instant
2. Use sed to extract partial file content around that line: `sed -n '${START_LINE},${END_LINE}p' ${FILE}`
   - This gives you focused context without reading the entire file
   - Example: `sed -n '45,60p' src/my/app/core.clj` reads lines 45-60
3. Why not source-fn? source-fn shows runtime, which may differ from what's in the file; file reading shows the authoritative source

**If PARTIAL (know name, unsure of namespace):**
1. Use grep to find the definition: `grep -r "defn symbol-name" src/`
2. Identify the namespace
3. Read the file

**If NO (don't know where to start):**
1. Read the whole file

#### Path C: Unsure Whether to Inspect Runtime or Source?

Ask yourself:
- **Did I just change this code?** → Look at runtime to see if my edit took effect
- **Is this an old function I haven't touched?** → Look at source
- **Am I debugging why X behaves wrong?** → Look at runtime (actual behavior) 
- **Am I trying to understand the design?** → Look at source

## Implementation: Which Tool to Use?

Both **brepl** and **clj-nrepl-eval** work. Choose whichever is already set up:

### Using brepl
```clojure
brepl <<'EOF'
(require '[clojure.repl :refer [source-fn doc meta]])

; See source code
(source-fn 'your.namespace/function-name)

; See docstring
(doc your.namespace/function-name)

; See metadata (includes file location)
(meta #'your.namespace/function-name)
EOF
```

### Using clj-nrepl-eval
```bash
clj-nrepl-eval '(source-fn (quote your.namespace/function-name))'
clj-nrepl-eval '(doc your.namespace/function-name)'
clj-nrepl-eval '(meta #'"'"'your.namespace/function-name)'
```

## Common Scenarios

### Scenario 1: I Just Modified a Function

**Goal:** Verify my change took effect  
**Strategy:** Runtime  
**Action:** Have you reloaded/re-evaluated the namespace? If yes, use `source-fn` to see the current runtime definition. If no, reload first then check.

### Scenario 2: I'm Seeing Wrong Behavior and Need to Debug

**Goal:** Understand what the code is actually doing  
**Strategy:** Runtime + meta  
**Action:** 
1. Use `meta` to confirm you're looking at the right file location
2. Use `source-fn` to see the actual code running
3. If it matches the file, read the source for logic. If it doesn't, you've found a discrepancy (old vs new code)

### Scenario 3: First Time Exploring a Namespace

**Goal:** Understand the overall design  
**Strategy:** File  
**Action:** Read the whole file. You need to see imports, function organization, and how functions relate.

### Scenario 4: Need to Call a Function But Don't Know the Full Namespace

**Goal:** Find the symbol location  
**Strategy:** Meta + grep  
**Action:**
1. If you know the function name, grep for its definition
2. Once you find the namespace, use `meta` or `source-fn`

## When NOT to Use This Skill

- You're reading a file for the first time and need full context
- You don't have access to a running REPL
- The symbol is external (third-party library) and not in your session

In these cases, just read the file directly or check the library docs.

## Why This Matters

In a large Clojure codebase, reading files is expensive — you might read 500 lines to understand 10. REPL inspection lets you target exactly what you need. But it only works if you know the symbol name and understand whether you're looking for runtime state or source truth. This framework helps you make that call fast.
