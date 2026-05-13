# Verification: Running clj-replace Tests

This guide explains how to verify that clj-replace works correctly on your system.

## What Are These Tests?

clj-replace includes **evaluation tests** (`evals.json`) that demonstrate real-world scenarios:

1. **Eval-0:** Parameter rename after code formatter changes indentation
2. **Eval-1:** Renaming with multiple references (scope handling)

These tests show both:
- ✅ What clj-replace can do (WITH-skill scenarios)
- ❌ Why you need it (WITHOUT-skill scenarios)

## Prerequisites

You need:
- Clojure/Babashka environment
- `cljfmt` (optional, for realistic testing)
- Basic shell knowledge

## Quick Verification (5 minutes)

### Eval-0: Parameter Rename After Formatting

**Create test file:**
```bash
cat > /tmp/test-handlers.clj << 'EOF'
(ns handlers)

(defn hello-handler [req] (let [body "Hello" status 200] {:body body
                                                          :status status}))
EOF
```

**Verify clj-replace works:**
```bash
cd /tmp
bb /path/to/clj-replace/scripts/clj-replace.bb test-handlers.clj "req" "request"
```

**Expected output:**
```
✓ Replaced 1 node(s) in test-handlers.clj
  Location: line 3, column 22
  Old: req
  New: request
```

**Verify the change:**
```bash
cat test-handlers.clj | grep "request"
```

Should show the renamed parameter. ✅

---

### Eval-1: Multi-reference Scope Handling

**Create test file:**
```bash
cat > /tmp/test-process.clj << 'EOF'
(defn process-record [record]
  (let [data (extract record)]
    (transform data)))
EOF
```

**Step 1: Replace binding**
```bash
bb /path/to/clj-replace/scripts/clj-replace.bb /tmp/test-process.clj "[data (extract record)]" "[parsed-data (extract record)]"
```

**Expected output:**
```
✓ Replaced 1 node(s) in /tmp/test-process.clj
  Location: line 2, column 8
  Old: [data (extract record)]
  New: [parsed-data (extract record)]
```

**Step 2: Replace reference**
```bash
bb /path/to/clj-replace/scripts/clj-replace.bb /tmp/test-process.clj "(transform data)" "(transform parsed-data)"
```

**Expected output:**
```
✓ Replaced 1 node(s) in /tmp/test-process.clj
  Location: line 3, column 5
  Old: (transform data)
  New: (transform parsed-data)
```

**Verify final result:**
```bash
cat /tmp/test-process.clj
```

Should show both `data` renamed to `parsed-data` correctly. ✅

---

## Full Evaluation (20 minutes)

For a more comprehensive test, compare WITH-skill and WITHOUT-skill approaches:

### Setup

```bash
# Create test directories
mkdir -p /tmp/eval-with-skill /tmp/eval-without-skill

# Create identical test files
for dir in /tmp/eval-with-skill /tmp/eval-without-skill; do
  cat > $dir/handlers.clj << 'EOF'
(ns handlers)

(defn hello-handler [req] (let [body "Hello" status 200] {:body body
                                                          :status status}))
EOF
done
```

### With-Skill: clj-replace

```bash
cd /tmp/eval-with-skill
echo "=== WITH clj-replace ==="
time bb /path/to/clj-replace/scripts/clj-replace.bb handlers.clj "req" "request"
echo "=== RESULT ===" 
cat handlers.clj
```

**Expected:**
- ✅ Success on first try
- ✅ Exact location reported
- ✅ Fast execution
- ✅ Code modified correctly

### Without-Skill: Standard Tools

```bash
cd /tmp/eval-without-skill
echo "=== WITHOUT clj-replace (using sed) ==="
time sed -i 's/\[req\]/[request]/g' handlers.clj
echo "=== RESULT ===" 
cat handlers.clj
```

**Observations:**
- ⚠️ Worked, but required knowing the exact pattern
- ⚠️ Fragile (would fail if indentation changed differently)
- ⚠️ No feedback on what was replaced
- ⚠️ Would break on more complex scenarios

### Comparison

| Aspect | clj-replace | sed |
|--------|---|---|
| Success | ✅ Yes | ✅ Yes (this time) |
| Feedback | ✅ Precise location | ❌ None |
| Format-agnostic | ✅ Yes | ❌ No |
| Robust | ✅ Yes | ⚠️ Fragile |
| Scalable | ✅ Large codebases | ❌ Breaks at scale |

