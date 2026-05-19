---
name: clj-replace
description: Use when replacing Clojure or Babashka code but str_replace fails due to formatting differences - compares S-expression structure instead
---

# clj-replace: Structural Clojure Code Replacement

A Clojure-aware code replacement tool that improves on Claude Code's built-in `str_replace` by comparing code structure (S-expressions) rather than literal text.

## When to Use

Use `/clj-replace` when:

- Claude Code's `str_replace` fails because formatting (whitespace, indentation, line breaks) differs between the `old_string` and target code
- You need to replace code but the exact whitespace/formatting doesn't match the source
- You want replacements to preserve the original file's formatting and style

**Example:** If `str_replace` fails with "old_string not found" but the code structure looks identical, switch to `clj-replace`.

## How It Works

Instead of comparing text character-by-character, `clj-replace` uses structural comparison:

1. Parses both the `old-string` and file contents into S-expression trees (zippers)
2. Walks the file and compares each node's structure (`=` on sexpr) with the target
3. Formatting differences (spaces, indentation, newlines) are **ignored** during comparison
4. When a match is found, replaces the node while **preserving** the original file's formatting

This means these are all equivalent:
```clojure
(foo bar)          ≡ (foo  bar)        ≡ (foo
                                           bar)
```

## Parameters

**Positional Arguments:**

1. `filename` — Path to the Clojure source file to modify
2. `old-string` — Code snippet to find (as a string)
3. `new-string` — Code snippet to replace it with (as a string)

## Usage

```
/clj-replace path/to/file.clj "old code here" "new code here"
```

## Examples

### Example 1: Whitespace Variation

**File content:**
```clojure
(defn update-user
  [db id attrs]
  (merge-user db id attrs))
```

**Command:**
```
/clj-replace src/app.clj "(merge-user db id attrs)" "(update-user-in-db db id attrs)"
```

Works even though file has different indentation. Replaces the exact form while preserving file's style.

### Example 2: Multi-line Form

**File content:**
```clojure
(some-function
  arg1
  arg2
  arg3)
```

**Command:**
```
/clj-replace src/app.clj "(some-function arg1 arg2 arg3)" "(other-function arg1 arg2 arg3)"
```

Matches regardless of how the original is formatted across lines.

## Exit Codes

- `0` — Success: one or more nodes replaced
- `1` — Error: file doesn't exist, or `old-string`/`new-string` are invalid Clojure
- `2` — Error: no matching S-expression found in the file
- `3` — Error: multiple matching S-expressions found (ambiguous); user must provide more specific `old-string`

## Output

On success:
```
✓ Replaced 1 node(s) in path/to/file.clj
  Location: line 42, column 5
  Old: (merge-user db id attrs)
  New: (update-user-in-db db id attrs)
```

If multiple matches:
```
✗ Found 3 matching S-expressions in path/to/file.clj (ambiguous)
  1. line 42, column 5: (foo bar)
  2. line 85, column 10: (foo bar)
  3. line 120, column 1: (foo bar)
Please provide a more specific old-string (e.g., include surrounding context)
```

## Implementation Details

Uses `rewrite-clj` library for:
- Format-preserving parsing and modification
- Structural comparison of S-expressions
- Safe node replacement that maintains whitespace and style

The tool respects Clojure's semantics: two expressions are equivalent if they parse to the same structure, regardless of superficial formatting.
