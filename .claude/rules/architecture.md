# Architecture Guidelines

## Overview

This file covers architectural patterns, data retention strategies, cross-repository coordination, and design-first workflow principles. These guidelines apply to all repositories in the VMO system.

## Principles

### Principle 1: Design-First Approach

**Why**: Architectural decisions made upfront prevent costly rework and ensure system-wide consistency.

**Rule**: All architectural changes must be documented in PlantUML diagrams before implementation. The diagram is the single source of truth.

**Process**:
1. Update the design diagram first
2. Review and approve the design
3. Implement changes across affected repositories
4. Verify implementation matches design

### Principle 2: Single Source of Truth

**Why**: Multiple sources of truth lead to inconsistencies and confusion.

**Rule**: When code and design diagrams conflict, the diagram is authoritative. Code must be updated to match the design.

### Principle 3: Data Retention Strategy

**Why**: Unbounded data growth leads to performance degradation and storage costs.

**Rule**: Implement appropriate retention strategies based on data type:

| Data Type | Strategy | Description |
|-----------|----------|-------------|
| Operational snapshots | Upsert | Keep only latest state per entity |
| Event streams | Delete-before-insert | Replace old with new, cascade deletes |
| Audit logs | Append-only | Permanent retention for compliance |
| Configuration | Update-in-place | Preserve with change history |

**VMO-Specific Examples**:

| Table | Strategy | Retained Data |
|-------|----------|---------------|
| Heartbeat | Upsert (INSERT ON CONFLICT UPDATE) | Latest heartbeat per machine |
| VPN Report | Delete + Insert | Latest report per machine |
| Connections/Sessions | Cascade delete | Data from latest report only |
| Admin Action Log | Append-only | Permanent audit trail |
| Aliases (vm, user, vpn) | Permanent | Configuration records |
