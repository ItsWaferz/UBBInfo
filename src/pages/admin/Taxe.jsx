// Admin: complete tuition & fees statistic — who owes what and what they've paid.
import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { lei } from '../../utils/format';
import Icon from '../../components/Icon';


export default function Taxe() {
  const [data, setData] = useState(null);
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('all'); // all | outstanding | paid

  useEffect(() => {
    (async () => {
      try { setData(await api.get('/api/tuition/admin/overview')); }
      catch (e) { console.error(e); }
    })();
  }, []);

  const rows = useMemo(() => {
    const all = data?.rows ?? [];
    const term = q.trim().toLowerCase();
    return all.filter((r) => {
      if (filter === 'outstanding' && Number(r.outstanding) <= 0) return false;
      if (filter === 'paid' && Number(r.outstanding) > 0) return false;
      if (!term) return true;
      return (r.name || '').toLowerCase().includes(term) || (r.code || '').toLowerCase().includes(term);
    });
  }, [data, q, filter]);

  return (
    <div className="page" style={{ display: 'grid', gap: 20 }}>
      <section className="header-card">
        <h1 className="page-title">Statistică taxe</h1>
        <p className="page-subtitle">Cine ce taxe are de plătit (școlarizare + restanțe) și cât a plătit până acum.</p>
      </section>

      <div className="stats-row">
        <div className="stat-card"><div className="stat-label">Studenți cu taxe</div><div className="stat-value">{data?.students ?? '—'}</div></div>
        <div className="stat-card"><div className="stat-label">Total încasat</div><div className="stat-value">{lei(data?.total_paid)}</div></div>
        <div className="stat-card"><div className="stat-label">Total restant</div><div className="stat-value">{lei(data?.total_outstanding)}</div></div>
      </div>

      <section className="card">
        <div className="card-header" style={{ gap: 12 }}>
          <label className="field" style={{ maxWidth: 320, margin: 0 }}>
            <div className="input-wrap">
              <Icon name="search" className="input-icon" />
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Caută nume sau matricol" />
            </div>
          </label>
          <div className="facility-tabs">
            {[['all', 'Toți'], ['outstanding', 'Cu restanțe de plată'], ['paid', 'Achitat integral']].map(([k, l]) => (
              <button key={k} type="button" className={`facility-tab ${filter === k ? 'active' : ''}`} onClick={() => setFilter(k)}>{l}</button>
            ))}
          </div>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nume</th>
                <th>Matricol</th>
                <th>Finanțare</th>
                <th style={{ width: 120 }}>Școlarizare</th>
                <th style={{ width: 110 }}>Restanțe</th>
                <th style={{ width: 110 }}>Plătit</th>
                <th style={{ width: 110 }}>Rămas</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.student_id}>
                  <td>{r.name}</td>
                  <td className="mono">{r.code}</td>
                  <td>
                    <span className={`badge ${r.fee_paying ? 'badge-optional' : ''}`}>{r.financing}</span>
                  </td>
                  <td>{r.fee_paying ? `${r.installments_paid}/${r.installments_total}` : '—'}</td>
                  <td>{r.restante_total > 0 ? `${r.restante_paid}/${r.restante_total}` : '—'}</td>
                  <td className="mono">{lei(r.total_paid)}</td>
                  <td className="mono">
                    {Number(r.outstanding) > 0
                      ? <span className="text-danger">{lei(r.outstanding)}</span>
                      : <span className="badge status-pass">achitat</span>}
                  </td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan={7} className="muted center">Niciun student.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
