# Testing Guidelines

## Overview

This file covers testing standards that apply across all languages and technologies. These guidelines ensure consistent, reliable, and maintainable test suites.

## Principles

### Principle 1: Test-First Development

**Why**: Writing tests first ensures testable designs and clear requirements.

**Rule**:
- Write failing tests before implementation
- Tests define the expected behavior
- Bugs require a failing test before the fix

### Principle 2: Test Pyramid

**Why**: Different test types serve different purposes with different cost/benefit ratios.

**Rule**: Follow the test pyramid:
- **Unit tests** (many): Fast, isolated, test single units
- **Integration tests** (some): Test component interactions
- **E2E tests** (few): Test complete user journeys

### Principle 3: Test Isolation

**Why**: Tests that depend on each other are fragile and hard to debug.

**Rule**:
- Each test must be independently runnable
- Tests must not share mutable state
- Test order must not affect results
- Clean up test data after each test

### Principle 4: Deterministic Tests

**Why**: Flaky tests erode trust in the test suite.

**Rule**:
- No random data without fixed seeds
- Mock external dependencies
- Control time-dependent behavior
- Avoid race conditions in async tests

### Principle 5: Meaningful Assertions

**Why**: Good assertions make failures easy to diagnose.

**Rule**:
- Assert specific expected values, not just "not null"
- Include descriptive messages in assertions
- One logical assertion per test (may be multiple statements)
- Test behavior, not implementation details

### Principle 6: Test Coverage Requirements

**Why**: Coverage metrics ensure adequate testing without being the only goal.

**Rule**:
- Minimum 80% code coverage for new code
- 100% coverage for critical business logic
- Coverage alone is insufficient - tests must be meaningful

### Principle 7: Fast Feedback

**Why**: Slow tests discourage running them frequently.

**Rule**:
- Unit tests should complete in milliseconds
- Integration tests should complete in seconds
- Full suite should complete in minutes, not hours
- Parallelize where possible

## Test Organization

```
tests/
├── unit/           # Fast, isolated unit tests
├── integration/    # Component integration tests
├── contract/       # API contract tests
└── e2e/            # End-to-end tests (if applicable)
```

## Checklist

- [ ] Tests written before implementation
- [ ] Tests are isolated and deterministic
- [ ] Test pyramid followed (more unit, fewer E2E)
- [ ] Code coverage meets minimum thresholds
- [ ] Tests run quickly (CI completes in reasonable time)
- [ ] Assertions are meaningful and specific
- [ ] No flaky tests in the suite