---

## Testing After Formatter Changes

This demonstrates the core problem clj-replace solves:

### Setup

```bash
mkdir -p /tmp/formatter-test && cd /tmp/formatter-test

cat > code.clj << 'EOF'
(defn process [data] (let [x (parse data)] (transform x)))
EOF

echo "=== Before cljfmt ===" 
cat code.clj
```

### Run Formatter

```bash
cljfmt fix code.clj

echo "=== After cljfmt ===" 
cat code.clj
```

Notice how `cljfmt` changed the formatting!

### Try WITH-Skill

```bash
bb /path/to/clj-replace/scripts/clj-replace.bb code.clj "data" "record"
```

**Result:** ✅ Works despite formatting changes

### Why This Matters

The key insight: clj-replace **doesn't care about formatting**. It understands that the code structure is the same, even if whitespace changed.

---

## Automated Testing (For CI/CD)

If you want to automate these tests:

### Script: `run-tests.sh`

```bash
#!/bin/bash
set -e

CLOJURE_FILE=/tmp/test.clj
SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts/clj-replace.bb"

# Test 1: Simple rename
cat > $CLOJURE_FILE << 'EOF'
(defn foo [x] x)
EOF

bb $SCRIPT_PATH $CLOJURE_FILE "x" "y"
if ! grep -q "y" $CLOJURE_FILE; then
  echo "FAIL: Test 1 (simple rename)"
  exit 1
fi
echo "PASS: Test 1 (simple rename)"

# Test 2: Multi-line expression
cat > $CLOJURE_FILE << 'EOF'
(defn foo
  [x]
  x)
EOF

bb $SCRIPT_PATH $CLOJURE_FILE "x" "y"
if ! grep -q "y" $CLOJURE_FILE; then
  echo "FAIL: Test 2 (multi-line)"
  exit 1
fi
echo "PASS: Test 2 (multi-line)"

# Test 3: No match
if bb $SCRIPT_PATH $CLOJURE_FILE "nonexistent" "replacement" 2>/dev/null; then
  echo "FAIL: Test 3 (should not match)"
  exit 1
fi
echo "PASS: Test 3 (correctly rejected non-match)"

echo "All tests passed!"
```

### Run It

```bash
bash run-tests.sh
```

---

## Understanding the Test Cases

See [`../evals/evals.json`](../evals/evals.json) for the formal test definitions:

- **Eval-0 `prompt`** — The user's problem
- **Eval-0 `expected_output`** — What a good response should include
- **Eval-1 `prompt`** — Second scenario
- **Eval-1 `expected_output`** — Expected handling

These define "what success looks like."

---

## Troubleshooting Verification

### Issue: "Command not found: bb"

**Solution:**
```bash
# Install babashka
brew install babashka  # macOS
# or
curl -sL https://raw.githubusercontent.com/babashka/babashka/master/install  # Linux
```

### Issue: "rewrite-clj not found"

**Solution:** The clj-replace script auto-installs dependencies via `babashka.deps/add-deps`. On first run, it will download the required libraries.

### Issue: Test reports "ambiguous"

**Solution:** Multiple matches found. Use a more specific pattern that includes context:

```bash
# Instead of:
bb clj-replace.bb file.clj "x" "y"

# Try:
bb clj-replace.bb file.clj "[x ...]" "[y ...]"
```

---

## What Passing Tests Means

If all tests pass:

✅ **Code structure matching works** — S-expression comparison is reliable

✅ **Format-agnostic replacement works** — Whitespace variations are handled

✅ **Location reporting works** — Line and column are accurate

✅ **Ambiguity detection works** — Multiple matches are caught

✅ **Formatting preservation works** — Original style is maintained

---

## Next Steps

- **[`QUICK-START.md`](QUICK-START.md)** — Use clj-replace for real tasks
- **[`../SKILL.md`](../SKILL.md)** — Full technical details
- **[`../README.md`](../README.md)** — Overview and features

---

## Questions or Issues?

If tests fail:
1. Check that Babashka is installed (`bb --version`)
2. Ensure Clojure code in test files is valid
3. Review the error message (exit code 1, 2, or 3)
4. Try a simpler test case first
5. Check [`../SKILL.md`](../SKILL.md) for troubleshooting

**You can verify clj-replace is working correctly.** These tests are designed to be run locally and independently confirmed.
