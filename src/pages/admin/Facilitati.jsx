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
  const [users, setUsers] = useState([]);
  const [userSearch, setUserSearch] = useState('');
  const [busy, setBusy] = useState(false);
  const { toast, flash } = useToast(3500);

  const loadOverview = async () => {
    try { setOverview(await api.get('/api/facilities/admin/overview') || []); }
    catch (e) { console.error(e); }
  };
  const loadDorms = async () => {
    try { setDorms(await api.get('/api/facilities/admin/dorms') || []); }
    catch (e) { console.error(e); }
  };
  // Server-filtered: flagged-only by default, ?q= while searching (debounced) —
  // never ships the whole user table just for this tab.
  useEffect(() => {
    const term = userSearch.trim();
    const t = setTimeout(async () => {
      try {
        const url = term
          ? `/api/users?q=${encodeURIComponent(term)}`
          : '/api/users?flagged=true';
        setUsers(await api.get(url) || []);
      } catch (e) { console.error(e); }
    }, term ? 300 : 0);
    return () => clearTimeout(t);
  }, [userSearch]);

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

  const saveDorm = async (d) => {
    try {
      await api.post('/api/facilities/admin/dorms', {
        id: d.id, name: d.name, capacity: Number(d.capacity) || 0,
        sort_order: Number(d.sort_order) || 0, active: d.active,
      });
      flash('success', 'Cămin salvat.'); loadDorms(); loadOverview();
    } catch (e) { console.error(e); flash('error', 'Eroare la salvare cămin.'); }
  };
  const addDorm = () =>
    setDorms((ds) => [...ds, { id: null, name: 'Cămin nou', capacity: 0, sort_order: ds.length + 1, active: true }]);
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

  const toggleFlag = async (u, field) => {
    const next = !u.profile?.[field];
    try {
      await api.put(`/api/users/${u.profile.id}`, { [field]: String(next) });
      setUsers((us) => us.map((it) => it.profile.id === u.profile.id
        ? { ...it, profile: { ...it.profile, [field]: next } } : it));
    } catch (e) { console.error(e); flash('error', 'Eroare la marcare.'); }
  };

  // Filtering happens server-side (?flagged / ?q); this is just the result.
  const filteredUsers = users;

  const label = (k) => overview.find((o) => o.key === k)?.label || k;
  const showReserved = selected === 'camin' || selected === 'tabara';

  return (
    <div className="page" style={{ display: 'grid', gap: 20 }}>
      <section className="header-card">
        <h1 className="page-title">Facilități studențești</h1>
        <p className="page-subtitle">
          Burse, tabere și cămin: configurează locurile, marchează cazurile sociale/speciale,
          generează listele cu primii X studenți (după medie) și publică rezultatele.
        </p>
      </section>

      {/* Generate & publish */}
      <section className="card">
        <div className="card-header"><h2 className="card-title"><Icon name="checklist" /> Generare & publicare liste</h2></div>
        <div className="card-body" style={{ display: 'grid', gap: 16 }}>
          <div className="facility-tabs">
            {overview.map((o) => (
              <button key={o.key} type="button"
                className={`facility-tab ${selected === o.key ? 'active' : ''}`}
                onClick={() => setSelected(o.key)}>
                {o.label}
                <span className="facility-tab-count">{o.applicants}</span>
              </button>
            ))}
          </div>

          {current && (
            <div className="facility-controls">
              <div className="muted">
                {current.applicants} înscriși · capacitate {current.capacity ?? '—'}
                {Number(current.reserved_percent) > 0 ? ` · rezervat ${current.reserved_percent}%` : ''}
                {current.published ? ' · publicat' : ''}
              </div>
              <label className="field" style={{ maxWidth: 160 }}>
                <span className="field-label">Câți admiți (X)</span>
                <input type="number" min="0" value={x} onChange={(e) => setX(e.target.value)} />
              </label>
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
          )}

          {result && (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th style={{ width: 50 }}>Nr.</th>
                    <th>Cod academic</th>
                    <th>Nume</th>
                    <th style={{ width: 70 }}>Media</th>
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
                      <td className="mono">{r.media ?? '—'}</td>
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

      {/* Dorms config */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title"><Icon name="apartment" /> Cămine</h2>
          <button type="button" className="btn btn-outline btn-sm" onClick={addDorm}><Icon name="add" /> Adaugă cămin</button>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Nume</th><th style={{ width: 110 }}>Capacitate</th><th style={{ width: 90 }}>Ordine</th><th style={{ width: 80 }}>Activ</th><th style={{ width: 140 }}></th></tr></thead>
            <tbody>
              {dorms.map((d, i) => (
                <tr key={d.id || `new-${i}`}>
                  <td><input value={d.name} onChange={(e) => setDormField(i, 'name', e.target.value)} /></td>
                  <td><input type="number" min="0" value={d.capacity} onChange={(e) => setDormField(i, 'capacity', e.target.value)} /></td>
                  <td><input type="number" min="0" value={d.sort_order} onChange={(e) => setDormField(i, 'sort_order', e.target.value)} /></td>
                  <td><input type="checkbox" checked={!!d.active} onChange={(e) => setDormField(i, 'active', e.target.checked)} /></td>
                  <td className="row-actions">
                    <button type="button" className="btn btn-primary btn-sm" onClick={() => saveDorm(d)}><Icon name="save" /></button>
                    <button type="button" className="icon-btn" onClick={() => deleteDorm(d)} aria-label="Șterge"><Icon name="delete" /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Facility capacities */}
      <section className="card">
        <div className="card-header"><h2 className="card-title"><Icon name="tune" /> Capacități & rezervări</h2></div>
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Facilitate</th><th style={{ width: 140 }}>Capacitate</th><th style={{ width: 150 }}>Rezervat (%)</th><th style={{ width: 120 }}></th></tr></thead>
            <tbody>
              {overview.map((o) => (
                <SettingRow key={o.key} o={o} onSave={saveSetting} />
              ))}
            </tbody>
          </table>
        </div>
        <p className="muted" style={{ fontSize: 12, padding: '0 16px 12px' }}>
          La cămin capacitatea = suma locurilor din cămine (se editează mai sus). Rezervarea: cămin = cazuri sociale, tabără = cazuri speciale.
        </p>
      </section>

      {/* Social / special case manager */}
      <section className="card">
        <div className="card-header"><h2 className="card-title"><Icon name="how_to_reg" /> Cazuri sociale / speciale</h2></div>
        <div className="card-body">
          <label className="field" style={{ maxWidth: 360 }}>
            <span className="field-label">Caută student (nume sau matricol)</span>
            <div className="input-wrap">
              <Icon name="search" className="input-icon" />
              <input value={userSearch} onChange={(e) => setUserSearch(e.target.value)} placeholder="Lasă gol = doar cei deja marcați" />
            </div>
          </label>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Nume</th><th>Matricol</th><th style={{ width: 130 }}>Caz social</th><th style={{ width: 130 }}>Caz special</th></tr></thead>
            <tbody>
              {filteredUsers.map((u) => (
                <tr key={u.profile.id}>
                  <td>{u.profile.full_name || '—'}</td>
                  <td className="mono">{u.profile.student_id || '—'}</td>
                  <td><label className="course-check"><input type="checkbox" checked={!!u.profile.is_social_case} onChange={() => toggleFlag(u, 'is_social_case')} /><span>social</span></label></td>
                  <td><label className="course-check"><input type="checkbox" checked={!!u.profile.is_special_case} onChange={() => toggleFlag(u, 'is_special_case')} /><span>special</span></label></td>
                </tr>
              ))}
              {filteredUsers.length === 0 && <tr><td colSpan={4} className="muted center">Niciun student.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}

function SettingRow({ o, onSave }) {
  const [cap, setCap] = useState(o.capacity ?? '');
  const [res, setRes] = useState(o.reserved_percent ?? 0);
  const isCamin = o.key === 'camin';
  return (
    <tr>
      <td>{o.label}</td>
      <td>{isCamin ? <span className="muted">{o.capacity ?? '—'} (din cămine)</span>
        : <input type="number" min="0" value={cap} onChange={(e) => setCap(e.target.value)} />}</td>
      <td><input type="number" min="0" max="100" step="1" value={res} onChange={(e) => setRes(e.target.value)} /></td>
      <td>
        <button type="button" className="btn btn-primary btn-sm"
          onClick={() => onSave(o.key, isCamin ? null : (Number(cap) || 0), Number(res) || 0)}>
          <Icon name="save" /> Salvează
        </button>
      </td>
    </tr>
  );
}
