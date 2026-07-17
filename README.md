# ScanPro

A free, cross-platform document scanning app (Adobe Scan alternative) with unlimited scanning, folder organization, OCR search, cloud-drive auto-sync, and translation. Android-first (v1).

## Project documents

| File | What it is |
|---|---|
| [PRD.md](PRD.md) | Product Requirements Document, incl. review & risk assessment (§11) |
| [ERD.md](ERD.md) | Entity-relationship data model (entities, attributes, relationships, enums, indexes) |
| [BUILD_SPEC.md](BUILD_SPEC.md) | Engineering build spec — stack, architecture, modules, integrations, CI/CD, milestones |
| [ENGINEERING_SPEC.md](ENGINEERING_SPEC.md) | Detailed technical design — interfaces, sequence flows, sync state machine, contracts, error taxonomy, testing, acceptance criteria |
| [PHASED_PLAN.md](PHASED_PLAN.md) | Phased development roadmap (P0–P6) + what's needed to sync the repo and build the APK |
| [wireframes.html](wireframes.html) | Low-fidelity wireframes — 10 core screens, annotated to the PRD |
| [scan-pro-figma-wireframes.html](scan-pro-figma-wireframes.html) | Low-fidelity wireframes as Figma-import-ready mobile artboards |
| [figma-design.html](figma-design.html) | High-fidelity UI (minimal & neutral), ready to import into Figma |

## Code

| Directory | What it is |
|---|---|
| [cloudflare/](cloudflare) | Deployable Cloudflare Workers + D1 API — account/document metadata and cross-device sync. See [cloudflare/README.md](cloudflare/README.md). |
| [android/](android) | Android app scaffold (Kotlin, Compose, Hilt, Retrofit) wired to the API; one screen live (Home). See [android/README.md](android/README.md). |

**Architecture:** on-device scan/OCR/translation + files in the user's own Google Drive, with a thin **Cloudflare (Workers + D1)** backend for accounts, metadata, and cross-device sync. Auth via Firebase (the Worker validates the Firebase ID token). No document files touch our servers.

## Key product decisions

- **Platform:** Android only for v1 (iOS later).
- **Funding:** Free core + paid power features. Scanning, folders, editing, PDF export, and drive sync are free forever; high-accuracy cloud translation is the paid tier.
- **Cloud drives:** Google Drive at launch; Dropbox + OneDrive in v1.1.
- **Storage:** Documents live in the user's own drive, not our servers.

---

## Figma MCP (Framelink)

This project is configured to connect to Figma (read/inspect) via the [Framelink Figma MCP](https://github.com/GLips/Figma-Context-MCP) server, defined in [`.mcp.json`](.mcp.json). It lets Claude Code read Figma files — layers, colors, text, components — and generate or compare code against them. It is **read-only**; it cannot author designs in Figma.

### One-time setup

1. **Create a Figma token** — in Figma: avatar → **Settings → Security → Personal access tokens → Generate new token**. Scope: **File content: Read-only**. Copy it (shown once).

2. **Store the token as an environment variable** (the config reads `${FIGMA_API_KEY}`, so the token is never written into the repo). In a terminal:

   **Windows (PowerShell), persistent for your user:**
   ```powershell
   setx FIGMA_API_KEY "figd_your_token_here"
   ```
   Then **close and reopen** the terminal/app so the variable is inherited (`setx` only affects new sessions).

   **macOS/Linux (bash/zsh):** add to `~/.zshrc` or `~/.bashrc`:
   ```bash
   export FIGMA_API_KEY="figd_your_token_here"
   ```

3. **Restart Claude Code.** On restart it picks up the project-scoped `figma` server and shows a **trust prompt** — approve it. Confirm it's connected in the app's MCP/connectors view.

### Usage

Once connected, paste a Figma **file or frame URL** and ask Claude to read it (e.g. "read this frame and generate the CSS", or "compare this to figma-design.html"). The Framelink tools (e.g. `get_figma_data`) load on demand.

### Notes

- The server runs via `npx figma-developer-mcp` (fetched on first use — Node.js required).
- Getting designs *into* Figma (the reverse direction) uses the **html.to.design** Figma plugin with the published `figma-design.html` URL — see the top of that file for steps.
