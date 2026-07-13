// Admin: manage student facilities (burse / tabere / camin) — configure
// capacities & dorms, mark social/special cases, generate ranked lists (PDF)
// and publish results to all applicants.
import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';
import { useToast } from '../../hooks/useToast';

export default function Facilitati() {
  const [overview, setOverview] = useState([]);
  const [dorms, setDorms] = useState([]);
  const [selected, setSelected] = useState('bursa_sociala');
  const [x, setX] = useState(0);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [dormsOpen, setDormsOpen] = useState(false);
  const { toast, flash } = useToast(3500);

  const loadOverview = async () => {
    try { setOverview(await api.get('/api/facilities/admin/overview') || []); }
    catch (e) { console.error(e); }
  };
  const loadDorms = async () => {
    try { setDorms(await api.get('/api/facilities/admin/dorms') || []); }
    catch (e) { console.error(e); }
  };
  useEffect(() => { loadOverview(); loadDorms(); }, []);

  const current = useMemo(() => overview.find((o) => o.key === selected), [overview, selected]);

  // Default X to the facility capacity when switching facility.
  useEffect(() => {
    if (current) { setX(current.capacity || 0); setResult(null); }
  }, [selected, current?.capacity]);

  const generate = async () => {
    setBusy(true);
    try { setResult(await api.post(`/api/facilities/${selected}/generate?x=${x}`)); }
    catch (e) { console.error(e); flash('error', e.message || 'Eroare la generare.'); }
    finally { setBusy(false); }
  };
  const downloadPdf = async () => {
    try { await api.download(`/api/facilities/${selected}/pdf?x=${x}`, { method: 'POST' }); }
    catch (e) { console.error(e); flash('error', 'Eroare la PDF.'); }
  };
  const publish = async () => {
    if (!window.confirm('Publici rezultatele? Statusul tuturor înscrișilor la această facilitate va fi actualizat.')) return;
    setBusy(true);
    try {
      const res = await api.post(`/api/facilities/${selected}/publish?x=${x}`);
      setResult(res);
      flash('success', `Publicat: ${res.accepted} admiși din ${res.applicants} înscriși.`);
      loadOverview();
    } catch (e) { console.error(e); flash('error', e.message || 'Eroare la publicare.'); }
    finally { setBusy(false); }
  };

  // Order is implicit: a dorm's position in the list becomes its sort_order, and
  // dorms are always active (the active toggle was removed).
  const saveDorm = async (d, index) => {
    if (!String(d.name || '').trim()) { flash('error', 'Numele căminului este obligatoriu.'); return; }
    try {
      await api.post('/api/facilities/admin/dorms', {
        id: d.id, name: d.name.trim(), capacity: Number(d.capacity) || 0,
        sort_order: index, active: true,
      });
      flash('success', 'Cămin salvat.'); loadDorms(); loadOverview();
    } catch (e) { console.error(e); flash('error', 'Eroare la salvare cămin.'); }
  };
  const addDorm = () =>
    setDorms((ds) => [...ds, { id: null, name: '', capacity: 0 }]);
  const deleteDorm = async (d) => {
    if (!d.id) { setDorms((ds) => ds.filter((x) => x !== d)); return; }
    if (!window.confirm('Ștergi căminul?')) return;
    try { await api.del(`/api/facilities/admin/dorms/${d.id}`); loadDorms(); loadOverview(); }
    catch (e) { console.error(e); flash('error', 'Eroare la ștergere.'); }
  };
  const setDormField = (i, k, v) => setDorms((ds) => ds.map((d, j) => j === i ? { ...d, [k]: v } : d));

  const saveSetting = async (key, capacity, reserved) => {
    try {
      await api.put(`/api/facilities/admin/settings/${key}`, { capacity, reserved_percent: reserved });
      flash('success', 'Setare salvată.'); loadOverview();
    } catch (e) { console.error(e); flash('error', 'Eroare la setare.'); }
  };

  const showReserved = selected === 'camin' || selected === 'tabara';

  return (
    <div className="page facilitati-page">
      <section className="header-card">
        <h1 className="page-title">Facilități studențești</h1>
        <p className="page-subtitle">
          Burse, tabere și cămin: configurează locurile, marchează cazurile sociale/speciale,
          generează listele cu primii X studenți (după medie) și publică rezultatele.
        </p>
      </section>

      {/* Generate & publish — all settings scoped to the selected facility */}
      <section className="card">
        <div className="card-header"><h2 className="card-title"><Icon name="checklist" /> Generare & publicare liste</h2></div>
        <div className="card-body" style={{ display: 'grid', gap: 16 }}>
          <label className="field facility-select">
            <span className="field-label">Facilitate</span>
            <div className="input-wrap">
              <select className="select-bare" value={selected} onChange={(e) => setSelected(e.target.value)}>
                {overview.map((o) => (
                  <option key={o.key} value={o.key}>{o.label} · {o.applicants} înscriși</option>
                ))}
              </select>
            </div>
          </label>

          {current && (
            <>
              {/* Per-facility capacity, reservation & how-many-admitted */}
              <FacilitySettings key={selected} o={current} showReserved={showReserved} x={x} setX={setX} onSave={saveSetting} />

              {/* Dorms — only for cămin, collapsed by default */}
              {selected === 'camin' && (
                <div className={`users-accordion ${dormsOpen ? 'open' : ''}`}>
                  <button type="button" className={`users-accordion-trigger${dormsOpen ? ' open' : ''}`}
                    onClick={() => setDormsOpen((v) => !v)}>
                    <div className="users-accordion-left">
                      <Icon name="apartment" />
                      <span className="users-accordion-label">Cămine</span>
                      <span className="users-accordion-count">{dorms.length}</span>
                    </div>
                    <Icon name="expand_more" className={`users-accordion-chevron${dormsOpen ? ' rotated' : ''}`} />
                  </button>
                  {dormsOpen && (
                    <div className="users-accordion-body" style={{ padding: 12 }}>
                      <div className="dorm-list">
                        {dorms.map((d, i) => (
                          <div className="dorm-row" key={d.id || `new-${i}`}>
                            <input className="dorm-name" value={d.name} placeholder="Nume cămin"
                              onChange={(e) => setDormField(i, 'name', e.target.value)} />
                            <input className="dorm-cap" type="number" min="0" value={d.capacity} placeholder="Locuri"
                              onChange={(e) => setDormField(i, 'capacity', e.target.value)} />
                            <div className="row-actions">
                              <button type="button" className="btn btn-primary btn-sm" onClick={() => saveDorm(d, i)} aria-label="Salvează"><Icon name="save" /></button>
                              <button type="button" className="icon-btn" onClick={() => deleteDorm(d)} aria-label="Șterge"><Icon name="delete" /></button>
                            </div>
                          </div>
                        ))}
                        {dorms.length === 0 && <p className="muted" style={{ margin: 0 }}>Niciun cămin încă.</p>}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
                        <button type="button" className="btn btn-outline btn-sm" onClick={addDorm}><Icon name="add" /> Adaugă cămin</button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Bottom: generation actions */}
              <div className="facility-controls facility-actions">
                <div className="row-actions">
                  <button type="button" className="btn btn-outline btn-sm" onClick={generate} disabled={busy}>
                    <Icon name="preview" /> Generează
                  </button>
                  <button type="button" className="btn btn-outline btn-sm" onClick={downloadPdf} disabled={busy}>
                    <Icon name="picture_as_pdf" /> PDF
                  </button>
                  <button type="button" className="btn btn-primary btn-sm" onClick={publish} disabled={busy}>
                    <Icon name="task_alt" /> Publică rezultatele
                  </button>
                </div>
              </div>
            </>
          )}

          {result && (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: 50 }}>Nr.</th>
                    <th>Cod academic</th>
                    <th>Nume</th>
                    <th className="td-num" style={{ width: 70 }}>Media</th>
                    <th>Rezultat</th>
                    {showReserved && <th style={{ width: 80 }}>Rezervat</th>}
                    <th style={{ width: 90 }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {result.rows.map((r, i) => (
                    <tr key={r.student_id || i} className={r.status === 'accepted' ? '' : 'row-muted'}>
                      <td>{r.rank ?? '—'}</td>
                      <td className="mono">{r.code || '—'}</td>
                      <td>{r.name}</td>
                      <td className="mono td-num">{r.media ?? '—'}</td>
                      <td>{r.result || '—'}</td>
                      {showReserved && <td>{r.reserved ? <span className="badge badge-optional">rezervat</span> : ''}</td>}
                      <td>
                        <span className={`badge ${r.status === 'accepted' ? 'status-pass' : 'status-fail'}`}>
                          {r.status === 'accepted' ? 'Admis' : 'Neadmis'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {result.rows.length === 0 && <tr><td colSpan={showReserved ? 7 : 6} className="muted center">Niciun înscris.</td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}

/** Inline capacity + reservation editor (+ the X generation param) for the selected facility. */
function FacilitySettings({ o, showReserved, x, setX, onSave }) {
  const isCamin = o.key === 'camin';
  const [cap, setCap] = useState(o.capacity ?? '');
  const [res, setRes] = useState(o.reserved_percent ?? 0);
  return (
    <div className="facility-settings">
      <label className="field">
        <span className="field-label">Capacitate</span>
        {isCamin
          ? <div className="input-wrap input-readonly" title="Suma locurilor din cămine"><span className="ro-value">{o.capacity ?? '—'}</span></div>
          : <input type="number" min="0" value={cap} onChange={(e) => setCap(e.target.value)} />}
      </label>
      {showReserved && (
        <label className="field">
          <span className="field-label">Rezervat (%)</span>
          <input type="number" min="0" max="100" step="1" value={res} onChange={(e) => setRes(e.target.value)} />
        </label>
      )}
      <label className="field">
        <span className="field-label">Câți admiți (X)</span>
        <input type="number" min="0" value={x} onChange={(e) => setX(e.target.value)} />
      </label>
      <button type="button" className="btn btn-outline btn-sm"
        onClick={() => onSave(o.key, isCamin ? null : (Number(cap) || 0), Number(res) || 0)}>
        <Icon name="save" /> Salvează setările
      </button>
    </div>
  );
}
