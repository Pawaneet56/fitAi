import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Activity, ArrowDownLeft, ArrowRight, Check, ChevronRight, Clock3, Dumbbell, Flame, History, LayoutDashboard, LoaderCircle, Plus, RefreshCw, Sparkles, TrendingUp, X, Pencil, CheckCircle2 } from 'lucide-react';
import { api, type Exercise, type Metrics, type Summary, type Workout, type WorkoutSet, type IndexStatus } from './api';

const number = (value: number | null | undefined) => value == null ? '—' : new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value);
const date = (value: string) => new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
const duration = (seconds: number | null) => seconds == null ? 'In progress' : seconds < 60 ? `${seconds}s` : seconds < 3600 ? `${Math.floor(seconds / 60)}m` : `${Math.floor(seconds / 3600)}h ${Math.floor(seconds % 3600 / 60)}m`;
type Dialog = { kind: 'workout' } | { kind: 'exercise' } | { kind: 'set'; exercise: Exercise; set?: WorkoutSet } | { kind: 'finish' };

export default function App() {
  const [workouts, setWorkouts] = useState<Workout[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [workout, setWorkout] = useState<Workout | null>(null);
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [indexStatus, setIndexStatus] = useState<IndexStatus | null>(null);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [view, setView] = useState<'workspace' | 'history'>('workspace');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [dialog, setDialog] = useState<Dialog | null>(null);
  const [busy, setBusy] = useState(false);
  const [summaryBusy, setSummaryBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  const requestId = useRef(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    api<Workout[]>('', 'GET', undefined, controller.signal).then(data => {
      setWorkouts(data);
      setSelected(current => current && data.some(w => w.id === current) ? current : data.find(w => w.status === 'IN_PROGRESS')?.id ?? data[0]?.id ?? null);
    }).catch(e => { if (e.name !== 'AbortError') setError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);

  useEffect(() => {
    const controller = new AbortController();
    requestId.current += 1; setSummaryBusy(false); setSummary(null); setWorkout(null); setMetrics(null); setIndexStatus(null);
    if (!selected) return;
    setDetailLoading(true);
    Promise.all([api<Workout>(`/${selected}`, 'GET', undefined, controller.signal), api<Metrics>(`/${selected}/metrics`, 'GET', undefined, controller.signal)])
      .then(([w, m]) => { setWorkout(w); setMetrics(m); })
      .catch(e => { if (e.name !== 'AbortError') setError(e.message); })
      .finally(() => { if (!controller.signal.aborted) setDetailLoading(false); });
    return () => controller.abort();
  }, [selected, revision]);

  useEffect(() => {
    if (!workout || workout.status !== 'COMPLETED') return;
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    const poll = async () => {
      try {
        const status = await api<IndexStatus>(`/${workout.id}/index`, 'GET', undefined, controller.signal);
        if (controller.signal.aborted) return;
        setIndexStatus(status);
        if (status.status !== 'READY') timer = setTimeout(poll, 5000);
      } catch (e) {
        if (!controller.signal.aborted) {
          setError(e instanceof Error ? e.message : 'Unable to check insight preparation.');
          timer = setTimeout(poll, 15000);
        }
      }
    };
    void poll();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [workout]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!dialog) return;
    const form = new FormData(event.currentTarget); const value = (key: string) => String(form.get(key) ?? '').trim();
    setBusy(true); setError('');
    try {
      if (dialog.kind === 'workout') {
        const created = await api<Workout>('', 'POST', { notes: value('notes') || null });
        setSelected(created.id); setView('workspace');
      } else if (workout) {
        if (dialog.kind === 'exercise') await api(`/${workout.id}/exercises`, 'POST', { exerciseName: value('exerciseName') });
        if (dialog.kind === 'finish') await api(`/${workout.id}/end`, 'PATCH');
        if (dialog.kind === 'set') {
          const path = `/${workout.id}/exercises/${dialog.exercise.id}/sets`;
          await api(dialog.set ? `${path}/${dialog.set.id}` : path, dialog.set ? 'PATCH' : 'POST', {
            weight: Number(value('weight')), reps: Number(value('reps')),
            ...(value('rir') !== '' ? { rir: Number(value('rir')) } : {}),
            ...(value('durationSeconds') !== '' ? { durationSeconds: Number(value('durationSeconds')) } : {}),
            notes: dialog.set ? value('notes') : value('notes') || null,
          });
        }
      }
      setDialog(null); setRevision(r => r + 1);
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to save your changes.'); }
    finally { setBusy(false); }
  }

  async function generateSummary() {
    if (!workout) return;
    const id = ++requestId.current; setSummaryBusy(true); setError('');
    try {
      if (indexStatus?.status !== 'READY') {
        const status = await api<IndexStatus>(`/${workout.id}/index`, 'POST');
        if (id === requestId.current) setIndexStatus(status);
        return;
      }
      const data = await api<Summary>(`/${workout.id}/summary`);
      if (id === requestId.current) setSummary(data);
    }
    catch (e) { if (id === requestId.current) setError(e instanceof Error ? e.message : 'Unable to generate summary.'); }
    finally { if (id === requestId.current) setSummaryBusy(false); }
  }

  useEffect(() => {
    if (!dialog) return;
    const previous = document.activeElement as HTMLElement | null;
    const modal = document.querySelector<HTMLDialogElement>('dialog'); modal?.showModal();
    return () => { modal?.close(); previous?.focus(); };
  }, [dialog]);

  const openDialog = (next: Dialog) => { setError(''); setDialog(next); };
  const indexPending = !!indexStatus && ['PENDING', 'INDEXING'].includes(indexStatus.status);
  const active = workout?.status === 'IN_PROGRESS';
  const completed = workouts.filter(w => w.status === 'COMPLETED').length;
  const selectWorkout = (id: string) => { setError(''); setSelected(id); setView('workspace'); };
  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#" onClick={e => { e.preventDefault(); setView('workspace'); }}><span className="brand-icon"><Activity size={24}/></span>fitai<span className="brand-dot">.</span></a>
      <div className="workspace-label">YOUR WORKSPACE</div>
      <nav aria-label="Main navigation"><button className={view === 'workspace' ? 'nav-item selected' : 'nav-item'} onClick={() => setView('workspace')}><LayoutDashboard size={19}/> Workout workspace <ChevronRight size={15}/></button><button className={view === 'history' ? 'nav-item selected' : 'nav-item'} onClick={() => setView('history')}><History size={19}/> Session history <span className="nav-count">{workouts.length}</span></button></nav>
      <div className="sidebar-bottom"><div className="small-brand"><Dumbbell size={20}/> Built around your effort.</div><p>Log the work.<br/>Understand your training.</p><span className="version">FITAI / TRAINING JOURNAL</span></div>
    </aside>
    <div className="main-shell">
      <header className="topbar"><div><span className="breadcrumb">Workspace</span><ChevronRight size={13}/><span>{view === 'history' ? 'Session history' : 'Overview'}</span></div><span className="topbar-tag"><span/> YOUR TRAINING, IN FOCUS</span></header>
      <main>
        <div className="page-heading"><div><div className="eyebrow">A LITTLE STRONGER, EVERY SESSION</div><h1>{view === 'history' ? 'Your training journal' : 'Make every rep count.'}</h1><p>{view === 'history' ? 'All your sessions, ready to revisit.' : 'Your workouts, your numbers, a clearer picture.'}</p></div><button className="button primary" onClick={() => openDialog({kind:'workout'})}><Plus size={18}/> Start workout</button></div>
        {error && <div className="error" role="alert"><span>{error}</span><button className="icon-button" onClick={() => {setError(''); setRevision(r => r+1);}} aria-label="Retry loading"><RefreshCw size={17}/></button><button className="icon-button" onClick={() => setError('')} aria-label="Dismiss error"><X size={17}/></button></div>}
        <section className="intro-band"><div className="intro-copy"><span className="pill"><span/> THE WORK ADDS UP</span><h2>Show up. Put in the work.<br/><em>See what it means.</em></h2><p>One place to log your sessions and turn your<br className="desktop-break"/> training into insight.</p></div><div className="bar-art" aria-hidden="true"><div className="art-grid"/>{[28,43,36,60,52,76,69,92].map((h,i)=><div key={i} className="art-bar" style={{height:`${h}%`}}/>)}<span className="art-label"><TrendingUp size={16}/> EVERY SESSION IS A STEP</span></div><div className="band-stat"><span>SESSIONS COMPLETED</span><strong>{String(completed).padStart(2,'0')}</strong><small>Your effort, recorded.</small></div></section>
        {view === 'history' ? <section className="panel history-panel"><div className="section-heading"><div><h2>All sessions <span className="count">{workouts.length}</span></h2><p>A record of the work you’ve put in.</p></div></div>{loading ? <Loading/> : workouts.length === 0 ? <Empty onStart={() => openDialog({kind:'workout'})}/> : <div className="history-list">{workouts.map(w => <button key={w.id} onClick={()=>selectWorkout(w.id)}><span className="session-icon"><Dumbbell size={21}/></span><span><strong>{w.notes || 'Untitled workout'}</strong><small>{date(w.startedAt)} · {duration(w.durationSeconds)}</small></span><Status active={w.status === 'IN_PROGRESS'}/><ArrowRight size={18}/></button>)}</div>}</section> : <>
        <div className="workspace-grid"><section className="session-column">
          <div className="section-heading"><div><h2>Your session <span className="live-dot"/></h2><p>{workout ? 'A closer look at the work.' : 'Ready when you are.'}</p></div><button className="text-button" onClick={()=>setView('history')}>View history <ArrowRight size={15}/></button></div>
          {loading ? <div className="panel"><Loading/></div> : workouts.length === 0 ? <div className="panel"><Empty onStart={()=>openDialog({kind:'workout'})}/></div> : <>
            <label className="session-select-label" htmlFor="session">SELECT SESSION</label><select id="session" className="session-select" value={selected ?? ''} onChange={e=>selectWorkout(e.target.value)}>{workouts.map(w=><option key={w.id} value={w.id}>{w.notes || 'Untitled workout'} · {date(w.startedAt)}{w.status === 'IN_PROGRESS' ? ' · In progress' : ''}</option>)}</select>
            {detailLoading ? <div className="panel"><Loading/></div> : workout && <section className="panel session-panel"><div className="session-header"><div className="session-title"><div className="session-icon"><Dumbbell size={23}/></div><div><h3>{workout.notes || 'Untitled workout'}</h3><span className="muted session-date">{date(workout.startedAt)}<span>·</span><Clock3 size={13}/>{duration(workout.durationSeconds)}</span></div></div><Status active={!!active}/></div>
              <div className="exercise-list">{(workout.exercises ?? []).length === 0 ? <div className="exercise-empty"><Dumbbell size={27}/><h3>A fresh start.</h3><p>{active ? 'Add your first exercise to begin logging sets.' : 'No exercises were recorded in this session.'}</p></div> : (workout.exercises ?? []).map((exercise,index)=><div className="exercise" key={exercise.id}><div className="exercise-heading"><span className="exercise-number">{String(index+1).padStart(2,'0')}</span><h4>{exercise.exerciseName}</h4><span>{exercise.sets?.length || 0} sets</span></div><div className="table-scroll"><table><thead><tr><th>SET</th><th>WEIGHT</th><th>REPS</th><th>RIR</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{(exercise.sets ?? []).map(set=><tr key={set.id}><td><span className="set-number">{set.setNumber}</span></td><td>{number(set.weight)}</td><td>{set.reps}</td><td>{number(set.rir)}</td><td>{active ? <button className="icon-button" aria-label={`Edit ${exercise.exerciseName} set ${set.setNumber}`} onClick={()=>openDialog({kind:'set',exercise,set})}><Pencil size={14}/></button> : <Check size={15} className="check"/>}</td></tr>)}</tbody></table></div>{!exercise.sets?.length && <p className="no-sets">No sets logged yet.</p>}{active && <button className="add-set" onClick={()=>openDialog({kind:'set',exercise})}><Plus size={15}/> Add set</button>}</div>)}</div>
              {active && <div className="session-actions"><button className="button secondary" onClick={()=>openDialog({kind:'exercise'})}><Plus size={16}/> Add exercise</button><button className="text-button" onClick={()=>openDialog({kind:'finish'})}><CheckCircle2 size={16}/> Finish workout</button></div>}
            </section>}
          </>}
        </section><aside className="insights-column"><div className="section-heading"><div><h2>The numbers</h2><p>Calculated from your logged sets.</p></div><Activity size={19} className="muted"/></div><div className="metrics-grid"><Metric icon={<Dumbbell size={17}/>} label="TOTAL VOLUME" value={number(metrics?.totalVolume)} note="Logged weight × reps"/><Metric icon={<Flame size={17}/>} label="TOTAL SETS" value={number(metrics?.totalSets)} note={`${number(metrics?.totalExercises)} exercises`}/><Metric icon={<TrendingUp size={17}/>} label="HIGHEST WEIGHT" value={number(metrics?.highestWeight)} note="Your logged weight unit"/><Metric icon={<ArrowDownLeft size={17}/>} label="AVERAGE RIR" value={number(metrics?.averageRir)} note="Recorded sets only"/></div><div className="reps-strip"><span>Total reps</span><strong>{number(metrics?.totalReps)}</strong></div>
          <section className="ai-panel"><div className="ai-heading"><span className="spark-icon"><Sparkles size={19}/></span><h3>A little perspective.</h3><span className="ai-badge">AI</span></div>{summary ? <div className="summary"><p>{summary.summary}</p><h4>WHAT STANDS OUT</h4><ul>{summary.observations.map((s,i)=><li key={i}>{s}</li>)}</ul><h4>FOR YOUR NEXT SESSION</h4><ul>{summary.suggestions.map((s,i)=><li key={i}>{s}</li>)}</ul></div> : <><p>Your effort tells a story. Get a thoughtful summary grounded in your session’s numbers.</p><div className="ai-note"><span>✧</span><span>{active ? 'Finish your workout to unlock your summary.' : workout ? indexStatus?.status === 'FAILED' ? 'Insight preparation failed and will retry automatically. You can also retry now.' : indexPending ? 'Preparing insights from your saved workout…' : indexStatus?.status === 'NOT_QUEUED' ? 'Prepare this saved session to unlock your insights.' : 'Your session is ready for a closer look.' : 'Log a workout to discover your insights.'}</span></div></>}<button className="button ai-button" disabled={!workout || active || summaryBusy || detailLoading || indexPending || !indexStatus} onClick={generateSummary}>{summaryBusy ? <LoaderCircle size={16} className="spin"/> : <Sparkles size={16}/>} {indexPending ? 'Preparing insights…' : summaryBusy ? 'Working on your insights…' : indexStatus?.status === 'FAILED' ? 'Retry insight preparation' : indexStatus?.status === 'NOT_QUEUED' ? 'Prepare workout insights' : summary ? 'Regenerate summary' : 'Generate workout summary'}{!summaryBusy && <ArrowRight size={16}/>}</button><small>Powered by Gemini · Based on your logged data</small></section>
        </aside></div></>}
        <footer><span className="footer-brand"><Activity size={16}/> fitai</span><span>Small steps. Honest numbers. Stronger sessions.</span><span>MADE FOR THE WORK</span></footer>
      </main>
    </div>
    {dialog && <dialog onCancel={e=>{e.preventDefault(); if (!busy) setDialog(null);}} onClick={e=>{if(e.target===e.currentTarget&&!busy)setDialog(null);}}><form onSubmit={save}><div className="dialog-heading"><span className="session-icon"><Dumbbell size={21}/></span><button type="button" className="icon-button" disabled={busy} onClick={()=>setDialog(null)} aria-label="Close dialog"><X size={20}/></button></div><h2>{dialog.kind==='workout'?'A new session.':dialog.kind==='exercise'?'Add an exercise':dialog.kind==='finish'?'Finish this workout?':dialog.set?'Edit your set':'Log your next set'}</h2><p className="dialog-description">{dialog.kind==='workout'?'Give your workout a name. Then make it count.':dialog.kind==='exercise'?'What are you working on next?':dialog.kind==='finish'?'Your session will be saved as completed. Exercises and sets can no longer be changed.':dialog.exercise.exerciseName}</p>
      {error && <p className="dialog-error" role="alert">{error}</p>}
      {dialog.kind==='workout' && <label>Workout name<input autoFocus name="notes" placeholder="e.g. Monday · Push day" maxLength={1000}/></label>}
      {dialog.kind==='exercise' && <label>Exercise name<input autoFocus name="exerciseName" placeholder="e.g. Bench Press" required maxLength={255}/></label>}
      {dialog.kind==='set' && <><div className="form-row"><label>Weight<input autoFocus name="weight" type="number" min={dialog.set?0.000001:0} step="any" required defaultValue={dialog.set?.weight} placeholder="80"/></label><label>Reps<input name="reps" type="number" min="1" step="1" required defaultValue={dialog.set?.reps} placeholder="8"/></label></div><div className="form-row"><label>RIR <span>(optional)</span><input name="rir" type="number" min="0" step="1" defaultValue={dialog.set?.rir??''} placeholder="2"/></label><label>Duration, seconds <span>(optional)</span><input name="durationSeconds" type="number" min="0" step="1" defaultValue={dialog.set?.durationSeconds??''} placeholder="30"/></label></div><label>Notes <span>(optional)</span><textarea name="notes" maxLength={1000} defaultValue={dialog.set?.notes??''} placeholder="How did this set feel?"/></label><p className="form-hint">RIR = reps in reserve. Keep weight units consistent across sets.{dialog.set && ' Leave optional numbers blank to keep their recorded values.'}</p></>}
      <div className="dialog-actions"><button className="button secondary" type="button" disabled={busy} onClick={()=>setDialog(null)}>Cancel</button><button className="button primary" disabled={busy}>{busy?<LoaderCircle size={16} className="spin"/>:<Check size={16}/>} {busy?'Saving…':dialog.kind==='finish'?'Finish workout':dialog.kind==='workout'?'Start workout':dialog.kind==='exercise'?'Add exercise':'Save set'}</button></div></form></dialog>}
  </div>;
}
function Status({active}:{active:boolean}) {return <span className={`status ${active?'active':''}`}><span/>{active?'In progress':'Completed'}</span>;}
function Metric({icon,label,value,note}:{icon:React.ReactNode;label:string;value:string;note:string}) {return <div className="metric"><div>{label}{icon}</div><strong>{value}</strong><small>{note}</small></div>;}
function Loading(){return <div className="loading" role="status"><LoaderCircle size={23} className="spin"/><span>Loading your training…</span></div>;}
function Empty({onStart}:{onStart:()=>void}){return <div className="empty"><span className="empty-icon"><Dumbbell size={33}/></span><h3>Your next chapter starts here.</h3><p>Start a workout, log your sets, and see<br/>your effort take shape.</p><button className="button primary" onClick={onStart}><Plus size={17}/> Start your first workout</button></div>;}
