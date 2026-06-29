import { useEffect, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const TYPES = ['CURS', 'SEMINAR', 'LABORATOR'];
const PARITIES = ['saptamanal', 'par', 'impar'];
const DAY_LABELS = { 1: 'Luni', 2: 'Marți', 3: 'Miercuri', 4: 'Joi', 5: 'Vineri' };

const BLANK_REQ = {
  course_id: '', activity_type: 'CURS', group_name: '', sessions_per_week: 1,
  duration_hours: 2, week_parity: 'saptamanal', student_count: '', professor_id: '',
};
const hhmm = (t) => (t ? t.slice(0, 5) : '');

export default function OrarGenerator() {
  const [courses, setCourses] = useState([]);
  const [requirements, setRequirements] = useState([]);
  const [drafts, setDrafts] = useState([]);
  const [reqForm, setReqForm] = useState(BLANK_REQ);
  const [draftCount, setDraftCount] = useState(3);
  const [generating, setGenerating] = useState(false);
  const [previewId, setPreviewId] = useState(null);
  const [previewLessons, setPreviewLessons] = useState([]);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3500);
  };

  const loadAll = async () => {
    try {
      const [c, r, d] = await Promise.all([
        api.get('/api/courses'),
        api.get('/api/scheduling-requirements'),
        api.get('/api/orar/drafts'),
      ]);
      setCourses(c || []);
      setRequirements(r || []);
      setDrafts(d || []);
    } catch (err) {
      console.error('Load generator data failed:', err);
      flash('error', 'Eroare la încărcare.');
    }
  };

  useEffect(() => { loadAll(); }, []);

  // ----- requirements -----
  const addRequirement = async (e) => {
    e.preventDefault();
    if (!reqForm.course_id || !reqForm.group_name.trim()) {
      flash('error', 'Disciplina și grupa sunt obligatorii.');
      return;
    }
    const payload = {
      course_id: reqForm.course_id,
      activity_type: reqForm.activity_type,
      group_name: reqForm.group_name.trim(),
      sessions_per_week: Number(reqForm.sessions_per_week) || 1,
      duration_hours: Number(reqForm.duration_hours) || 2,
      week_parity: reqForm.week_parity,
      student_count: reqForm.student_count === '' ? null : Number(reqForm.student_count),
      professor_id: reqForm.professor_id || null,
    };
    try {
      await api.post('/api/scheduling-requirements', payload);
      setReqForm({ ...BLANK_REQ });
      flash('success', 'Cerință adăugată.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la adăugare.');
    }
  };

  const deleteRequirement = async (id) => {
    try {
      await api.del(`/api/scheduling-requirements/${id}`);
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la ștergere.');
    }
  };

  // ----- generation -----
  const generate = async () => {
    setGenerating(true);
    try {
      await api.post(`/api/orar/generate?drafts=${Number(draftCount) || 1}`);
      flash('success', 'Drafturi generate.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', err.message || 'Eroare la generare.');
    } finally {
      setGenerating(false);
    }
  };

  const preview = async (id) => {
    if (previewId === id) { setPreviewId(null); setPreviewLessons([]); return; }
    try {
      const lessons = await api.get(`/api/orar/drafts/${id}/lessons`);
      setPreviewId(id);
      setPreviewLessons(lessons || []);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la previzualizare.');
    }
  };

  const publish = async (id) => {
    if (!window.confirm('Publici acest draft? Va ÎNLOCUI întreg orarul actual.')) return;
    try {
      await api.post(`/api/orar/drafts/${id}/publish`);
      flash('success', 'Draft publicat în orar.');
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la publicare.');
    }
  };

  const deleteDraft = async (id) => {
    try {
      await api.del(`/api/orar/drafts/${id}`);
      if (previewId === id) { setPreviewId(null); setPreviewLessons([]); }
      loadAll();
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la ștergere.');
    }
  };

  const courseName = (id) => courses.find((c) => c.id === id)?.name || '—';

  return (
    <div style={{ display: 'grid', gap: 24 }}>
      {/* ---- Generation ---- */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title"><Icon name="auto_awesome" /> Generare orar</h2>
        </div>
        <div className="card-body">
          <div className="form-grid-2" style={{ alignItems: 'end', maxWidth: 480 }}>
            <label className="field">
              <span className="field-label">Număr de drafturi</span>
              <div className="input-wrap">
                <input type="number" min="1" max="5" value={draftCount}
                  onChange={(e) => setDraftCount(e.target.value)} />
              </div>
            </label>
            <button type="button" className="btn btn-primary" onClick={generate} disabled={generating}>
              {generating ? <span className="spinner" /> : <><Icon name="auto_awesome" /> Generează</>}
            </button>
          </div>
          <p className="muted" style={{ fontSize: 13, marginTop: 8 }}>
            Solver-ul ține cont de: conflicte profesor/sală/grupă, tip & capacitate sală,
            eligibilitate & disponibilitate profesor, durată & paritate, <strong>timp de
            deplasare între clădiri</strong> (min 2h dacă nu sunt în aceeași zonă) și
            <strong> compactarea orarului</strong> (fără ferestre mari). Câteva secunde per draft.
          </p>

          {drafts.length > 0 && (
            <div className="table-wrap" style={{ marginTop: 12 }}>
              <table className="data-table">
                <thead>
                  <tr><th>Draft</th><th>Scor (dur/soft)</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {drafts.map((d) => (
                    <tr key={d.id}>
                      <td>{d.name}</td>
                      <td className="mono">
                        <span className={d.hard_score === 0 ? 'badge status-pass' : 'badge status-fail'}>
                          {d.hard_score ?? '?'} dure
                        </span>{' '}
                        {d.soft_score ?? 0} soft
                      </td>
                      <td>{d.status}</td>
                      <td>
                        <div className="row-actions">
                          <button className="btn btn-outline btn-sm" onClick={() => preview(d.id)}>
                            {previewId === d.id ? 'Ascunde' : 'Vezi'}
                          </button>
                          <button className="btn btn-primary btn-sm" onClick={() => publish(d.id)}>Publică</button>
                          <button className="icon-btn" onClick={() => deleteDraft(d.id)} aria-label="Șterge">
                            <Icon name="delete" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {previewId && (
            <div className="table-wrap" style={{ marginTop: 12 }}>
              <table className="data-table">
                <thead>
                  <tr><th>Zi</th><th>Interval</th><th>Grupă</th><th>Disciplină</th><th>Tip</th><th>Profesor</th><th>Sală</th></tr>
                </thead>
                <tbody>
                  {previewLessons.map((l) => (
                    <tr key={l.id}>
                      <td>{DAY_LABELS[l.day_of_week] || l.day_of_week}</td>
                      <td className="mono">{hhmm(l.start_time)}–{hhmm(l.end_time)}</td>
                      <td>{l.group_name}</td>
                      <td>{l.course_name}</td>
                      <td>{l.activity_type}</td>
                      <td>{l.professor_name || '—'}</td>
                      <td>{l.room_code || '—'}</td>
                    </tr>
                  ))}
                  {previewLessons.length === 0 && (
                    <tr><td colSpan={7} className="muted center">Draft gol.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      {/* ---- Requirements ---- */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title"><Icon name="checklist" /> Cerințe orar ({requirements.length})</h2>
        </div>
        <form className="card-body" onSubmit={addRequirement}>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Disciplina</span>
              <div className="input-wrap">
                <select className="select-bare" value={reqForm.course_id}
                  onChange={(e) => setReqForm({ ...reqForm, course_id: e.target.value })}>
                  <option value="">Alege…</option>
                  {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
            </label>
            <label className="field">
              <span className="field-label">Tip</span>
              <div className="input-wrap">
                <select className="select-bare" value={reqForm.activity_type}
                  onChange={(e) => setReqForm({ ...reqForm, activity_type: e.target.value })}>
                  {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </label>
          </div>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Grupă / semigrupă</span>
              <div className="input-wrap">
                <input value={reqForm.group_name} placeholder="ex. 1321/1"
                  onChange={(e) => setReqForm({ ...reqForm, group_name: e.target.value })} />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Nr. studenți</span>
              <div className="input-wrap">
                <input type="number" min="0" value={reqForm.student_count}
                  onChange={(e) => setReqForm({ ...reqForm, student_count: e.target.value })} />
              </div>
            </label>
          </div>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Sesiuni / săptămână</span>
              <div className="input-wrap">
                <input type="number" min="1" max="5" value={reqForm.sessions_per_week}
                  onChange={(e) => setReqForm({ ...reqForm, sessions_per_week: e.target.value })} />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Durată (ore)</span>
              <div className="input-wrap">
                <select className="select-bare" value={reqForm.duration_hours}
                  onChange={(e) => setReqForm({ ...reqForm, duration_hours: e.target.value })}>
                  <option value={2}>2 ore</option>
                  <option value={3}>3 ore</option>
                </select>
              </div>
            </label>
          </div>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Paritate</span>
              <div className="input-wrap">
                <select className="select-bare" value={reqForm.week_parity}
                  onChange={(e) => setReqForm({ ...reqForm, week_parity: e.target.value })}>
                  {PARITIES.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
            </label>
            <div className="field" style={{ justifyContent: 'end' }}>
              <button type="submit" className="btn btn-primary"><Icon name="add" /> Adaugă cerință</button>
            </div>
          </div>
        </form>

        {requirements.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr><th>Disciplină</th><th>Tip</th><th>Grupă</th><th>Sesiuni</th><th>Durată</th><th>Paritate</th><th></th></tr>
              </thead>
              <tbody>
                {requirements.map((r) => (
                  <tr key={r.id}>
                    <td>{r.course_name || courseName(r.course_id)}</td>
                    <td>{r.activity_type}</td>
                    <td>{r.group_name}</td>
                    <td>{r.sessions_per_week}</td>
                    <td>{r.duration_hours}h</td>
                    <td>{r.week_parity}</td>
                    <td>
                      <button className="icon-btn" onClick={() => deleteRequirement(r.id)} aria-label="Șterge">
                        <Icon name="delete" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
