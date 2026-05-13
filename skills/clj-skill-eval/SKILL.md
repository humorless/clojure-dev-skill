---
name: clj-skill-eval
description: Evaluate Clojure skills through rigorous real-project testing with and without skills enabled. Use this skill whenever you need to assess whether a Clojure skill (like clj-discover, clj-debug, clj-replace) actually improves development outcomes. Compare behavior differences via context-logs, code quality, documentation depth, and test comprehensiveness. This prevents vague or hypothetical evaluation and forces concrete real-world testing.
---

# Clojure Skill Evaluation Framework

Evaluating Clojure skills requires **real projects with measurable behavior differences**, not hypothetical assumptions. This skill enforces a rigorous evaluation methodology that compares with-skill vs without-skill development on identical tasks.

## Core Principle

**Skills only matter if they change actual behavior.** 

The evaluation method:
1. Create two isolated projects (with/without skills)
2. Execute identical real development tasks in both
3. Compare: code quality, development time, documentation depth, test comprehensiveness
4. Analyze context-logs to observe behavioral differences (REPL usage, exploration patterns, debugging approach)

---

## Phase 1: Evaluation Planning

### Define the Skill(s) to Evaluate

**Input:** 
- Skill name and path (e.g., `clj-discover`, `clj-debug`)
- What behavior should the skill change? (e.g., "Should shift from log-based debugging to REPL-based debugging")

**Output:**
- Clear evaluation hypothesis (not vague assumptions)
- List of observable behavioral differences to measure

### Design a Realistic Development Task

**Requirements:**
- **Real project scenario** - Not toy examples or contrived problems
- **Concrete requirements** - Clear success criteria, not ambiguous specs
- **Medium complexity** - Enough to showcase skill value, not trivial (5-15 min per version)
- **Type diversity** - Different task types reveal different skill strengths:
  - **Discovery task**: "Implement feature using unfamiliar library/API" (tests clj-discover)
  - **Debugging task**: "Fix a bug in existing system" (tests clj-debug)
  - **Exploration task**: "Understand and modify macro behavior" (tests clj-discover)
  - **Integration task**: "Use Java library via interop" (tests clj-discover)

**Example Task:**
```
Implement GET /system-stats API endpoint that returns CPU usage and memory info.
- Use OSHI library for system statistics
- Return JSON with cpu-usage (0.0-1.0) and memory-info {total, available}
- Include at least one passing test
```

### Create Evaluation Criteria

**Quantitative:**
- Execution time (seconds, minutes)
- Token usage
- Lines of code
- Number of documentation files
- Test coverage

**Qualitative:**
- API choice (found optimal path or basic path?)
- Code quality (comments, clarity, type safety)
- Test defensiveness (what scenarios are covered?)
- Documentation depth (decisions explained or just results?)
- Behavioral patterns (REPL usage? Logs added? Files read?)

---

## Phase 2: Project Setup

### Create Two Isolated Projects

Use `neil` to generate identical Clojure Stack Lite projects:

```bash
neil new io.github.abogoyavlensky/clojure-stack-lite my-test-without-skill
neil new io.github.abogoyavlensky/clojure-stack-lite my-test-with-skill
```

Then in each:
```bash
cd my-test-without-skill && neil add nrepl
cd my-test-with-skill && neil add nrepl
```

**Why isolation?**
- No shared state or configuration
- Can run in parallel without interference
- Each gets clean Integrant system with (reset)

### Create Task Description

Create `TASK.md` in both projects with identical requirements:

```markdown
# Task: [Implementation Task Name]

## Objective
[Clear, concrete objective]

## Requirements
[Bullet points with specific requirements]

## Success Criteria
✓ Criterion 1
✓ Criterion 2
```

---

## Phase 3: Parallel Execution with Subagents

### Critical Permission Setting

When running subagents in Claude Code, **use `--dangerously-skip-permissions`** to avoid permission constraint blocks:

```bash
claude --dangerously-skip-permissions [your-claude-code-command]
```

**Why this is necessary:** Skills may use tools (brepl, WebSearch, file operations) that require explicit permission. Without this flag, subagents may abort with "permission constraints" instead of completing the task.

### Launch Both Versions in Parallel

Create two subagents simultaneously (same turn) to ensure comparable timing:

**Subagent 1 (WITHOUT skills):**
```
Execute this task in /path/to/my-test-without-skill:

1. Read TASK.md to understand requirements
2. Complete the implementation task
3. Save all final code files to outputs/
4. Record your approach in outputs/APPROACH.md:
   - How did you discover what to do?
   - What APIs/libraries did you choose and why?
   - What debugging/exploration did you do?
   - How much time was spent coding vs exploring?
5. DO NOT use any Clojure skills

Focus: Complete the task efficiently.
```

**Subagent 2 (WITH skills):**
```
Execute this task in /path/to/my-test-with-skill:

1. Read TASK.md to understand requirements
2. Complete the implementation task
3. Save all final code files to outputs/
4. Record your approach in outputs/APPROACH.md:
   - Which skills did you use and when?
   - How did each skill influence your decisions?
   - What exploration patterns did you use?
   - Did you use REPL-based debugging or log-based?
5. Use these Clojure skills as they become relevant:
   - clj-discover (understanding APIs, Java interop, macros)
   - clj-debug (interactive debugging via brepl)
   - clj-replace (structural code replacement)

Focus: Complete the task with skill guidance.
```

