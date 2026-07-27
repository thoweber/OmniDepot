# omnidepot: MCP Server Setup & Token Guide

This guide details the setup, credentials, environment variables, and permission scopes required to run the **Model Context Protocol (MCP)** servers integrated into **omnidepot**.

---

## Integrated MCP Servers Overview

The omnidepot repository configures four specialized MCP servers defined in [.mcp.json](file:///home/developer/projects/omnidepot/.mcp.json) and [.agents/config.json](file:///home/developer/projects/omnidepot/.agents/config.json):

```text
                                omnidepot MCP ECOSYSTEM
┌───────────────────────┬───────────────────────────────┬────────────────────────────────┐
│ MCP Server            │ Utility / Purpose             │ Authentication / Credentials   │
├───────────────────────┼───────────────────────────────┼────────────────────────────────┤
│ 1. github             │ Repository issues, PRs, CI    │ GITHUB_TOKEN (PAT)             │
│ 2. intellij           │ Live IDE AST & symbol navigation│ Direct SSE (127.0.0.1:64343)   │
│ 3. archunitChecker    │ Package boundary verification │ Node STDIO Server (.agents/scripts/archunit-checker-server.js) │
│ 4. liquibaseValidator │ DB changelog & rollback validation│ Node STDIO Server (.agents/scripts/liquibase-validator-server.js) │
└───────────────────────┴───────────────────────────────┴────────────────────────────────┘
```

---

## Recommended Usage: Extensive Refactoring & Complex Coding Tasks

The **`intellij` MCP server** is strongly recommended for **large-scale refactorings**, multi-module architecture updates, and type-safe code restructurings across omnidepot's 15 reactor modules.

### Key Refactoring Capabilities

1. **AST-Aware Symbol Renaming (`rename_refactoring`)**:
   * Automatically updates class, method, and variable usages across all 15 Maven reactor modules simultaneously, avoiding broken cross-module imports.
2. **Call Hierarchy Analysis (`analyze_calls` & `search_symbol`)**:
   * Inspects upstream callers and downstream implementation dependencies before modifying core interface signatures in `repo-core-api`.
3. **Real-time IDE Diagnostics (`get_file_problems` & `lint_files`)**:
   * Catches compilation errors, missing generic parameters, and `@NullMarked` annotation violations instantly without waiting for a full Maven reactor build.
4. **Automated Formatting (`reformat_file`)**:
   * Ensures modified Java and TypeScript files strictly comply with omnidepot code style guidelines before commit.

> [!NOTE]
> **Best Practice for AI Agents:** When planning multi-file refactorings or domain model updates, use the `/plan` or `/goal` slash commands alongside the `intellij` MCP server tools to safely inspect symbol call graphs before performing modifications.

---

## Required Tokens & Credentials

### 1. GitHub MCP Server (`github`)

The GitHub MCP server provides AI agents with access to repository issues, pull request creation, code reviews, and GitHub Actions workflow monitoring.

#### Required Environment Variable
```bash
export GITHUB_TOKEN="ghp_yourPersonalAccessTokenHere..."
```

#### Creating a GitHub Personal Access Token (PAT)
1. Log in to GitHub and navigate to **Settings** $\rightarrow$ **Developer Settings** $\rightarrow$ **Personal Access Tokens**.
2. Select **Tokens (classic)** or **Fine-grained tokens**.
3. Grant the following mandatory scopes:
   - `repo` (Full control of private repositories, commit status, and code access)
   - `workflow` (Access to GitHub Actions workflows and job logs)
   - `read:org` (Read organization and team membership)

---

## Leveraging `.env` Files (Automated Credential Management)

Instead of manually executing `export GITHUB_TOKEN="..."` every time you open a terminal, omnidepot supports automatic environment variable loading via a local **`.env`** file.

### 1. Quick Setup: Create your `.env` file

Copy the provided [.env.example](file:///home/developer/projects/omnidepot/.env.example) template to `.env`:

```bash
cp .env.example .env
```

Edit `.env` and fill in your actual credentials:

```ini
# .env (git-ignored secret file)
GITHUB_TOKEN=ghp_1234567890abcdefghijklmnopqrstuvwxyz
INTELLIJ_PORT=64343
QUARKUS_PROFILE=dev
REPO_AUTH_MODE=disabled
```

> [!WARNING]
> **Security Warning:** `.env` is automatically ignored in [.gitignore](file:///home/developer/projects/omnidepot/.gitignore). Never commit `.env` files containing actual tokens to Git.

---

### 2. Auto-Loading `.env` in Terminal Sessions

#### Option A: Native Bash / Zsh Sourcing
Add a quick `source` alias to your shell config (`~/.bashrc` or `~/.zshrc`), or run it directly in your workspace:

```bash
# Export all variables defined in .env into the current shell session
export $(grep -v '^#' .env | xargs)
```

#### Option B: Using `direnv` (Recommended for Seamless Shells)
`direnv` automatically loads `.env` variables when you `cd` into the project directory:

1. Install `direnv` (`sudo apt install direnv` or `brew install direnv`).
2. Add `eval "$(direnv hook bash)"` to your `~/.bashrc`.
3. Enable `.env` loading in the project root:
   ```bash
   echo "dotenv" > .envrc
   direnv allow
   ```

#### Option C: Using `dotenvx` / `dotenv-cli`
Run commands wrapped with `dotenvx`:
```bash
npx @dotenvx/dotenvx run -- ./mvnw test
```

---

### 3. Auto-Loading `.env` in IDEs & AI Agents

- **IntelliJ IDEA:** Install the **EnvFile** plugin (**Settings** $\rightarrow$ **Plugins** $\rightarrow$ **EnvFile**). Enable `.env` file loading in your Maven / Quarkus Run Configurations.
- **Antigravity CLI / AI Agents:** AI tools reading `.mcp.json` automatically resolve variables from your shell environment or workspace `.env` file.

---

## Verification & Troubleshooting

### Test MCP Server Connection
You can test and inspect active MCP servers using the official MCP Inspector tool:

```bash
# Load .env and run GitHub MCP Server test
export $(grep -v '^#' .env | xargs)
npx -y @modelcontextprotocol/inspector npx -y @modelcontextprotocol/server-github

# Test IntelliJ Direct SSE Connection
curl -i -N -H "IJ_MCP_SERVER_PROJECT_PATH: /home/developer/projects/omnidepot" http://127.0.0.1:64343/sse
```

### Common Issues

1. **GitHub MCP Returns 401 Unauthorized:**
   - Verify `GITHUB_TOKEN` is loaded (`echo $GITHUB_TOKEN`).
   - Confirm token has not expired and holds `repo` scope permissions.

2. **IntelliJ MCP Connection Refused (`port 64343`):**
   - Ensure IntelliJ IDEA is open with the omnidepot project loaded.
   - Verify built-in server port in IntelliJ settings (**Build, Execution, Deployment** $\rightarrow$ **Debugger** $\rightarrow$ **Built-in Server** = `64343`).
