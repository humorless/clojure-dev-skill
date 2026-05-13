# Quick Start: clj-replace

Get up and running with clj-replace in 5 minutes.

## The Problem It Solves

You run a code formatter, and suddenly your find-and-replace tools don't work anymore:

```clojure
; Original code:
(defn hello [req] {:body "Hello"})

; After cljfmt:
(defn hello [req]
  {:body "Hello"})

; Now try to rename 'req' to 'request' with find-and-replace...
; ❌ FAILS because whitespace doesn't match
```

**clj-replace solves this** by comparing code structure, not text.

## Installation

### Option 1: Use Directly

```bash
bb /path/to/clj-replace.bb file.clj "old" "new"
```

### Option 2: Add to PATH

```bash
ln -s /path/to/clj-replace.bb ~/.local/bin/clj-replace
clj-replace file.clj "old" "new"
```

## Basic Usage

### Rename a Parameter

```clojure
; File: src/handlers.clj
(defn process-request [req]
  (validate req)
  {:result (handle req)})
```

**Command:**
```bash
bb clj-replace.bb src/handlers.clj "req" "request"
```

**Result:**
```
✓ Replaced 3 node(s) in src/handlers.clj
  Location: line 1, column 18
  Old: req
  New: request
```

✅ All three occurrences of `req` are now `request`.

### Rename a Local Binding

```clojure
; File: src/utils.clj
(defn extract-data [record]
  (let [data (parse record)]
    (transform data)))
```

**Problem:** Multiple `data` references. Need to be specific.

**Command:**
```bash
bb clj-replace.bb src/utils.clj "[data (parse record)]" "[parsed-data (parse record)]"
```

**Result:**
```
✓ Replaced 1 node(s) in src/utils.clj
  Location: line 2, column 8
  Old: [data (parse record)]
  New: [parsed-data (parse record)]
```

Then rename the reference:
```bash
bb clj-replace.bb src/utils.clj "(transform data)" "(transform parsed-data)"
```

**Result:**
```
✓ Replaced 1 node(s) in src/utils.clj
  Location: line 3, column 5
```

✅ Only the relevant occurrences changed. No scope violations.

## Common Tasks

### Task 1: Rename a Function

**Before:**
```clojure
(defn old-name [x] (process x))
(defn other [] (old-name 42))
```

**Command:**
```bash
bb clj-replace.bb file.clj "old-name" "new-name"
```

**After:**
```clojure
(defn new-name [x] (process x))
(defn other [] (new-name 42))
```

### Task 2: Update a Multi-line Expression

**Before:**
```clojure
(defn handler [request]
  (let [response (fetch-api request)]
    (format-response response)))
```

**Command:**
```bash
bb clj-replace.bb file.clj "(fetch-api request)" "(cached-fetch request)"
```

**After:**
```clojure
(defn handler [request]
  (let [response (cached-fetch request)]
    (format-response response)))
```

Works even with different indentation. ✅

### Task 3: Refactor After Formatter Runs

**Before (original code):**
```clojure
(defn handler [data] (process data))
```

**After running cljfmt:**
```clojure
(defn handler
  [data]
  (process data))
```

**Command:**
```bash
bb clj-replace.bb file.clj "data" "record"
```

**Works perfectly**, even though formatting changed. ✅

## When Ambiguity Occurs

If your `old-string` matches multiple places, clj-replace warns you:

**Example:**
```clojure
(let [x 1] (let [x 2] x))
```

**Command:**
```bash
bb clj-replace.bb file.clj "x" "y"
```

**Result:**
```
✗ Found 3 matching S-expressions (ambiguous)
  1. line 1, column 10: x
  2. line 1, column 20: x
  3. line 1, column 24: x
  
Please provide a more specific old-string (e.g., include surrounding context)
```

**Solution:** Be more specific:
```bash
bb clj-replace.bb file.clj "[x 2]" "[y 2]"
```

✅ Now unambiguous.

## Troubleshooting

### Error: "old-string not found"

**Cause:** The exact S-expression structure doesn't exist.

**Solution:** 
- Check that your code is valid Clojure
- Ensure you're using the exact same structure as in the file
- Try simpler patterns first

```bash
# Try this simpler pattern first:
bb clj-replace.bb file.clj "old" "new"

# Instead of matching complex nested structures:
bb clj-replace.bb file.clj "(complex (nested (structure old)))" "new"
```

### Error: "multiple matching S-expressions (ambiguous)"

**Cause:** Your pattern matches multiple places.

**Solution:** Make your `old-string` more specific by including context.

```bash
# Too generic:
bb clj-replace.bb file.clj "x" "y"

# More specific:
bb clj-replace.bb file.clj "[x (extract record)]" "[y (extract record)]"
```

### File changes but replacement looks wrong

**Cause:** Usually means you matched the right pattern but not the one you intended.

**Solution:** 
1. Verify with `--dry-run` (if available)
2. Use `git diff` to review the change
3. Undo with `git checkout` if needed
4. Try a more specific pattern

## Next Steps

- **[`VERIFICATION.md`](VERIFICATION.md)** — Run tests to verify clj-replace works
- **[`../SKILL.md`](../SKILL.md)** — Full technical documentation
- **[`../evals/evals.json`](../evals/evals.json)** — Test cases and examples

## Tips

1. **Start simple** — Use short patterns, expand if needed
2. **Test first** — Run on a small file to verify behavior
3. **Use git** — Commit before major refactoring
4. **Be specific** — Include context to avoid ambiguity
5. **Check results** — Always review what changed

---

Questions? See the full documentation in [`SKILL.md`](../SKILL.md) or run the verification tests.
