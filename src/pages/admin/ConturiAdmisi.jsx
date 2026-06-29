// Admin: automatic account generation for admitted candidates (feature #4).
// Upload a CSV/XLSX → preview (proposed emails + validation) → create accounts.
import { useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const STATUS_BADGE = {
  OK: 'status-pass',
  CREATED: 'status-pass',
  DUPLICATE: 'badge-seminar',
  SKIPPED: 'badge-seminar',
  INVALID: 'status-fail',
  ERROR: 'status-fail',
};

export default function ConturiAdmisi() {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [report, setReport] = useState(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 4000);
  };

  const onPick = (e) => {
    setFile(e.target.files?.[0] || null);
    setPreview(null);
    setReport(null);
  };

  const doPreview = async () => {
    if (!file) return;
    setBusy(true);
    setReport(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      setPreview(await api.upload('/api/admisi/preview', fd));
    } catch (err) {
      console.error(err);
      flash('error', err.message || 'Eroare la previzualizare.');
    } finally {
      setBusy(false);
    }
  };

  const doImport = async () => {
    if (!file) return;
    if (!window.confirm(`Creezi ${preview?.ok ?? 0} conturi? Operația nu poate fi anulată automat.`)) return;
    setBusy(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await api.upload('/api/admisi/import', fd);
      setReport(res);
      flash('success', `${res.created} conturi create, ${res.skipped} sărite, ${res.errors} erori.`);
    } catch (err) {
      console.error(err);
      flash('error', err.message || 'Eroare la creare.');
    } finally {
      setBusy(false);
    }
  };

  const downloadReport = () => {
    if (!report) return;
    const header = 'rand,nume,email,parola,status,mesaj';
    const lines = report.rows.map((r) =>
      [r.row, r.full_name, r.email || '', r.password || '', r.status, (r.message || '').replace(/[\n,]/g, ' ')]
        .map((x) => `"${String(x ?? '')}"`).join(',')
    );
    const blob = new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'conturi_admisi.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          <Icon name="group_add" />
          Generare conturi admiși
        </h2>
      </div>

      <div className="card-body">
        <p className="muted" style={{ marginBottom: 12 }}>
          Încarcă lista admișilor (CSV sau Excel). Coloane așteptate: <strong>nume</strong>,{' '}
          <strong>prenume</strong>, <strong>cnp</strong> (obligatorii) + opțional specializare,
          grupă, finanțare, an, matricol, facultate, telefon, email. Email-ul instituțional și
          parola se generează automat.
        </p>

        <div className="form-actions" style={{ justifyContent: 'flex-start', gap: 12, flexWrap: 'wrap' }}>
          <input type="file" accept=".csv,.xlsx,.xls" onChange={onPick} />
          <button type="button" className="btn btn-outline" onClick={doPreview} disabled={!file || busy}>
            {busy && !report ? <span className="spinner" /> : <><Icon name="preview" /> Previzualizează</>}
          </button>
          {preview && (
            <button type="button" className="btn btn-primary" onClick={doImport}
              disabled={busy || preview.ok === 0 || !preview.provisioning_ready}>
              <Icon name="person_add" /> Creează {preview.ok} conturi
            </button>
          )}
        </div>

        {preview && !preview.provisioning_ready && (
          <p className="modal-error" style={{ marginTop: 12 }}>
            ⚠️ Cheia <code>SUPABASE_SERVICE_ROLE_KEY</code> nu e configurată pe backend — pot doar
            previzualiza, nu și crea conturi.
          </p>
        )}

        {/* ---- Preview ---- */}
        {preview && (
          <div style={{ marginTop: 16 }}>
            <p className="muted">
              Total {preview.total} · <strong>{preview.ok} valide</strong> · {preview.duplicate} duplicate · {preview.invalid} invalide
            </p>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr><th>Rând</th><th>Nume</th><th>CNP</th><th>Grupă</th><th>Email propus</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {preview.rows.map((r, i) => (
                    <tr key={i}>
                      <td>{r.row}</td>
                      <td>{r.full_name}</td>
                      <td className="mono">{r.cnp}</td>
                      <td>{r.group_name || '—'}</td>
                      <td className="mono">{r.proposed_email || '—'}</td>
                      <td>
                        <span className={`badge ${STATUS_BADGE[r.status] || ''}`}>{r.status}</span>
                        {r.message ? <span className="muted"> {r.message}</span> : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ---- Import report ---- */}
        {report && (
          <div style={{ marginTop: 24 }}>
            <div className="card-header" style={{ paddingLeft: 0 }}>
              <h3 className="card-title"><Icon name="task_alt" /> Raport import</h3>
              <button type="button" className="btn btn-outline btn-sm" onClick={downloadReport}>
                <Icon name="download" /> Descarcă CSV (cu parole)
              </button>
            </div>
            <p className="muted">
              <strong>{report.created} create</strong> · {report.skipped} sărite · {report.errors} erori
            </p>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr><th>Rând</th><th>Nume</th><th>Email</th><th>Parolă</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {report.rows.map((r, i) => (
                    <tr key={i}>
                      <td>{r.row}</td>
                      <td>{r.full_name}</td>
                      <td className="mono">{r.email || '—'}</td>
                      <td className="mono">{r.password || '—'}</td>
                      <td>
                        <span className={`badge ${STATUS_BADGE[r.status] || ''}`}>{r.status}</span>
                        {r.message ? <span className="muted"> {r.message}</span> : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="muted" style={{ fontSize: 12, marginTop: 8 }}>
              ⚠️ Parolele sunt afișate o singură dată — descarcă CSV-ul ca să le comunici studenților.
            </p>
          </div>
        )}
      </div>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </section>
  );
}
