Run a two-phase planning → implementation handoff for a new feature.

**Feature to implement:** $ARGUMENTS

---

## Phase 1: Plan

Spawn a planning agent with this prompt (do not write any code yourself yet):

```
You are a software architect. Analyse the codebase and produce an implementation plan for the following feature:

FEATURE: $ARGUMENTS

Investigate:
- Relevant existing files, models, and patterns to follow
- Where new code should attach to existing code (file paths, function names, line numbers)
- Any schema or data model changes required

Return a structured plan with these exact sections:

## Summary
One paragraph describing the approach.

## Schema changes
List each table/model/type change with field names and types. Write "none" if not applicable.

## New files
List each file to create with a one-line description of its responsibility.

## Files to modify
For each file: path, the specific function or line range, and exactly what to add or change.

## Open questions
List any ambiguities that need a decision before implementation begins. If none, write "none".

Do not write any code. Research and planning only.
```

---

## Phase 2: Review and decide

Present the plan output to the user. For each item in **Open questions**, ask the user for a decision and record their answers.

Once all questions are resolved, summarise the final decisions in a **Decisions** block.

---

## Phase 3: Implement

Spawn a second agent with this prompt, filling in `<PLAN>` and `<DECISIONS>` from the outputs above:

```
Implement the following feature exactly as specified in the plan below.
Do not deviate from the plan. If you encounter an unexpected blocker, stop and report it rather than improvising.

FEATURE: $ARGUMENTS

DECISIONS:
<DECISIONS>

PLAN:
<PLAN>

When done, run the test suite and report any failures.
```
