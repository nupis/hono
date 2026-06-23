# Tasks: Single Asset Filter

**Input**: Design documents from `/specs/003-single-asset-filter/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested — test tasks are included because the constitution (Principle III: Test-First Development) mandates tests alongside implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single module**: `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/` (production)
- **Tests**: `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/` (tests)
- **Config**: `adapters/edc/src/main/resources/application.yaml`

---

## Phase 1: Setup

**Purpose**: No new project setup needed — feature extends existing classes. This phase ensures the branch is ready and existing tests pass.

- [x] T001 Verify existing build passes: run `mvn clean test` from `adapters/edc/`
- [x] T002 Review current `EdcAdapterProperties.java` at `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterProperties.java` to confirm property pattern (field, getter, setter, validation)
- [x] T003 Review current `TelemetryPollingService.java` at `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java` to confirm insertion point after `queryCatalog()` and before `processAssetsSequentially()`

**Checkpoint**: Existing codebase builds and tests pass. Developer understands modification points.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add the configuration property that all user stories depend on.

**⚠️ CRITICAL**: US1 and US2 both depend on the `assetIdFilter` property existing.

- [x] T004 Add `assetIdFilter` field (nullable String, default null) with getter and setter to `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterProperties.java` — follow existing property pattern (private field, public getter/setter)
- [x] T005 Add validation logic in the setter: if value is non-null, trim it; if trimmed result is blank, set to null (treat blank as absent) in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterProperties.java`
- [x] T006 Add unit tests for `assetIdFilter` property in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcAdapterPropertiesTest.java` — test: null returns null, valid string returns trimmed value, blank/whitespace-only returns null, getter returns set value

**Checkpoint**: Configuration property exists, is validated, and tested. User stories can now begin.

---

## Phase 3: User Story 1 — Filter Polling to a Specific Asset (Priority: P1) 🎯 MVP

**Goal**: When `assetIdFilter` is configured, only the matching asset is processed during a polling cycle. No unnecessary contract negotiations occur for non-matching assets.

**Independent Test**: Configure adapter with `assetIdFilter=sensor-042`, run polling cycle against catalog with multiple assets, verify only `sensor-042` is processed.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T007 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is set to "sensor-042" and catalog returns ["sensor-001", "sensor-042", "sensor-099"], only "sensor-042" is passed to `processAssetsSequentially()`
- [x] T008 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is set to "sensor-042" and catalog returns ["sensor-001", "sensor-099"] (no match), `processAssetsSequentially()` receives an empty list and a warning is logged
- [x] T009 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is set, catalog query is still called (full catalog fetched), but downstream processing only receives the filtered asset

### Implementation for User Story 1

- [x] T010 [US1] Add `filterAssets(List<CatalogAsset> assets)` method to `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java` — if `assetIdFilter` is configured, filter list to matching asset; if no match, log WARN with asset ID, catalog size, and correlation ID; if filter is absent, return list unmodified
- [x] T011 [US1] Integrate `filterAssets()` into `executeCycle()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java` — call between `queryCatalog()` result and `processAssetsSequentially()`, update the INFO log to show "discovered X assets, processing Y after filtering"
- [x] T012 [US1] Verify tests from T007-T009 now pass: run `mvn test -pl adapters/edc -Dtest=TelemetryPollingServiceTest`

**Checkpoint**: Asset filtering works end-to-end. Configuring `assetIdFilter` restricts processing to one asset. This is the MVP.

---

## Phase 4: User Story 2 — No Filter Configured / Backward Compatibility (Priority: P1)

**Goal**: When no `assetIdFilter` is configured (absent, empty, blank), all catalog assets are processed exactly as before.

**Independent Test**: Start adapter with no `assetIdFilter` set, verify all catalog assets are processed unchanged.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T013 [P] [US2] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is null (not configured), all catalog assets are passed through to `processAssetsSequentially()` unmodified
- [x] T014 [P] [US2] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is set to empty string or whitespace, it is treated as absent and all assets are processed

### Implementation for User Story 2

- [x] T015 [US2] Verify that `filterAssets()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java` correctly handles null/absent filter by returning the full asset list — this should already work from T010 implementation; adjust if tests T013-T014 fail
- [x] T016 [US2] Verify tests from T013-T014 now pass: run `mvn test -pl adapters/edc -Dtest=TelemetryPollingServiceTest`

**Checkpoint**: Backward compatibility confirmed. Existing deployments without `assetIdFilter` continue to work identically.

---

## Phase 5: User Story 3 — Operational Visibility of Active Filter (Priority: P2)

**Goal**: Platform operators can see which asset filter is active via startup logs and polling cycle logs.

**Independent Test**: Configure an asset filter, start adapter, verify startup log shows filter; run a cycle, verify cycle log includes filter info.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T017 [P] [US3] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when `assetIdFilter` is configured, the polling cycle start log includes the asset filter value and correlation ID
- [x] T018 [P] [US3] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` — test that when no `assetIdFilter` is configured, the polling cycle start log indicates all assets will be processed

