// Professor: define a course's grading scheme, pull values from a Google Sheet
// and/or manual entry, compute & save each student's final grade + breakdown.
import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const MATCH_FIELDS = [
  { v: 'student_id', label: 'Nr. matricol' },
  { v: 'email', label: 'Email instituțional' },
  { v: 'full_name', label: 'Nume complet' },
];
const BLANK_SCHEME = {
  pass_mode: 'overall', pass_threshold: 5, round_up: true,
  sheet_url: '', match_field: 'student_id', match_column: '', components: [],
};
const BLANK_COMP = { name: '', weight: 0, is_bonus: false, min_threshold: '', source: 'document', sheet_columns: [] };

export default function Grading() {
  const { user, currentRole } = useAuth();
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [scheme, setScheme] = useState(BLANK_SCHEME);
  const [headers, setHeaders] = useState([]);
  const [students, setStudents] = useState([]);
  const [manual, setManual] = useState({}); // `${componentIndex}:${studentId}` -> value (by name, since ids only after save)
  const [manualByComp, setManualByComp] = useState({}); // componentId|studentId -> value
  const [computeRows, setComputeRows] = useState(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState(null);

  const flash = (v, m) => { setToast({ variant: v, message: m }); setTimeout(() => setToast(null), 3500); };

  useEffect(() => {
    if (!user) return;
    api.get('/api/professor-courses/mine').then((rows) => {
      const seen = new Set();
      const list = [];
      for (const r of rows || []) {
        if (!seen.has(r.course_id)) { seen.add(r.course_id); list.push({ id: r.course_id, name: r.courses?.name || '—' }); }
      }
      setCourses(list);
      if (list.length && !courseId) setCourseId(list[0].id);
    }).catch((e) => console.error(e));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  useEffect(() => {
    if (!courseId) return;
    setComputeRows(null);
    setHeaders([]);
    (async () => {
      try {
        const [s, cat, mg] = await Promise.all([
          api.get(`/api/grading/${courseId}/scheme`),
          api.get(`/api/professor/catalog?courseId=${courseId}`),
          api.get(`/api/grading/${courseId}/manual`),
        ]);
        setScheme(s || { ...BLANK_SCHEME });
        // unique students from catalog
        const seen = new Set(); const studs = [];
        for (const row of cat || []) { if (!seen.has(row.student_id)) { seen.add(row.student_id); studs.push({ id: row.student_id, name: row.student_name }); } }
        setStudents(studs);
        const map = {};
        for (const m of mg || []) map[`${m.component_id}|${m.student_id}`] = m.value;
        setManualByComp(map);
      } catch (e) { console.error(e); flash('error', 'Eroare la încărcare.'); }
    })();
  }, [courseId]);

  // ----- scheme editing -----
  const setField = (k, v) => setScheme((s) => ({ ...s, [k]: v }));
  const setComp = (i, k, v) => setScheme((s) => ({ ...s, components: s.components.map((c, j) => j === i ? { ...c, [k]: v } : c) }));
  const addComp = () => setScheme((s) => ({ ...s, components: [...s.components, { ...BLANK_COMP }] }));
  const removeComp = (i) => setScheme((s) => ({ ...s, components: s.components.filter((_, j) => j !== i) }));
  const toggleCol = (i, col) => setScheme((s) => ({
    ...s,
    components: s.components.map((c, j) => j === i
      ? { ...c, sheet_columns: c.sheet_columns.includes(col) ? c.sheet_columns.filter((x) => x !== col) : [...c.sheet_columns, col] }
      : c),
  }));

  const saveScheme = async () => {
    const payload = {
      ...scheme,
      pass_threshold: Number(scheme.pass_threshold) || 5,
      components: scheme.components.map((c, i) => ({
        name: c.name, weight: Number(c.weight) || 0, is_bonus: !!c.is_bonus,
        min_threshold: c.min_threshold === '' || c.min_threshold == null ? null : Number(c.min_threshold),
        source: c.source, sheet_columns: c.source === 'document' ? c.sheet_columns : [], sort_order: i,
      })),
    };
    const saved = await api.put(`/api/grading/${courseId}/scheme`, payload);
    setScheme(saved);
    return saved;
  };

  const onSave = async () => {
    setBusy(true);
    try { await saveScheme(); flash('success', 'Schemă salvată.'); }
    catch (e) { console.error(e); flash('error', e.message || 'Eroare la salvare.'); }
    finally { setBusy(false); }
  };

  const readColumns = async () => {
    setBusy(true);
    try {
      await saveScheme();
      const data = await api.get(`/api/grading/${courseId}/sheet-columns`);
      setHeaders(data.headers || []);
      flash('success', `${(data.headers || []).length} coloane citite din sheet.`);
    } catch (e) { console.error(e); flash('error', e.message || 'Eroare la citirea sheet-ului.'); }
    finally { setBusy(false); }
  };

  const saveManual = async (componentId, studentId, value) => {
    try {
      await api.put(`/api/grading/${courseId}/manual`, { component_id: componentId, student_id: studentId, value: value === '' ? null : Number(value) });
      setManualByComp((m) => ({ ...m, [`${componentId}|${studentId}`]: value === '' ? null : Number(value) }));
    } catch (e) { console.error(e); flash('error', 'Eroare la nota manuală.'); }
  };

  const compute = async (save) => {
    setBusy(true);
    try {
      await saveScheme();
      const res = await api.post(`/api/grading/${courseId}/compute?save=${save}`);
      setComputeRows(res);
      flash('success', save ? 'Note calculate și salvate.' : `Calculat ${res.rows.length} studenți.`);
    } catch (e) { console.error(e); flash('error', e.message || 'Eroare la calcul.'); }
    finally { setBusy(false); }
  };

  if (currentRole && currentRole !== 'profesor') return <Navigate to="/" replace />;

  const manualComps = (scheme.components || []).filter((c) => c.source === 'manual' && c.id);

  return (
    <div className="page" style={{ display: 'grid', gap: 20 }}>
      <section className="header-card">
        <h1 className="page-title">Sistem de notare</h1>
        <p className="page-subtitle">
          Definește formula, leagă un Google Sheet (partajat „oricine cu link poate vedea"),
          completează notele manuale, apoi calculează nota finală a fiecărui student.
        </p>
      </section>

      <section className="card">
        <div className="card-body">
          <label className="field" style={{ maxWidth: 460 }}>
            <span className="field-label">Disciplina</span>
            <div className="input-wrap">
              <Icon name="menu_book" className="input-icon" />
              <select className="select-bare" value={courseId} onChange={(e) => setCourseId(e.target.value)}>
                {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </label>
        </div>
      </section>

      {/* Scheme settings */}
      <section className="card">
        <div className="card-header"><h2 className="card-title"><Icon name="tune" /> Setări</h2></div>
        <div className="card-body">
          <div className="form-grid-2">
            <label className="field"><span className="field-label">Promovare</span>
              <div className="input-wrap"><select className="select-bare" value={scheme.pass_mode}
                onChange={(e) => setField('pass_mode', e.target.value)}>
                <option value="overall">Doar nota finală ≥ prag</option>
                <option value="per_criterion">Prag pe fiecare criteriu</option>
              </select></div></label>
            <label className="field"><span className="field-label">Prag promovare</span>
              <div className="input-wrap"><input type="number" step="0.5" min="1" max="10"
                value={scheme.pass_threshold} onChange={(e) => setField('pass_threshold', e.target.value)} /></div></label>
          </div>
          <div className="form-grid-2">
            <label className="field"><span className="field-label">Identificare student după</span>
              <div className="input-wrap"><select className="select-bare" value={scheme.match_field}
                onChange={(e) => setField('match_field', e.target.value)}>
                {MATCH_FIELDS.map((f) => <option key={f.v} value={f.v}>{f.label}</option>)}
              </select></div></label>
            <label className="field" style={{ justifyContent: 'center' }}>
              <label className="course-check"><input type="checkbox" checked={!!scheme.round_up}
                onChange={(e) => setField('round_up', e.target.checked)} />
                <span>Rotunjește nota finală (≥ .50 în sus)</span></label>
            </label>
          </div>
          <label className="field"><span className="field-label">Link Google Sheet</span>
            <div className="input-wrap"><input value={scheme.sheet_url || ''} placeholder="https://docs.google.com/spreadsheets/d/.../edit?gid=0"
              onChange={(e) => setField('sheet_url', e.target.value)} /></div></label>
          <div className="form-actions" style={{ justifyContent: 'flex-start', gap: 10 }}>
            <button type="button" className="btn btn-outline" onClick={readColumns} disabled={busy}>
              <Icon name="sync" /> Citește coloanele din sheet
            </button>
            {headers.length > 0 && (
              <label className="field" style={{ margin: 0 }}><span className="field-label" style={{ fontSize: 12 }}>Coloana cu identificatorul</span>
                <div className="input-wrap"><select className="select-bare" value={scheme.match_column || ''}
                  onChange={(e) => setField('match_column', e.target.value)}>
                  <option value="">Alege…</option>
                  {headers.map((h) => <option key={h} value={h}>{h}</option>)}
                </select></div></label>
            )}
          </div>
        </div>
      </section>

      {/* Components */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title"><Icon name="functions" /> Componente</h2>
          <button type="button" className="btn btn-outline btn-sm" onClick={addComp}><Icon name="add" /> Adaugă</button>
        </div>
        <div className="card-body" style={{ gap: 12 }}>
          {scheme.components.length === 0 && <p className="muted">Nicio componentă. Adaugă criterii (laborator, seminar, examen…).</p>}
          {scheme.components.map((c, i) => (
            <div key={i} className="card" style={{ padding: 12 }}>
              <div className="form-grid-2">
                <label className="field"><span className="field-label">Nume</span>
                  <div className="input-wrap"><input value={c.name} placeholder="ex. Laborator"
                    onChange={(e) => setComp(i, 'name', e.target.value)} /></div></label>
                <label className="field"><span className="field-label">Pondere (%)</span>
                  <div className="input-wrap"><input type="number" min="0" max="100" value={c.weight}
                    onChange={(e) => setComp(i, 'weight', e.target.value)} /></div></label>
              </div>
              <div className="form-grid-2">
                <label className="field"><span className="field-label">Sursă</span>
                  <div className="input-wrap"><select className="select-bare" value={c.source}
                    onChange={(e) => setComp(i, 'source', e.target.value)}>
                    <option value="document">Din document (sheet)</option>
                    <option value="manual">Manual (în aplicație)</option>
                  </select></div></label>
                <label className="field"><span className="field-label">Prag minim (opțional)</span>
                  <div className="input-wrap"><input type="number" step="0.5" min="0" max="10" value={c.min_threshold ?? ''}
                    onChange={(e) => setComp(i, 'min_threshold', e.target.value)} /></div></label>
              </div>
              <label className="course-check" style={{ marginBottom: 8 }}>
                <input type="checkbox" checked={!!c.is_bonus} onChange={(e) => setComp(i, 'is_bonus', e.target.checked)} />
                <span>Bonus (se adaugă peste notă)</span>
              </label>
              {c.source === 'document' && headers.length > 0 && (
                <div className="field">
                  <span className="field-label">Coloane din sheet (se mediază)</span>
                  <div className="course-multiselect">
                    {headers.map((h) => (
                      <label key={h} className="course-check">
                        <input type="checkbox" checked={c.sheet_columns.includes(h)} onChange={() => toggleCol(i, h)} />
                        <span>{h}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
              {c.source === 'document' && headers.length === 0 && (
                <p className="muted" style={{ fontSize: 12 }}>Apasă „Citește coloanele" ca să poți selecta coloanele.</p>
              )}
              <div className="form-actions">
                <button type="button" className="icon-btn" onClick={() => removeComp(i)} aria-label="Șterge"><Icon name="delete" /></button>
              </div>
            </div>
          ))}
          <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
            <button type="button" className="btn btn-primary" onClick={onSave} disabled={busy}>
              {busy ? <span className="spinner" /> : <><Icon name="save" /> Salvează schema</>}
            </button>
          </div>
        </div>
      </section>

      {/* Manual grades grid */}
      {manualComps.length > 0 && (
        <section className="card">
          <div className="card-header"><h2 className="card-title"><Icon name="edit_note" /> Note manuale</h2></div>
          <div className="table-wrap">
            <table className="data-table">
              <thead><tr><th>Student</th>{manualComps.map((c) => <th key={c.id}>{c.name}</th>)}</tr></thead>
              <tbody>
                {students.map((st) => (
                  <tr key={st.id}>
                    <td>{st.name}</td>
                    {manualComps.map((c) => (
                      <td key={c.id}>
                        <input type="number" step="0.5" min="0" max="10" className="grade-input" style={{ width: 70 }}
                          defaultValue={manualByComp[`${c.id}|${st.id}`] ?? ''}
                          onBlur={(e) => saveManual(c.id, st.id, e.target.value)} />
                      </td>
                    ))}
                  </tr>
                ))}
                {students.length === 0 && <tr><td colSpan={manualComps.length + 1} className="muted center">Niciun student.</td></tr>}
              </tbody>
            </table>
          </div>
          <p className="muted" style={{ fontSize: 12, padding: '0 16px 12px' }}>Notele se salvează automat când ieși din câmp.</p>
        </section>
      )}

      {/* Compute */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title"><Icon name="calculate" /> Calcul note</h2>
          <div className="row-actions">
            <button type="button" className="btn btn-outline btn-sm" onClick={() => compute(false)} disabled={busy}>Calculează (preview)</button>
            <button type="button" className="btn btn-primary btn-sm" onClick={() => compute(true)} disabled={busy}>Calculează & salvează</button>
          </div>
        </div>
        {computeRows && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr><th>Student</th>{scheme.components.map((c, i) => <th key={i}>{c.name}</th>)}<th>Final</th><th>Status</th></tr>
              </thead>
              <tbody>
                {computeRows.rows.map((r) => (
                  <tr key={r.student_id}>
                    <td>{r.student_name}{r.note ? <span className="muted"> · {r.note}</span> : null}</td>
                    {scheme.components.map((c, i) => <td key={i} className="mono">{r.components[c.name] ?? '—'}</td>)}
                    <td className="mono"><strong>{r.final_stored ?? '—'}</strong> <span className="muted">({r.final_raw})</span></td>
                    <td><span className={`badge ${r.passed ? 'status-pass' : 'status-fail'}`}>{r.passed ? 'Promovat' : 'Picat'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {computeRows.unmatched_sheet_keys?.length > 0 && (
              <p className="muted" style={{ fontSize: 12, padding: '8px 16px' }}>
                Rânduri din sheet nepotrivite la niciun student: {computeRows.unmatched_sheet_keys.join(', ')}
              </p>
            )}
          </div>
        )}
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
