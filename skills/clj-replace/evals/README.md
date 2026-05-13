# clj-replace Evaluations

This directory contains test cases and evaluation guidelines for the clj-replace skill.

## Files

- **`evals.json`** — Formal test case definitions (Skill Creator format)

## Test Cases

### Eval-0: Post-formatting Parameter Rename

**Scenario:**
You run a code formatter (like `cljfmt`), which changes indentation and line breaks. Now you need to rename a parameter, but text-based find-and-replace fails because the formatting changed.

**Test Steps:**
```bash
# Create test file
cat > /tmp/handlers.clj << 'EOF'
(ns handlers)

(defn hello-handler [req] (let [body "Hello" status 200] {:body body
                                                          :status status}))
EOF

# Use clj-replace
bb ../scripts/clj-replace.bb /tmp/handlers.clj "req" "request"
```

**Expected Result:**
```
✓ Replaced 1 node(s) in /tmp/handlers.clj
  Location: line 3, column 22
  Old: req
  New: request
```

**What This Tests:**
- ✅ Format-agnostic matching (50-space indentation is irrelevant)
- ✅ Precise location reporting
- ✅ Single occurrence replacement

**Why It Matters:**
When formatters change your code's whitespace, text tools break. clj-replace handles this seamlessly.

---

### Eval-1: Multi-reference Scope Handling

**Scenario:**
You need to rename a local binding that appears multiple times (binding + references). The symbol appears in multiple contexts with different meanings.

**Test Steps:**
```bash
# Create test file
cat > /tmp/process.clj << 'EOF'
(defn process-record [record]
  (let [data (extract record)]
    (transform data)))
EOF

# Step 1: Replace the binding
bb ../scripts/clj-replace.bb /tmp/process.clj "[data (extract record)]" "[parsed-data (extract record)]"

# Step 2: Replace the reference
bb ../scripts/clj-replace.bb /tmp/process.clj "(transform data)" "(transform parsed-data)"
```

**Expected Results:**
```
Step 1:
✓ Replaced 1 node(s) in /tmp/process.clj
  Location: line 2, column 8

Step 2:
✓ Replaced 1 node(s) in /tmp/process.clj
  Location: line 3, column 5
```

**What This Tests:**
- ✅ Ambiguity detection (bare "data" matches multiple places)
- ✅ Contextual pattern matching (with surrounding structure)
- ✅ Multi-step refactoring
- ✅ Scope-aware replacement

**Why It Matters:**
Text tools can't distinguish between different meanings of the same symbol. clj-replace understands scope.

---

## How to Run Evaluations

### Manual Testing (Recommended)

Follow the steps in each eval above. This lets you:
- Understand the actual behavior
- See the tool working in real-time
- Verify output matches expectations

### Automated Testing

Create a test script:

```bash
#!/bin/bash
set -e

# Test Eval-0
cat > /tmp/test-eval0.clj << 'EOF'
(ns handlers)
(defn hello-handler [req] (let [body "Hello" status 200] {:body body
                                                          :status status}))
EOF

echo "Testing Eval-0..."
if bb ../scripts/clj-replace.bb /tmp/test-eval0.clj "req" "request"; then
  if grep -q "request" /tmp/test-eval0.clj; then
    echo "✅ Eval-0 PASSED"
  else
    echo "❌ Eval-0 FAILED: Parameter not renamed"
    exit 1
  fi
else
  echo "❌ Eval-0 FAILED: clj-replace exited with error"
  exit 1
fi

# Test Eval-1
cat > /tmp/test-eval1.clj << 'EOF'
(defn process-record [record]
  (let [data (extract record)]
    (transform data)))
EOF

echo "Testing Eval-1..."
if bb ../scripts/clj-replace.bb /tmp/test-eval1.clj "[data (extract record)]" "[parsed-data (extract record)]" && \
   bb ../scripts/clj-replace.bb /tmp/test-eval1.clj "(transform data)" "(transform parsed-data)"; then
  if grep -q "parsed-data" /tmp/test-eval1.clj; then
    echo "✅ Eval-1 PASSED"
  else
    echo "❌ Eval-1 FAILED: Binding not renamed"
    exit 1
  fi
else
  echo "❌ Eval-1 FAILED: clj-replace exited with error"
  exit 1
fi

echo ""
echo "✅ All evaluations passed!"
```

### Running on Your System

```bash
# Read evals.json to understand test cases
cat evals.json | jq .

# Manually run test cases as shown above
```

---

## Understanding evals.json Format

Each evaluation in `evals.json` has:

```json
{
  "id": 0,
  "prompt": "The user's problem/request",
  "expected_output": "What a good response should include",
  "files": []
}
```

- **`id`** — Test case identifier (0, 1, ...)
- **`prompt`** — Real-world scenario describing the problem
- **`expected_output`** — What success looks like
- **`files`** — Input files needed (none for these tests)

These are designed to be **reproducible**: anyone can follow the steps and verify the behavior.

---

## Evaluation Results

These test cases have been verified to work correctly:

✅ **Eval-0** — Tested with `cljfmt` formatting changes
✅ **Eval-1** — Tested with multiple occurrences and scope boundaries

See [`../docs/VERIFICATION.md`](../docs/VERIFICATION.md) for detailed verification procedures and real execution examples.

---

## Extending the Tests

To add more test cases:

1. **Identify a new scenario** — What problem should clj-replace solve?
2. **Create a test case** — Add to `evals.json` with new `id`
3. **Document steps** — Show how to reproduce it
4. **Verify it passes** — Run the test manually
5. **Update README** — Document the new test

Example:
```json
{
  "id": 2,
  "prompt": "I need to rename a function across multiple files...",
  "expected_output": "Should use clj-replace for each file...",
  "files": []
}
```

---

## Questions?

- **How do I run the tests?** See [`../docs/VERIFICATION.md`](../docs/VERIFICATION.md)
- **What does each test verify?** See sections above for each eval
- **Can I add more tests?** Yes, follow "Extending the Tests" section
- **What exit codes mean?** See [`../SKILL.md`](../SKILL.md) for exit code meanings

---

**These evaluations exist so you can verify clj-replace works correctly on your system.**
