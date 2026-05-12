---
name: clojure-discovery
description: "When writing Clojure code that uses unfamiliar APIs, use this skill to automatically explore and gather context. For Clojure functions/macros, use brepl to look up documentation, source, and behavior. For Java interop, search for Clojure wrapper libraries first (via WebSearch), then explore Java classes if needed. Insufficient context about API usage leads to bugs and incorrect implementations — this skill prevents that by automatically gathering the information you need. ALWAYS use this skill when encountering unfamiliar Clojure functions, macros, Java classes, or any API you're not completely confident about."
---

# Clojure Code Discovery

When writing Clojure code, lack of context about unfamiliar functions, macros, or Java classes leads to incorrect usage, type mismatches, and wasted time. This skill automatically explores APIs to gather the information you need, and proactively searches for Clojure wrapper libraries when doing Java interop.

## Quick Reference

**Need to understand a Clojure function?**
```bash
brepl <<'EOF'
(doc function-name)                      ; documentation
(source function-name)                   ; read source code
(:arglists (meta #'function-name))       ; all arities
EOF
```

**Need to understand a macro?**
```bash
brepl <<'EOF'
(macroexpand-1 '(your-macro args))       ; expand one level
(macroexpand '(your-macro args))         ; fully expand
EOF
```

**Need to understand a Java class?**
```bash
brepl <<'EOF'
(require '[clojure.reflect :as r])
(->> (r/reflect JavaClassName)
     :members
     (filter #(instance? clojure.reflect.Method %))
     (map :name)
     sort)
EOF
```

## When to Use This Skill

**Automatically use this skill whenever you encounter:**

- An unfamiliar Clojure function or macro in the code
- A Java class that needs to be used from Clojure
- Uncertainty about function signatures, arities, or behavior
- A need to understand what a macro expands to

This skill works best when you have brepl running (nREPL connection). If not available, ask the user to start their nREPL server first.

## Workflow: Java Interop

When you identify code that uses a Java class or library, follow this sequence:

### 1. Ask About Clojure Wrappers

Before exploring the Java class directly, ask the user:

> "I see you're working with `java.time.LocalDateTime` (or whatever the Java library is). Would you like me to search for a Clojure wrapper library first? Wrappers often make the code simpler and more idiomatic. For example, `clj-time` wraps java.time and provides Clojure-friendly APIs."

Give the user the choice:
- **"Search for wrapper"** → Use WebSearch to find `clojure wrapper for <library-name>` or `clojure <java-library-name>` (try variations)
- **"Use Java directly"** → Skip to Java exploration below

### 2. Evaluate Wrapper Options (if found)

If you find a wrapper library:

- Check if it's actively maintained and has good documentation
- Provide usage examples if available
- Recommend it clearly, with the rationale: "This wrapper provides simpler syntax and avoids raw Java interop patterns"

If no good wrapper exists, proceed to Java exploration.

### 3. Explore the Java Class (if using Java directly)

Use brepl to reflect on the Java class and understand its API:

```bash
brepl <<'EOF'
(require '[clojure.reflect :as r])

; Get all public methods and fields
(r/reflect java.time.LocalDateTime)

; Filter to just method names for readability
(->> (r/reflect java.time.LocalDateTime)
     :members
     (filter #(instance? clojure.reflect.Method %))
     (map :name)
     sort)

; Look at specific method signatures
(r/reflect java.time.LocalDateTime)
EOF
```

Document findings for the code you're about to write:
- Available constructors or factory methods (e.g., `now()`, `of()`)
- Key methods and their return types
- Common patterns used in the codebase already

## Workflow: Clojure Functions and Macros

### 1. Explore Function Documentation

When you encounter an unfamiliar Clojure function, gather context:

```bash
brepl <<'EOF'
(require '[clojure.repl :refer [doc source dir apropos find-doc]])

(doc map)                        ; function documentation and arity
(source filter)                  ; read the function source
(:arglists (meta #'reduce))      ; check all arities
EOF
```

Use these tools to understand:
- What the function does
- Its argument order and types
- What it returns
- Common usage patterns

### 2. Explore Macro Expansion

When working with macros, expand them to understand what code they generate:

```bash
brepl <<'EOF'
(macroexpand-1 '(your-macro args))                    ; expand one level
(macroexpand '(your-macro args))                      ; fully expand
(clojure.walk/macroexpand-all '(your-macro args))     ; expand all nested
EOF
```

Macro expansion reveals:
- What actual code runs when you use the macro
- Hidden function calls or side effects
- Whether the macro does what you think it does

### 3. Search for Related Functions

When you need to find functions by name or behavior:

```bash
brepl <<'EOF'
(require '[clojure.repl :refer [dir apropos find-doc]])

(dir clojure.string)             ; list all public vars in a namespace
(apropos "split")                ; search functions by name pattern
(find-doc "regular expression")  ; search docstrings for keyword
EOF
```

## Integration into Code Writing

After exploration, immediately:

1. **Use the information you gathered** when writing the code. For functions: use correct arities, argument order, and patterns. For macros: be aware of what they expand to. For Java: use the constructor/method names and return types you found.

2. **Test the code in the REPL** before saving. Verification catches mistakes early.

3. **If exploration reveals ambiguity**, ask the user for clarification rather than guessing.

## Why Discovery Matters

Clojure's REPL is a powerful tool for code exploration, but only if you use it. Without discovery:
- You might use the wrong function signature
- You might misunderstand what a macro does
- You might miss simpler wrapper libraries (Java interop specifically)
- Code that works locally might fail under different conditions

With discovery, you write code with confidence, knowing:
- The exact API you're calling
- What alternatives exist
- Whether simpler patterns are available

The cost is minimal (a few brepl calls or one web search), and the benefit is significant: fewer bugs, better code, shorter debugging cycles.
