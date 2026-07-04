import { useEffect, useState } from 'react';
import { api } from '../../api';
import { lei } from '../../utils/format';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';
import { useToast } from '../../hooks/useToast';


export default function PlataTaxe() {
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(null);
  const { toast, flash } = useToast();

  const load = async () => {
    try { setData(await api.get('/api/tuition/me')); }
    catch (e) { console.error(e); }
  };
  useEffect(() => { load(); }, []);

  const pay = async (key) => {
    setBusy(key);
    try { setData(await api.post('/api/tuition/pay', { charge_key: key })); flash('success', 'Plată efectuată.'); }
    catch (e) { console.error(e); flash('error', e.message || 'Eroare la plată.'); }
    finally { setBusy(null); }
  };
  const payAdvance = async () => {
    setBusy('advance');
    try { setData(await api.post('/api/tuition/pay-advance')); flash('success', 'Toate tranșele au fost plătite cu reducere.'); }
    catch (e) { console.error(e); flash('error', e.message || 'Eroare la plată.'); }
    finally { setBusy(null); }
  };

  if (!data) return <div className="page"><section className="header-card"><h1 className="page-title">Plată taxe</h1></section></div>;

  const nothingOwed = !data.fee_paying && (data.restante?.length ?? 0) === 0;

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Plată taxe</h1>
        <p className="page-subtitle">Taxe de școlarizare și restanțe. Plata este simulată — apasă „Plătește".</p>
      </section>

      <div className="stats-row">
        <div className="stat-card"><div className="stat-label">Total de plată</div><div className="stat-value">{lei(data.total_due)}</div></div>
        <div className="stat-card"><div className="stat-label">Plătit</div><div className="stat-value">{lei(data.total_paid)}</div></div>
        <div className="stat-card"><div className="stat-label">Rămas de plată</div><div className="stat-value">{lei(data.outstanding)}</div></div>
      </div>

      {data.can_pay_all_advance && (
        <section className="card advance-banner">
          <div className="advance-main">
            <span className="facility-icon"><Icon name="bolt" /></span>
            <div>
              <div className="facility-name">Plătește toate tranșele în avans</div>
              <div className="muted">{lei(data.advance_amount)} în loc de {lei(Number(data.advance_amount) + Number(data.advance_saving))} — economisești {lei(data.advance_saving)} (−10%).</div>
            </div>
          </div>
          <button type="button" className="btn btn-primary" onClick={payAdvance} disabled={busy === 'advance'}>
            {busy === 'advance' ? <span className="spinner" /> : <><Icon name="bolt" /> Plătește tot (−10%)</>}
          </button>
        </section>
      )}

      {data.fee_paying && (
        <section className="card">
          <div className="card-header"><h2 className="card-title"><Icon name="school" /> Taxe de școlarizare</h2></div>
          <div className="fee-grid">
            {data.installments.map((c) => (
              <ChargeCard key={c.key} c={c} busy={busy} onPay={pay} />
            ))}
          </div>
        </section>
      )}

      {(data.restante?.length ?? 0) > 0 && (
        <section className="card">
          <div className="card-header"><h2 className="card-title"><Icon name="warning" /> Taxe restanțe</h2></div>
          <div className="fee-grid">
            {data.restante.map((c) => (
              <ChargeCard key={c.key} c={c} busy={busy} onPay={pay} />
            ))}
          </div>
        </section>
      )}

      {nothingOwed && (
        <section className="card"><div className="card-body muted center">Nu ai taxe de plătit. 🎉</div></section>
      )}

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}

function ChargeCard({ c, busy, onPay }) {
  return (
    <div className={`fee-card ${c.paid ? 'paid' : ''}`}>
      <div className="fee-card-head">
        <span className="fee-label">{c.label}</span>
        <span className="fee-amount">{lei(c.amount)}</span>
      </div>
      {c.paid ? (
        <span className="badge status-pass">
          <Icon name="check_circle" size={16} /> Plătit{c.paid_via === 'advance' ? ' (avans)' : ''}
        </span>
      ) : (
        <button type="button" className="btn btn-primary btn-sm" onClick={() => onPay(c.key)} disabled={busy === c.key}>
          {busy === c.key ? <span className="spinner" /> : <><Icon name="credit_card" /> Plătește</>}
        </button>
      )}
    </div>
  );
}
