Scan the specified files or directory for opportunities to improve
mechanism/policy separation, following the principles below.

## What to Look For

Using Arne Brasseur's definitions:

**Mechanism** is code that is:
- Unopinionated and context-free
- Reusable across different business domains
- Easy to test in isolation
- Stable — changes slowly over time

**Policy** is code that is:
- Opinionated — reflects current business decisions
- Contextual — tied to a specific domain
- Expected to change frequently

The distinction is not in *what* the code does, but in *how* it is written.
The goal is to push code toward the edges of this spectrum.

## Diagnostic Heuristics

When assessing where code sits on the spectrum, ask:

- Does this code accept all its dependencies as explicit parameters,
  or does it rely on defaults, globals, or implicit context?
  (More explicit → more mechanism)

- If you needed to reuse this logic elsewhere, would you call it
  with different parameters, or copy-paste and modify it?
  (Parametric reuse → mechanism; copy-paste reuse → policy)

- Is this code expressing a stable technical fact, or a current
  business decision? Business decisions belong in policy.

- How hard-coded are the values? Hard-coded constants are a sign
  that policy has leaked into mechanism.

- Does this code prioritize convenience (easy) over explicitness
  (simple)? Convenience-oriented defaults are often policy leaking
  into mechanism.

## Your Task

For each file or module examined, report:

1. **Mixed code** — functions or modules where mechanism and policy
   are tangled together. Describe what is mixed and where the boundary
   should be drawn.

2. **Extraction candidates** — pieces of mechanism that are currently
   embedded in policy code and could be extracted into a separate
   module or library. For each candidate, explain why it qualifies
   as mechanism (context-free, stable, reusable).

3. **Misplaced policy** — business logic that has drifted into what
   should be low-level mechanism. Describe what assumption is hidden
   and where it belongs.

4. **Proposed refactoring** — for each finding, suggest a concrete
   refactoring. Show the before/after structure, not just the principle.

## Constraints

- Do not refactor prematurely. Only suggest extraction when a pattern
  appears in at least two distinct use cases.
- Preserve existing behavior. This is a structural refactor, not a
  rewrite of business logic.
- Keep suggestions grounded in the actual code. Do not propose
  abstractions that have no evidence in the current codebase.
