---
name: clj-discover
description: "When writing Clojure code with unfamiliar Java interop or macros, use this skill to explore and gather context. For Java interop, search for Clojure wrapper libraries first (via WebSearch), then explore Java classes if needed. For macros, expand them to understand what code they generate. This prevents bugs from incorrect API usage. Use this skill when encountering Java classes or unclear macros."
---

# Clojure Code Discovery

When writing Clojure code with unfamiliar APIs, gather context before writing. This skill focuses on **Java interop** and **macros**.

## When to Use This Skill

**Automatically use this skill whenever you encounter:**

- A Java class that needs to be used from Clojure
- A macro whose behavior is unclear (need to see what it expands to)

## Workflow: Java Interop

### 1. Search for Clojure Wrappers First

Before exploring the Java class directly, ask:

> "I see you're working with `java.time.LocalDateTime`. Would you like me to search for a Clojure wrapper library? Wrappers often provide simpler, more idiomatic APIs."

- **"Search for wrapper"** → Use WebSearch to find `clojure wrapper for <library-name>`
- **"Use Java directly"** → Skip to Java exploration below

### 2. Explore the Java Class

If using Java directly, use brepl to reflect on the class:

```bash
brepl <<'EOF'
(require '[clojure.reflect :as r])

; Get all public methods
(->> (r/reflect java.time.LocalDateTime)
     :members
     (filter #(instance? clojure.reflect.Method %))
     (map :name)
     sort)
EOF
```

Document findings:
- Available constructors or factory methods (e.g., `now()`, `of()`)
- Key methods and their return types
- Common patterns in your codebase

## Workflow: Macro Exploration

When working with macros, expand them to understand what code they generate:

```bash
brepl <<'EOF'
(macroexpand-1 '(your-macro args))     ; expand one level
(macroexpand '(your-macro args))       ; fully expand
EOF
```

Macro expansion reveals:
- What actual code runs when you use the macro
- Hidden function calls or side effects
- Whether the macro does what you think it does
