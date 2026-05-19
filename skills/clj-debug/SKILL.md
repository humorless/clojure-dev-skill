---
name: clj-debug
description: "Use when debugging Clojure or Babashka code, especially before adding log statements or println - redirects to REPL-based inline inspection instead"
compatibility: Requires /brepl skill access, works with Integrant-based systems
---

## Why REPL-based Debugging for Clojure

Adding logs and re-running tests creates a slow feedback loop. Clojure's REPL lets you inspect values directly, test hypotheses instantly, and understand failures without modifying code.

## Core Pattern: Stop Before Logging

**RED FLAG:** You're about to add `println`, `tap>`, `(log ...)`, or modify code for debugging.

**Instead:** Use REPL inspection. It's faster, non-invasive, and gives immediate feedback.

## Inline Inspection: Using `def`

When a test fails or you need to inspect an intermediate value:

```clojure
;; In REPL:
(require '[your.namespace :as ns])

;; Pin the value you want to inspect
(def result (ns/function-under-test arg1 arg2))

;; Inspect its structure
result
(:key result)
(keys result)
```

This is the primary technique. Use `def` to capture a value, then explore its structure with keyword access, `get`, `keys`, or other accessor functions.

## Integrant Component Lifecycle

If the code uses Integrant components, understand this: **When you modify component code and evaluate it in the REPL, the running component may not pick up the change.** You'll see the old behavior even though the new code is loaded.

**The fix:**
```clojure
;; After evaluating your code change:
(require '[your.component :as comp] :reload)

;; Reset the component to pick up the new code:
(ig/halt! @system :component-key)
(alter-var-root #'@system (fn [s] (ig/init (:system/config @system) :component-key)))

;; Now test again — the component reflects your change
```

**When to reset:** You modified code that the component loads or uses. **When NOT:** You're only inspecting values with `def` or testing pure functions.

## Debugging a Test Failure: The Workflow

**When a test fails:**

1. Read the error message and stack trace
2. Copy the failing test code into a REPL comment block
3. Use `/brepl` to evaluate pieces in isolation
4. Pin intermediate values with `def` and inspect them
5. Test your hypothesis: if I change X, does this fix it?
6. Only then modify the source code

## Quick Checklist

- [ ] Did I use `/brepl` to reproduce the issue first?
- [ ] Did I use `def` to inspect values before changing code?
- [ ] Did I consider component lifecycle (Integrant state)?
