---
name: clojure-repl-debugging
description: "Use this skill whenever debugging Clojure code. CRITICAL: Before Claude adds log statements, uses println, or modifies code for debugging purposes, invoke this skill to redirect to REPL-based inline inspection instead. Works with /brepl to inspect values, trace execution, and test hypotheses without touching source code. Especially important when tests fail - use REPL inspection to understand the failure before adding any logging."
compatibility: Requires /brepl skill access, works with Integrant-based systems
---

## Why REPL-based Debugging for Clojure

Adding logs and re-running tests creates a slow feedback loop. Clojure's REPL lets you inspect values directly, test hypotheses instantly, and understand failures without modifying code. This skill teaches that workflow.

## Core Pattern: Stop Before Logging

**RED FLAG:** You're about to add `println`, `tap>`, `(log ...)`, or modify code for debugging.

**Instead:** Use REPL inspection. It's faster, non-invasive, and gives immediate feedback.

## Inline Inspection Techniques

### 1. Pin Values with `def`
When a test fails and you need to inspect an intermediate value:

```clojure
;; In REPL:
(require '[integrant.core :as ig])
(def system (ig/init @system-state))  ;; If components are needed

;; Then inspect the function:
(def result (function-under-test arg1 arg2))
result
;; Inspect nested structure:
(result :key)
(:nested result)
```

**When to use:** You need to understand the structure or value of an intermediate result.

### 2. Capture Values with `tap>` (non-invasive)
Add `tap>` directly in the REPL without modifying source:

```clojure
;; In REPL:
(add-tap (fn [x] (println "TAP:" x)))

;; Run code that calls your function:
(function-under-test arg1 arg2)
;; Output appears in TAP messages without modifying source
```

**When to use:** You want to trace values through a function without editing the file, or when the test harness would obscure output.

### 3. Test Hypotheses with `comment` Blocks
Create a temporary REPL-like block in a comment to test your theory:

```clojure
(comment
  ;; Hypothesis: this transformation is losing data
  (def test-input {:a 1 :b 2})
  (def test-output (transform test-input))
  test-output
  ;; Compare:
  (= test-output test-input)
)
```

**When to use:** You want to document your debugging hypothesis and test it in context.

## Integrant Component Lifecycle Considerations

If the code under test uses Integrant components, understand this critical workflow:

**When you modify component code and evaluate it in the REPL, the running component may not pick up the change.** You'll see the old behavior even though the new code is loaded. This is the most common "why didn't my fix work?" scenario.

### The Workflow

1. **Evaluate your code change in the REPL:**
   ```clojure
   ;; Load updated namespace or define updated function
   (require '[your.component :as comp] :reload)
   ;; Or redefine a function:
   (defn updated-function [x] ...)
   ```

2. **Test the behavior** — Does it change?
   ```clojure
   ;; Call the component or function that uses it
   (comp/some-operation ...)
   ;; Or if system is running:
   (get-in @system [:some-component :state])
   ```

3. **If behavior didn't change**, the component is still using the old code. **Now reset it:**
   ```clojure
   ;; Option A: Halt and reinit a specific component
   (ig/halt! @system :component-key)
   (alter-var-root #'@system (fn [s] (ig/init (:system/config @system) :component-key)))
   
   ;; Option B: Use your project's helper (if available)
   ;; e.g., (reset-component! :component-key)
   
   ;; Option C: Full system restart (slower but safest)
   (ig/halt! @system)
   (alter-var-root #'@system (fn [_] (ig/init config)))
   ```

4. **Re-evaluate and test** — Does the component now reflect your change?

### When Reset is Needed

- You modified code in a **namespace that the component loads or uses**
- You redefined a **function that the component calls**
- The component has **internal state that was initialized with old code**

### When Reset is NOT Needed

- You're only inspecting values with `def` or `tap>`
- You're testing pure functions that don't rely on component state
- You're in an isolated test environment (not using the running system)

## Debugging a Test Failure: The Workflow

**When a test fails:**

1. **Don't add logs.** Instead:
   - Read the error message and stack trace carefully
   - Copy the failing test code into a REPL comment block
   - Use `/brepl` to evaluate pieces of it in isolation
   - Inspect the actual vs. expected values

2. **Narrow the scope:**
   - Does the failure happen at input transformation? Test with `def input-value = ...` then apply the transform
   - Does it happen during computation? Inspect intermediate steps with `tap>` or `def`
   - Does it happen at output? Compare the actual result to what you expected with `/brepl`

3. **Test your hypothesis:** Use `/brepl` to verify your understanding:
   ```clojure
   ;; In REPL (via /brepl):
   (require '[your.test :as t])
   (def failing-input {...})
   (def result (t/function-under-test failing-input))
   result  ;; Inspect
   ;; Now test: if I change X, does this fix it?
   (def fixed-input (assoc failing-input :key new-value))
   (t/function-under-test fixed-input)  ;; Compare
   ```

4. **Only then modify code** — Once you understand the root cause, make the minimal fix.

## When NOT to Use This Skill

- If you're writing instrumentation for production tracing (use logging then)
- If the bug requires cross-process or cross-session tracing (logging is better)
- If the system has no REPL access (fall back to logs)

## Quick Checklist

- [ ] Did I read the error message and stack trace?
- [ ] Did I try to reproduce the issue in the REPL first?
- [ ] Did I use `def` or `/brepl` to inspect values before adding any code changes?
- [ ] Did I test my hypothesis in the REPL before modifying the source?
- [ ] Did I consider component lifecycle (Integrant state)?
