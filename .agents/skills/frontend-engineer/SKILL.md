---
name: frontend-engineer
description: Front-end architecture, UI/UX design system, Angular Material 3 theming, component guidelines, and Storybook protocols for omnidepot (@omni-depot/ui).
version: 1.0.0
tech_stack:
  framework: Angular (22+, Standalone, Signals)
  ui_library: Angular Material 3 (M3)
  architecture: Shared Component Library (@omni-depot/ui)
  workbench: Storybook with @storybook/addon-themes
  styling: SCSS + M3 CSS Design Tokens
---

# Front-End Architecture & Design System Skill: omnidepot

This document defines the core standards, visual identity, accessibility requirements, and component patterns for building the omnidepot Angular Web UI and its shared presentation library.

---

## 1. Visual Identity & Color Tokens

omnidepot uses a **dark-mode-first** visual identity designed for high-performance developer tools, paired with WCAG AA/AAA compliant light and high-contrast modes.

### A. Color Palette Matrix

| Token Role | Hex Code | Purpose / Context |
| :--- | :--- | :--- |
| **Primary (Dark)** | `#00D2FF` *(Reactive Cyan)* | Active states, main nav, primary actions in dark mode |
| **Primary (Light)** | `#0284C7` *(Sky Blue)* | Primary actions in light mode (WCAG AA compliant) |
| **Primary (High-Contrast)** | `#00FFFF` *(Electric Cyan)* | Luminescent primary element in pure black mode |
| **Background (Dark)** | `#0B0F17` *(Obsidian Slate)* | Root application canvas |
| **Background (Light)** | `#F8FAFC` *(Slate 50)* | Light theme application canvas |
| **Background (HC)** | `#000000` *(Pure Black)* | High-contrast application canvas |
| **Surface (Dark)** | `#161B26` *(Carbon Gray)* | Elevated cards, data tables, dialogs |
| **Surface (Light)** | `#FFFFFF` *(Pure White)* | Light theme card and modal surface |
| **Format Accent: OCI** | `#38BDF8` *(Docker Blue)* | OCI V2 badges, tags, and image metrics |
| **Format Accent: Maven**| `#F59E0B` *(Amber Gold)* | Java / Maven GAV artifact badges |
| **Format Accent: NPM**  | `#F87171` *(Coral Red)*   | Node / NPM package badges |
| **Status: Success/Cached**| `#10B981` *(Emerald Glow)* | Cache hits, verified digests, healthy proxies |
| **Status: Error/Warn**  | `#EF4444` *(Coral Red)*   | Sync errors, rate limits, storage exhaustion |

### B. Typography Standards
* **UI & Body:** `Inter, system-ui, -apple-system, sans-serif`
* **Code & Technical Data:** `JetBrains Mono, monospace` (used for digests, `sha256` hashes, terminal commands, and version tags)

---

## 2. Shared Library Architecture (`@omni-depot/ui`)

To ensure a strict separation between presentation and domain logic, all UI elements are isolated into a shared presentation library (`projects/omni-ui` or `@omni-depot/ui`).

### A. Core Architectural Rules
1. **Zero Domain Awareness:** Shared components must never import HTTP clients, state stores, or Quarkus REST endpoints.
2. **Signal-Based Inputs/Outputs:** Use modern Angular `input()` and `output()` API syntax.
3. **Change Detection:** All presentational components must use `ChangeDetectionStrategy.OnPush`.
4. **Token-Based Styling:** Style components using Angular Material 3 CSS variables (`var(--mat-sys-primary)`, `var(--mat-sys-surface-container)`). Never hardcode color hex values in library components.
5. **Public API Guarding:** Expose only public components and interfaces via `projects/omni-ui/src/public-api.ts`.

### B. Component Implementation Pattern

```typescript
// projects/omni-ui/src/lib/artifact-card/artifact-card.component.ts
import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';

export interface ArtifactCardData {
  name: string;
  format: 'OCI' | 'MAVEN' | 'NPM';
  version: string;
  digest: string;
}

@Component({
  selector: 'omni-artifact-card',
  standalone: true,
  imports: [MatCardModule, MatChipsModule],
  templateUrl: './artifact-card.component.html',
  styleUrl: './artifact-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArtifactCardComponent {
  data = input.required<ArtifactCardData>();
  copied = output<string>();

  onCopyDigest(): void {
    this.copied.emit(this.data().digest);
  }
}
```

---

## 3. Angular Material 3 & Multi-Theme Setup

Theming is managed at the root application level using scoped CSS classes (`.light-theme`, `.high-contrast-theme`) and Angular Material 3 Sass mixins.

### Global SCSS Configuration (`styles.scss`)

```scss
@use '@angular/material' as mat;

@include mat.core();

// 1. Define Palettes
$primary-dark: mat.define-palette(mat.$cyan-palette, 500);
$primary-light: mat.define-palette(mat.$light-blue-palette, 700);
$primary-hc: mat.define-palette(mat.$cyan-palette, A400);

// 2. Define Theme Configurations
$dark-theme: mat.define-theme((
  color: (theme-type: dark, primary: $primary-dark),
  typography: (plain-family: 'Inter, sans-serif')
));

$light-theme: mat.define-theme((
  color: (theme-type: light, primary: $primary-light),
  typography: (plain-family: 'Inter, sans-serif')
));

$high-contrast-theme: mat.define-theme((
  color: (theme-type: dark, primary: $primary-hc),
  typography: (plain-family: 'Inter, sans-serif')
));

// 3. Scoped Theme Targets
html {
  @include mat.theme($dark-theme);
  background-color: #0B0F17;
  color: #F8FAFC;

  &.light-theme {
    @include mat.theme($light-theme);
    background-color: #F8FAFC;
    color: #0F172A;
  }

  &.high-contrast-theme {
    @include mat.theme($high-contrast-theme);
    background-color: #000000;
    color: #FFFFFF;
  }
}
```

---

## 4. Storybook Setup & Theme Switching Protocol

Shared UI components must be developed and visually validated inside Storybook using `@storybook/addon-themes`.

### Storybook Preview Configuration (`.storybook/preview.ts`)

```typescript
import type { Preview } from '@storybook/angular';
import { applicationConfig } from '@storybook/angular';
import { provideAnimations } from '@angular/platform-browser/animations';
import { withThemeByClass } from '@storybook/addon-themes';

import './styles.scss'; // Global M3 theme SCSS

const preview: Preview = {
  decorators: [
    applicationConfig({
      providers: [provideAnimations()],
    }),
    withThemeByClass({
      themes: {
        Dark: 'dark-theme',
        Light: 'light-theme',
        'High Contrast': 'high-contrast-theme',
      },
      defaultTheme: 'Dark',
    }),
  ],
};

export default preview;
```

---

## 5. Stakeholder UX Requirements

* **Command Palette (`Ctrl + K`):** Quick jump modal allowing developers to search across OCI image tags, Maven coordinates, and NPM packages.
* **One-Click Snippets:** Instant copy-paste controls for `docker pull`, `pom.xml` dependencies, and `npm install` statements.
* **Live Stream Indicators:** Real-time throughput metrics powered by WebSockets/SSE for active layer downloads and cache hit ratios.
