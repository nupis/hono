---
description: Execute tasks using parallel Agent Teams for faster multi-stream implementation
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

**Supported arguments**:
- `--dry-run`: Preview stream detection and team structure without executing
- `--streams N`: Limit parallel agents to N streams (default: auto-detect)
- `--require-plan-approval`: Require user approval of plans before implementation

## Prerequisites Check

Before proceeding, verify Agent Teams are enabled:

1. Check if `~/.claude/settings.json` contains `"CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"`
2. If not enabled, **STOP** and inform user:
   ```
   Agent Teams not enabled. Add to ~/.claude/settings.json:
   { "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1" }
   ```

## Outline

1. Run `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks` from repo root and parse FEATURE_DIR and AVAILABLE_DOCS list. All paths must be absolute.

2. **Minimum Task Threshold Check**:
   - Count unchecked tasks (`- [ ]`) in tasks.md
   - If fewer than 5 unchecked tasks:
     - **STOP** and suggest: "Only {N} tasks remaining. Use `/speckit.implement` for sequential execution (lower overhead)."
   - If 5+ unchecked tasks, proceed

3. **Check checklists status** (if FEATURE_DIR/checklists/ exists):
   - Same validation as `/speckit.implement`
   - Display checklist table and prompt if incomplete

4. Load implementation context:
   - **REQUIRED**: Read tasks.md for complete task list
   - **REQUIRED**: Read plan.md for tech stack and architecture
   - **IF EXISTS**: Read data-model.md, contracts/, research.md

5. **File-Conflict Analysis** (Stream Detection):

   Build a conflict graph by analyzing file paths in task descriptions:

   a. **Extract file paths** from each task:
      - Look for paths in task descriptions (e.g., `backend/src/`, `frontend/src/`)
      - Use two-segment path prefixes for grouping

   b. **Build conflict graph**:
      - Tasks touching overlapping file paths → must run sequentially (same stream)
      - Tasks with completely separate file paths → can run in parallel (different streams)
      - "Repo-wide" tasks with no file paths → assign to qa-security stream

   c. **Identify streams**:
      - Connected components in conflict graph become independent work streams
      - Name streams by dominant path prefix (e.g., `backend-dev`, `frontend-dev`, `e2e-tester`)

   d. **Create dependency graph**:
      - Frontend tasks wait for corresponding backend APIs
      - E2E tests wait for feature implementations
      - Later phases wait for earlier phases

6. **Display Stream Detection Results**:

   ```
   Stream Detection Results:
   ─────────────────────────────────────────
   ✓ backend-dev:   {N} tasks (backend/src/, migrations/, tests/)
   ✓ frontend-dev:  {N} tasks (frontend/src/)
   ✓ e2e-tester:    {N} tasks (frontend/e2e/)
   ✓ qa-security:   {N} repo-wide + verification tasks

   Dependencies:
   - frontend tasks wait for backend APIs
   - E2E tests wait for implementations
   ─────────────────────────────────────────
   ```

   - If `--dry-run` specified: Display results and **STOP**
   - Otherwise: Ask user to confirm team creation

7. **Hydrate Tasks**:
   - Convert SpecKit tasks into Claude tasks with proper dependencies
   - Preserve [P] markers for parallel execution within streams
   - Set up blockedBy relationships based on dependency graph

8. **Create Agent Team**:

   Spawn specialized agents using the Task tool with `spawnTeam`:

   **Implementation agents** (one per detected stream):
   - Assigned specific file path prefixes exclusively
   - Tools: Read, Write, Edit, Bash, Grep, Glob
   - Instructions: Read spec, plan, tasks; implement assigned tasks; run quality gates; commit after each task

   **QA/Security gatekeeper** (always present):
   - Never implements features, only verifies work
   - Tools: Read, Bash, Grep, Glob
   - Instructions: Run all quality gates (tests, linting, coverage); perform security reviews (OWASP Top 10, injection checks, credential scanning); report results after each phase

9. **Coordination Loop**:

   As lead agent, coordinate the team:

   a. **Task Assignment**:
      - Agents self-claim unblocked tasks from shared list
      - Each agent works on tasks matching their file ownership

   b. **Progress Monitoring**:
      - Track task completion via shared task list
      - Trigger qa-security verification after each phase

   c. **Dependency Management**:
      - Unblock dependent tasks as prerequisites complete
      - Manage cross-stream dependencies (e.g., frontend waits for backend)

   d. **Error Handling**:
      - Create targeted fix tasks when quality gates fail
      - After 3 repeated failures, escalate to user
      - Critical security issues pause execution for user decision

10. **Quality Gates** (auto-detected from project):

    - **Rust**: `cargo clippy -- -D warnings` + `cargo test`
    - **Node/React**: `npm test`, lint, E2E commands
    - **Python**: `pytest` or detected test runners
    - **Go**: `go vet ./...` + `go test ./...`
    - **Java**: `mvn test` or `gradle test`
    - **.NET**: `dotnet test`

    Each stream agent runs gates relevant to its files.
    qa-security runs comprehensive gates after each phase.

11. **Sync Back to SpecKit**:

    After final qa-security approval:
    - Update tasks.md: Change `- [ ]` to `- [x]` for completed tasks
    - Add completion note to plan.md with quality gate status
    - Create single commit with all metadata updates
    - Gracefully shut down all teammates

12. **Final Report**:

    ```
    Team Implementation Complete
    ─────────────────────────────────────────
    Streams:        {N} parallel agents
    Tasks:          {completed}/{total} completed
    Commits:        {N} created
    Quality Gates:  All passed ✓
    Security:       Verified ✓

    Wall-clock time: {duration}
    (Sequential estimate: {estimate})
    ─────────────────────────────────────────
    ```

## Terminal Controls (during execution)

- `Shift+Up/Down`: Navigate between teammates
- `Shift+Tab`: Toggle delegate mode
- `Enter`: View specific teammate's session
- `Escape`: Interrupt teammate's current turn
- `Ctrl+T`: Toggle shared task list display

## Error Recovery

- **Crashed teammate**: Tasks reset to pending; replacement agent spawned
- **Persistent quality failures**: After 3 failures, escalate to user
- **Critical security issues**: Pause execution; user decides remediation
- **Git conflicts**: Coordinated through lead (rare due to file ownership)

## Limitations (Experimental Feature)

- `/resume` and `/rewind` don't restore in-process teammates
- Task status may lag, temporarily blocking dependents
- Shutdown can be slow (agents finish current request first)
- One team per session; clean up before creating new team
- Teammates cannot spawn nested teams
- Split-pane mode requires tmux or iTerm2 (not VS Code integrated terminal)

Note: For fewer than 5 tasks or single-directory projects, use `/speckit.implement` instead for lower overhead.
