import { useEffect, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';

const ORDER = ['bursa_sociala', 'bursa_merit', 'tabara', 'camin'];
const ICON = {
  bursa_sociala: 'volunteer_activism',
  bursa_merit: 'workspace_premium',
  tabara: 'beach_access',
  camin: 'apartment',
};

function StatusBadge({ a }) {
  if (a.status === 'accepted')
    return <span className="badge status-pass">Admis{a.rank ? ` · loc ${a.rank}` : ''}{a.result ? ` · ${a.result}` : ''}</span>;
  if (a.status === 'rejected')
    return <span className="badge status-fail">Neadmis (listă de așteptare)</span>;
  if (a.status === 'pending')
    return <span className="badge status-inprogress">În așteptare</span>;
  return <span className="muted">Neînscris</span>;
}

export default function FacilitiesCard() {
  const [apps, setApps] = useState([]);
  const [dorms, setDorms] = useState([]);
  const [caminFormOpen, setCaminFormOpen] = useState(false);
  const [prefs, setPrefs] = useState({});          // dormId -> priority string
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);

  const load = async () => {
    try {
      const [me, cfg] = await Promise.all([
        api.get('/api/facilities/me'),
        api.get('/api/facilities/config'),
      ]);
      setApps(me || []);
      setDorms(cfg?.dorms || []);
    } catch (err) {
      console.error('Load facilities failed:', err);
    }
  };

  useEffect(() => { load(); }, []);

  const byKey = Object.fromEntries(apps.map((a) => [a.facility, a]));

  const applySimple = async (facility) => {
    setBusy(facility);
    setError(null);
    try { await api.post(`/api/facilities/${facility}/apply`, {}); await load(); }
    catch (err) { console.error(err); setError(err.message || 'Eroare la înscriere.'); }
    finally { setBusy(null); }
  };

  const withdraw = async (facility) => {
    setBusy(facility);
    setError(null);
    try { await api.del(`/api/facilities/${facility}/apply`); await load(); }
    catch (err) { console.error(err); setError(err.message || 'Eroare la retragere.'); }
    finally { setBusy(null); }
  };

  const openCamin = (existing) => {
    const init = {};
    const order = existing?.dorm_prefs || [];
    dorms.forEach((d) => {
      const idx = order.indexOf(d.id);
      init[d.id] = idx >= 0 ? String(idx + 1) : '';
    });
    setPrefs(init);
    setCaminFormOpen(true);
  };

  const submitCamin = async () => {
    // Ordered dorm ids by the priority number the student set (blank = excluded).
    const ordered = dorms
      .map((d) => ({ id: d.id, p: parseInt(prefs[d.id], 10) }))
      .filter((x) => !Number.isNaN(x.p))
      .sort((a, b) => a.p - b.p)
      .map((x) => x.id);
    setBusy('camin');
    setError(null);
    try {
      await api.post('/api/facilities/camin/apply', { dorm_prefs: ordered });
      setCaminFormOpen(false);
      await load();
    } catch (err) { console.error(err); setError(err.message || 'Eroare la înscriere.'); }
    finally { setBusy(null); }
  };

  const sorted = ORDER.map((k) => byKey[k]).filter(Boolean);

  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          <Icon name="volunteer_activism" />
          Facilități studențești
        </h2>
      </div>
      {error && (
        <p className="text-danger" style={{ padding: '10px 20px 0', margin: 0, fontSize: 13 }}>{error}</p>
      )}
      <div className="facility-list">
        {sorted.map((a) => (
          <div key={a.facility} className="facility-row">
            <div className="facility-main">
              <span className="facility-icon"><Icon name={ICON[a.facility]} /></span>
              <div>
                <div className="facility-name">{a.label}</div>
                <StatusBadge a={a} />
              </div>
            </div>
            <div className="facility-actions">
              {a.status === 'none' && (
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => (a.facility === 'camin' ? openCamin(a) : applySimple(a.facility))}
                  disabled={busy === a.facility}
                >
                  <Icon name="add" /> Înscrie-te
                </button>
              )}
              {a.status === 'pending' && (
                <>
                  {a.facility === 'camin' && (
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => openCamin(a)}>
                      <Icon name="edit" /> Preferințe
                    </button>
                  )}
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => withdraw(a.facility)} disabled={busy === a.facility}>
                    <Icon name="close" /> Retrage
                  </button>
                </>
              )}
            </div>

            {caminFormOpen && a.facility === 'camin' && (
              <div className="facility-camin-form">
                <p className="muted" style={{ fontSize: 13, margin: '4px 0 10px' }}>
                  Stabilește ordinea preferințelor (1 = prima opțiune). Lasă gol căminele pe care nu le vrei.
                </p>
                <div className="facility-dorm-grid">
                  {dorms.map((d) => (
                    <label key={d.id} className="facility-dorm">
                      <span>{d.name}</span>
                      <input
                        type="number" min="1" max={dorms.length}
                        value={prefs[d.id] ?? ''}
                        onChange={(e) => setPrefs((p) => ({ ...p, [d.id]: e.target.value }))}
                        placeholder="—"
                      />
                    </label>
                  ))}
                </div>
                <div className="form-actions" style={{ marginTop: 12 }}>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setCaminFormOpen(false)}>Anulează</button>
                  <button type="button" className="btn btn-primary btn-sm" onClick={submitCamin} disabled={busy === 'camin'}>
                    <Icon name="check" /> Salvează înscrierea
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
