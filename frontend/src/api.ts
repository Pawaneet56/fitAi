export type WorkoutSet = { id: string; setNumber: number; weight: number; reps: number; rir: number | null; durationSeconds: number | null; notes: string | null };
export type Exercise = { id: string; exerciseName: string; orderIndex: number; sets: WorkoutSet[] };
export type Workout = { id: string; startedAt: string; endedAt: string | null; durationSeconds: number | null; status: 'IN_PROGRESS' | 'COMPLETED'; notes: string | null; exercises: Exercise[] | null };
export type Metrics = { totalExercises: number; totalSets: number; totalReps: number; totalVolume: number; highestWeight: number | null; averageRir: number | null };
export type Summary = { summary: string; observations: string[]; suggestions: string[] };
export async function api<T>(path: string, method = 'GET', body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/workouts${path}`, { method, signal, headers: body === undefined ? undefined : { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new Error(data?.message || data?.detail || `Request failed (${response.status}). Check that the backend is running on port 8080.`);
  }
  return response.json();
}
