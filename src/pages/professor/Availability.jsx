import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const DAYS = [
  { n: 1, label: 'Luni' },
  { n: 2, label: 'Marți' },
  { n: 3, label: 'Miercuri' },
  { n: 4, label: 'Joi' },
  { n: 5, label: 'Vineri' },
];
// 2-hour blocks 08:00–20:00
const BLOCKS = [8, 10, 12, 14, 16, 18];

// Cell cycle: '' -> available -> preferred -> unavailable -> ''
const NEXT = { '': 'available', available: 'preferred', preferred: 'unavailable', unavailable: '' };
const CELL_LABEL = { available: 'Disponibil', preferred: 'Preferat', unavailable: 'Indisponibil' };
const CELL_CLASS = {
  available: 'avail-on',
  preferred: 'avail-pref',
  unavailable: 'avail-off',
};

const hh = (n) => `${String(n).padStart(2, '0')}:00:00`;
const hourOf = (t) => (t ? parseInt(t.slice(0, 2), 10) : null);

export default function Availability() {
  const { user, currentRole } = useAuth();
  // grid[`${day}-${startHour}`] = '' | 'available' | 'preferred' | 'unavailable'
  const [grid, setGrid] = useState({});
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      try {
        const windows = await api.get('/api/availability/me');
        if (!active) return;
        const next = {};
        for (const w of windows || []) {
          const start = hourOf(w.start_time);
          const end = hourOf(w.end_time);
          for (let h = start; h < end; h += 2) {
            next[`${w.day_of_week}-${h}`] = w.preference || 'available';
          }
        }
        setGrid(next);
      } catch (err) {
        console.error('Load availability failed:', err);
      }
    })();
    return () => {
      active = false;
    };
  }, [user]);

  const cycle = (day, hour) =>
    setGrid((g) => {
      const key = `${day}-${hour}`;
      return { ...g, [key]: NEXT[g[key] || ''] };
    });

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const save = async () => {
    setSaving(true);
    // Each marked 2h block becomes one window.
    const windows = [];
    for (const [key, pref] of Object.entries(grid)) {
      if (!pref) continue;
      const [day, hour] = key.split('-').map(Number);
      windows.push({
        day_of_week: day,
        start_time: hh(hour),
        end_time: hh(hour + 2),
        preference: pref,
      });
    }
    try {
      await api.put('/api/availability/me', windows);
      flash('success', 'Disponibilitate salvată.');
    } catch (err) {
      console.error('Save availability failed:', err);
      flash('error', 'Eroare la salvare.');
    } finally {
      setSaving(false);
    }
  };

  if (currentRole && currentRole !== 'profesor') return <Navigate to="/" replace />;

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Disponibilitate</h1>
        <p className="page-subtitle">
          Apasă pe o celulă pentru a comuta: Disponibil → Preferat → Indisponibil → liber.
          Lasă gol = fără constrângere (generatorul te poate plasa oricând).
        </p>
      </section>

      <section className="card">
        <div className="card-body" style={{ overflowX: 'auto' }}>
          <table className="data-table avail-grid">
            <thead>
              <tr>
                <th>Interval</th>
                {DAYS.map((d) => (
                  <th key={d.n} style={{ textAlign: 'center' }}>{d.label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {BLOCKS.map((h) => (
                <tr key={h}>
                  <td className="mono">{`${String(h).padStart(2, '0')}:00–${String(h + 2).padStart(2, '0')}:00`}</td>
                  {DAYS.map((d) => {
                    const state = grid[`${d.n}-${h}`] || '';
                    return (
                      <td key={d.n} style={{ textAlign: 'center', padding: 4 }}>
                        <button
                          type="button"
                          className={`avail-cell ${CELL_CLASS[state] || ''}`}
                          onClick={() => cycle(d.n, h)}
                          style={{
                            width: '100%', minWidth: 90, padding: '8px 4px',
                            borderRadius: 8, border: '1px solid var(--outline-variant)',
                            cursor: 'pointer', fontSize: 12,
                            background: state === 'available' ? 'var(--success, #1b5e20)'
                              : state === 'preferred' ? 'var(--primary, #1565c0)'
                              : state === 'unavailable' ? 'var(--error, #b71c1c)'
                              : 'transparent',
                            color: state ? '#fff' : 'var(--on-surface)',
                          }}
                        >
                          {state ? CELL_LABEL[state] : '—'}
                        </button>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>

          <div className="form-actions" style={{ marginTop: 16 }}>
            <button type="button" className="btn btn-primary" onClick={save} disabled={saving}>
              {saving ? <span className="spinner" /> : <><Icon name="save" /> Salvează disponibilitatea</>}
            </button>
          </div>
        </div>
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
