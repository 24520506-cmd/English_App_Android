# Star Journey — UI/UX refinement import

This folder is a Figma Desktop development plugin. `manifest.json` points to the actual importer (`code.js`) and UI (`ui.html`); `design.json` is the **plugin's own** `vocab-figma-import-v1` data format. It is not a Figma native import schema.

## Import

1. Open the target Figma design file in Figma Desktop.
2. Choose **Plugins → Development → Import plugin from manifest** and select this folder's `manifest.json`.
3. Run **STAR JOURNEY — UI/UX REFINEMENT IMPORT**.
4. Select this folder's `design.json` in the plugin window, then click **TẠO 113 MÀN HÌNH + FLOW**.
5. Inspect the newly created page `STAR JOURNEY · UI/UX REFINEMENT · 113 SCREENS` and the plugin's reported link errors, if any.

The plugin creates a **new page**. It does not delete or overwrite existing pages. Running it again creates another new page.

## What the package contains

- All 113 screen IDs from the connected Figma page, including learner, admin, loading, error, empty, success, and confirmation screens. The screen ID set was checked against the live file's audit baseline.
- A darker, quieter color palette; clearer Home learning action; more prominent listening audio region; larger writing entry areas; and preserved text/controls.
- A working week/month/all-time ranking selector in the static prototype, a route to report an inappropriate name, and an admin route to the existing revoke screen.
- Native editable Figma frames and text. Routes in `design.json` are translated into prototype navigation by `code.js`.

This is a **reconstruction from the existing local import package**, informed by the live file's screen and link inventory. The Figma MCP limit stopped a full layer-by-layer export, so this is not an exact duplicate of the live file. The 36 Figma variables and 8 text styles created earlier in the connected file are not automatically bound by this local plugin.

The plugin is a UI mockup. Form validation, authentication, audio playback, role checks, backend-driven states, and scoring require implementation in the app. Response-state screens remain available as references; they are not linked artificially from normal screens.

## Local checks

```bash
python3 -m unittest discover -s tests -v
node --check star_journey_unified_auth_full_import/STAR_JOURNEY_UNIFIED_AUTH/code.js
```

Run these from the project root. A full visual and prototype QA pass in Figma Desktop is still required after import.
