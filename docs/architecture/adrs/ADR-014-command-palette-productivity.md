# ADR-014: Command Palette Productivity (`Cmd+K`)

* **Status:** Accepted

## Context
Power users and platform engineers require rapid keyboard-driven navigation across repositories, packages, and settings.

## Decision
Provide a global command palette triggered via `Cmd+K` / `Ctrl+K`, enabling instant fuzzy search across coordinates, virtual repositories, and workspace settings.

## Consequences

### Positive
- Delivers a desktop-class keyboard workflow for power users, returning global search results in sub-100ms.

### Negative
- Requires managing client-side search index caching and debouncing logic.

## Non-Negotiable Invariants
- Command palette search must return global coordinate results in $\le 100\text{ ms}$.