**Important:** Launch both in the same turn so timing is comparable.

---

## Phase 4: Deep Comparison Analysis

### Code Quality Comparison

Compare the generated code files (handlers, routes, tests):

**API Choices:**
- Did WITHOUT-skill take the first obvious path?
- Did WITH-skill find a more optimal/standard API path?
- Evidence of research vs. immediate coding?

**Type Safety:**
- Explicit type conversions or implicit assumptions?
- Comments explaining tricky parts?
- Error handling or happy path only?

**Test Defensiveness:**
- Basic assertions only or comprehensive coverage?
- Edge case handling?
- Business logic validation (e.g., available ≤ total)?

### Documentation Comparison

Compare the APPROACH.md and generated documentation:

**WITHOUT-skill should show:**
- "Direct implementation without exploration"
- Minimal decision-making explanation
- Quick success

**WITH-skill should show:**
- "Researched library before implementing"
- "Tested in REPL to verify approach"
- Clear decision rationale for API choices
- More comprehensive documentation

### Behavioral Pattern Analysis

From APPROACH.md, identify these patterns:

| Pattern | WITHOUT-skill | WITH-skill | Significance |
|---------|---|---|---|
| REPL usage | Absent/minimal | Frequent interactive testing | Indicates skill-enabled debugging |
| File reading | Scattered exploration | Targeted research | Skill guidance on what to look at |
| Debugging approach | Logs/println | Interactive brepl | Core behavior shift |
| Documentation | Minimal | Detailed with rationale | Skill encourages thorough thinking |

### Timing & Token Data

Capture from subagent notifications:
- Execution time (without-skill should be faster but lower quality)
- Total tokens (WITH-skill explores more)
- Time-to-quality ratio

---

## Phase 5: Drawing Conclusions

### Skill Effectiveness Scoring

For each skill, estimate:
- **Usage Strength**: How much did the skill actually activate? (⭐ scale)
- **Impact on Behavior**: Did it change development approach? (YES/NO)
- **Applicability**: For what task types is it valuable?

### Write Evaluation Report

Structure:
```markdown
# Evaluation Report: [Skill Name]

## Executive Summary
One sentence: Did the skill improve development outcomes?

## Task Executed
[Task description and success criteria]

## Key Findings

### Quantitative Comparison
- Time: [without-skill] vs [with-skill]
- Tokens: [without-skill] vs [with-skill]
- Code quality metrics

### Behavioral Differences
- API choices: [specific example]
- Testing approach: [specific example]
- Documentation depth: [specific example]

### Skill Activation Analysis
- Which skills were used: [list with ⭐ ratings]
- Permission issues: [if any]
- Unexpected patterns: [if any]

## Recommendations
- Is the skill valuable? [YES/NO with justification]
- In what scenarios is it most useful?
- What could be improved?
```

---

## Phase 6: Iteration & Refinement

If skill effectiveness is unclear:

1. **Design a task better suited to the skill**
   - If clj-discover wasn't triggered: add a task requiring exploration of unfamiliar APIs
   - If clj-debug wasn't used: add a task requiring interactive debugging

2. **Run evaluation again** with the new task

3. **Compare patterns** from multiple tasks to understand when/why the skill activates

---

## Anti-Patterns to Avoid

### ❌ DO NOT:
- Ask hypothetical questions ("Would this skill help with X?")
- Grade skills on "knowledge transfer" (they should change behavior, not just provide info)
- Accept "skill loaded but not executed" as success
- Run only simple tasks that don't showcase skill value
- Evaluate without comparing with a baseline

### ✅ DO:
- Run identical tasks in with/without conditions
- Measure actual behavioral changes (REPL usage, exploration patterns, debugging approach)
- Analyze why skills were or weren't triggered
- Test with multiple task types to find where skills shine
- Document specific code/documentation differences

---

## Task Type Reference

Use these to design varied evaluation tasks:

### 1. Discovery Task (triggers clj-discover)
"Implement feature using unfamiliar library"
- Example: "Add JWT authentication using auth0 library"
- Skill value: Helps navigate unfamiliar API without trial-and-error

### 2. Debugging Task (triggers clj-debug)
"Fix a bug in a multi-component system"
- Example: "Users report login failures; debug Integrant system lifecycle"
- Skill value: Shifts from log-based to interactive REPL debugging

### 3. Macro/Meta Task (triggers clj-discover)
"Understand and modify macro behavior"
- Example: "Extend defroutes macro to support new pattern"
- Skill value: Explains macro expansion and meta-patterns

### 4. Java Interop Task (triggers clj-discover)
"Integrate Java library"
- Example: "Add system monitoring via OSHI library" (what we tested)
- Skill value: Clarifies Java API navigation and method selection

---

## Remember

**A skill only matters if it changes how Claude develops code, not just what code it produces.**

If evaluation shows skills don't change behavior, that's valuable data — it means either:
1. The skill's triggering conditions need adjustment
2. The skill's content needs rethinking
3. The task isn't suited to the skill's strengths

All of these are solvable. But measuring behavior change is the only way to know.
