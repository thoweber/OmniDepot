# ADR-012: Material Design 3 and Dark-First Multi-Theming

* **Status:** Accepted

## Context
Enterprise users require accessible, modern user interfaces with dark mode support and high contrast for long monitoring sessions.

## Decision
Adopt Angular Material 3 (M3) utilizing CSS design tokens. Implement a default Dark Cyan / Carbon Gray theme meeting WCAG AA accessibility standards.

## Consequences

### Positive
- Out-of-the-box WCAG AA accessibility compliance, modern design language, and clean dark/light theme switching via CSS variables.

### Negative
- Material 3 design token setup adds initial styling setup complexity.

## Non-Negotiable Invariants
- All UI components must use M3 design tokens for colors, typography, and spacing.
