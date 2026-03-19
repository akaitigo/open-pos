# README Demo Assets

This guide explains how to refresh the screenshots and GIF used in the top-level [README](../../README.md).

## Prerequisites

- `pnpm e2e:install` has been run at least once on the machine.
- `ffmpeg` and `gifski` are installed and on `PATH`.
- The supported local demo stack is running:

```bash
make local-demo
pnpm dev:admin
pnpm dev:pos
```

If you are reusing a long-lived local PostgreSQL volume from older branches, run `make reset` once before capturing assets so the demo data is rebuilt on a fresh local schema.

## One Command Refresh

```bash
pnpm demo:assets
```

This runs two steps:

1. `pnpm demo:capture`
   Captures fresh README screenshots and a raw checkout video with Playwright.
2. `pnpm demo:gif`
   Converts the raw checkout video into `docs/assets/demo/pos-checkout.gif`.

## Outputs

- Final README assets:
  - `docs/assets/demo/admin-dashboard.png`
  - `docs/assets/demo/admin-inventory.png`
  - `docs/assets/demo/pos-products.png`
  - `docs/assets/demo/pos-checkout.gif`
- Raw capture used for GIF generation:
  - `.local/demo-assets/pos-checkout.webm`

## Notes

- The capture script assumes the seeded local demo data from `make local-demo`.
- If the GIF feels too heavy, rerun with a smaller width:

```bash
WIDTH=960 pnpm demo:gif
```

- If you want a smoother GIF, raise the frame rate carefully:

```bash
FPS=14 pnpm demo:gif
```
