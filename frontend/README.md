# FitAI frontend

React + TypeScript + Vite interface for the existing FitAI Spring Boot backend.

## Run locally

Use Node.js 22 or newer and pnpm 11 (or npm).

```bash
cd frontend
pnpm install
pnpm dev
```

With npm, use `npm install` and `npm run dev` instead.

Open http://127.0.0.1:5173. Run the Spring Boot backend at http://localhost:8080.
Vite proxies `/api` to the backend, so no CORS changes are required for local
use. Change the target in `vite.config.ts` if the backend moves.

## Available flows

- Start a named workout; browse and switch between saved sessions.
- Add exercises, log sets, and edit recorded sets during active workouts.
- Record weight, reps, optional RIR, optional set duration, and notes.
- Finish a workout after confirmation; completed sessions are read-only.
- Display backend-calculated totals, volume, highest weight, and average RIR.
- Generate structured Gemini summaries for completed sessions.

No sample workouts are injected and no workout data is stored in the browser.
Backend errors are shown with a retry action. Weight units remain unspecified.
The edit API treats omitted optional numeric values as unchanged; the form follows
that behavior. The current backend requires positive weight for edits, while new
sets can have zero weight.

## Production build

```bash
pnpm build
```

Output is in `dist/`. A production host must forward `/api` to the backend;
the Vite development proxy is not part of the static production build. Keep
Gemini credentials on the backend. Fonts use Google Fonts with local fallbacks.

## Verification

TypeScript strict checking and the Vite production build passed. Browser QA
verified loading saved workouts and metrics, completed-session restrictions,
the populated set-edit form, and session navigation against the local backend.
No existing sets were changed during these read-only checks.

## Screenshots

See the [project screenshot gallery](../README.md#screenshots) for the workout
workspace, session history, and new workout form.