### Implementation for User Story 3

- [x] T019 [US3] Add startup logging in the adapter initialization — log INFO "Asset filter configured: assetId={}" or "No asset filter configured, processing all catalog assets" in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java` at `doStart()` after properties are loaded
- [x] T020 [US3] Update cycle start log in `executeCycle()` of `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java` — include active filter value (or "all") alongside the existing correlation ID
- [x] T021 [US3] Verify tests from T017-T018 now pass: run `mvn test -pl adapters/edc -Dtest=TelemetryPollingServiceTest`

**Checkpoint**: Operators have full visibility into filter configuration via logs at startup and each polling cycle.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Configuration documentation, final validation

- [x] T022 [P] Add `assetIdFilter` property (commented out, with description) to `adapters/edc/src/main/resources/application.yaml` as an optional configuration example
- [x] T023 [P] Run full test suite: `mvn clean verify -pl adapters/edc` to confirm no regressions
- [x] T024 Validate quickstart.md scenarios from `specs/003-single-asset-filter/quickstart.md` — verify log messages match expected format

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — adds the config property that BLOCKS all stories
- **US1 (Phase 3)**: Depends on Foundational — core filtering logic (MVP)
- **US2 (Phase 4)**: Depends on Foundational — backward compatibility (can run in parallel with US1)
- **US3 (Phase 5)**: Depends on Foundational — logging (can run in parallel with US1/US2)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2 only. No dependencies on other stories.
- **User Story 2 (P1)**: Depends on Phase 2 only. Validates US1's filter logic handles absent case, but is independently testable.
- **User Story 3 (P2)**: Depends on Phase 2 only. Adds logging to existing code. Independent of US1/US2 filtering correctness.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Implementation follows: filter method → integration into cycle → test verification
- Story complete before moving to next priority (or run in parallel)

### Parallel Opportunities

- T007, T008, T009 (US1 tests) can all run in parallel
- T013, T014 (US2 tests) can all run in parallel
- T017, T018 (US3 tests) can all run in parallel
- US1, US2, US3 can all start in parallel after Phase 2 (they modify the same file but different methods/sections)
- T022, T023 (Polish) can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together (write first, expect failures):
Task: "T007 - Unit test: filter matches single asset from multi-asset catalog"
Task: "T008 - Unit test: filter warns when asset not found in catalog"
Task: "T009 - Unit test: catalog query unaffected by filter config"

# Then implement sequentially:
Task: "T010 - Add filterAssets() method"
Task: "T011 - Integrate into executeCycle()"
Task: "T012 - Verify all US1 tests pass"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify build)
2. Complete Phase 2: Foundational (add config property + tests)
3. Complete Phase 3: User Story 1 (filter logic + tests)
4. **STOP and VALIDATE**: Test filtering with a real EDC catalog
5. Deploy if ready — single asset polling works

### Incremental Delivery

1. Setup + Foundational → Config property ready
2. Add US1 → Filter works → Deploy (MVP!)
3. Add US2 → Backward compatibility confirmed → Deploy
4. Add US3 → Operator visibility → Deploy
5. Polish → Configuration docs, final validation

### Parallel Team Strategy

With multiple developers after Phase 2 completes:
- Developer A: User Story 1 (filtering logic)
- Developer B: User Story 2 (backward compatibility tests) + User Story 3 (logging)
- Stories integrate cleanly — US1 modifies `filterAssets()`, US3 modifies log statements

---

## Notes

- [P] tasks = different files or independent sections, no dependencies
- [Story] label maps task to specific user story for traceability
- This feature is small: 2 production files modified, 0 new files created
- Total: **24 tasks** across 6 phases
- All modifications are in `adapters/edc/` module
- Commit after each completed user story phase
