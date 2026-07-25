# OmniDepot: MCP Server Setup & Token Guide

This guide details the setup, credentials, environment variables, and permission scopes required to run the **Model Context Protocol (MCP)** servers integrated into **OmniDepot**.

---

## 🔌 Integrated MCP Servers Overview

OmniDepot configures four specialized MCP servers defined in [.mcp.json](file:///home/developer/projects/OmniDepot/.mcp.json) and [.agents/config.json](file:///home/developer/projects/OmniDepot/.agents/config.json):

```text
                                OMNIDEPOT MCP ECOSYSTEM
┌───────────────────────┬───────────────────────────────┬────────────────────────────────┐
│ MCP Server            │ Utility / Purpose             │ Authentication / Credentials   │
├───────────────────────┼───────────────────────────────┼────────────────────────────────┤
│ 1. github             │ Repository issues, PRs, CI    │ GITHUB_TOKEN (PAT)             │
│ 2. intellij           │ Live IDE AST & symbol navigation│ Localhost Port 63342 (No Auth) │
│ 3. archunitChecker    │ Package boundary verification │ Local Maven Runner (Zero Auth) │
│ 4. liquibaseValidator │ DB changelog & rollback validation│ Local Maven Runner (Zero Auth) │
└───────────────────────┴───────────────────────────────┴────────────────────────────────┘
```

---

## 🔑 Required Tokens & Credentials

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

## ⚡ Leveraging `.env` Files (Automated Credential Management)

Instead of manually executing `export GITHUB_TOKEN="..."` every time you open a terminal, OmniDepot supports automatic environment variable loading via a local **`.env`** file.

### 1. Quick Setup: Create your `.env` file

Copy the provided [.env.example](file:///home/developer/projects/OmniDepot/.env.example) template to `.env`:

```bash
cp .env.example .env
```

Edit `.env` and fill in your actual credentials:

```ini
# .env (git-ignored secret file)
GITHUB_TOKEN=ghp_1234567890abcdefghijklmnopqrstuvwxyz
INTELLIJ_PORT=63342
QUARKUS_PROFILE=dev
REPO_AUTH_MODE=disabled
```

> ⚠️ **Security Warning:** `.env` is automatically ignored in [.gitignore](file:///home/developer/projects/OmniDepot/.gitignore). Never commit `.env` files containing actual tokens to Git.

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

## 🛠️ Verification & Troubleshooting

### Test MCP Server Connection
You can test and inspect active MCP servers using the official MCP Inspector tool:

```bash
# Load .env and run GitHub MCP Server test
export $(grep -v '^#' .env | xargs)
npx -y @modelcontextprotocol/inspector npx -y @modelcontextprotocol/server-github

# Test IntelliJ MCP Server
npx -y @modelcontextprotocol/inspector npx -y @jetbrains/mcp-proxy
```

### Common Issues

1. **GitHub MCP Returns 401 Unauthorized:**
   - Verify `GITHUB_TOKEN` is loaded (`echo $GITHUB_TOKEN`).
   - Confirm token has not expired and holds `repo` scope permissions.

2. **IntelliJ MCP Connection Refused (`port 63342`):**
   - Ensure IntelliJ IDEA is open with the OmniDepot project loaded.
   - Verify built-in server port in IntelliJ settings (**Build, Execution, Deployment** $\rightarrow$ **Debugger** $\rightarrow$ **Built-in Server** = `63342`).
