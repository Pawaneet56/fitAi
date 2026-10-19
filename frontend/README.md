# FitAI frontend

React + TypeScript + Vite interface for the existing FitAI Spring Boot backend.

## Run locally

Use Node.js 22 or newer, which includes npm.

```bash
cd frontend
npm install
npm run dev
```

Use npm consistently for this project; `package-lock.json` records the dependency versions.

Open http://127.0.0.1:5173. Run the Spring Boot backend at http://localhost:8080.
Vite proxies `/api` to the backend, so no CORS changes are required for local
use. Change the target in `vite.config.ts` if the backend moves.

## Available flows

- Start a named workout; browse and switch between saved sessions.
- Add exercises, log sets, and edit recorded sets during active workouts.
- Record weight, reps, optional RIR, optional set duration, and notes.
- Finish a workout after confirmation; completed sessions are read-only.
- Display backend-calculated totals, volume, highest weight, and average RIR.
- Track asynchronous preparation of completed workouts in Gemini File Search.
- Queue older sessions for indexing and retry failed preparation.
- Generate structured Gemini summaries using retrieved workout documents.

No sample workouts are injected and no workout data is stored in the browser.
Backend errors are shown with a retry action. Weight units remain unspecified.
The edit API treats omitted optional numeric values as unchanged; the form follows
that behavior. The current backend requires positive weight for edits, while new
sets can have zero weight.

## Production build

```bash
npm run build
```

Output is in `dist/`. A production host must forward `/api` to the backend;
the Vite development proxy is not part of the static production build. Keep
Gemini credentials on the backend. Fonts use Google Fonts with local fallbacks.

## Insight preparation

The app assumes the backend's temporary single user, `user1`. New completed
workouts are queued automatically. Older sessions show **Prepare workout insights**.
The app polls indexing status, shows **Preparing insights**, and enables summaries
when Google reports readiness. Failed jobs retry automatically and offer a manual
retry action. Deploy/restart the updated backend before using these controls.

## Verification

The original frontend passed TypeScript strict checking and the Vite production build. Browser QA
verified loading saved workouts and metrics, completed-session restrictions,
the populated set-edit form, and session navigation against the local backend.
No existing sets were changed during these read-only checks. Live checks also covered creating, editing, and completing a synthetic workout,
updating metrics, and polling indexing status. The final summary success after
the latest source-verification change is awaiting a live recheck because Google's
daily request quota was exhausted. Quota errors are shown as readable messages.

## Screenshots

See the [project screenshot gallery](../README.md#screenshots) for the workout
workspace, session history, and new workout form.
