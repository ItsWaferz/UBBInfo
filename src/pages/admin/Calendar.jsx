import { useEffect, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK_SEM = { academic_year: '', semester: '1', start_date: '', end_date: '' };
const BLANK_VAC = { name: '', start_date: '', end_date: '' };

const fmtDate = (d) => {
  if (!d) return '—';
  const [y, m, day] = d.split('-');
  return `${day}.${m}.${y}`;
};

export default function Calendar() {
  const [semesters, setSemesters] = useState([]);
  const [vacations, setVacations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [semForm, setSemForm] = useState(BLANK_SEM);
  const [semEditId, setSemEditId] = useState(null);
  const [vacForm, setVacForm] = useState(BLANK_VAC);
  const [vacEditId, setVacEditId] = useState(null);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const load = async () => {
    setLoading(true);
    try {
      const [sems, vacs] = await Promise.all([
        api.get('/api/semester-config'),
        api.get('/api/vacations'),
      ]);
      setSemesters(sems || []);
      setVacations(vacs || []);
    } catch (err) {
      console.error('Load calendar failed:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // ----- semesters -----
  const submitSem = async (e) => {
    e.preventDefault();
    if (!semForm.academic_year.trim() || !semForm.start_date) {
      flash('error', 'Anul universitar și data de start sunt obligatorii.');
      return;
    }
    const payload = {
      academic_year: semForm.academic_year.trim(),
      semester: Number(semForm.semester),
      start_date: semForm.start_date,
      end_date: semForm.end_date || null,
    };
    try {
      if (semEditId) await api.put(`/api/semester-config/${semEditId}`, payload);
      else await api.post('/api/semester-config', payload);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la salvarea semestrului.');
      return;
    }
    flash('success', semEditId ? 'Semestru actualizat.' : 'Semestru adăugat.');
    setSemForm(BLANK_SEM);
    setSemEditId(null);
    load();
  };

  const editSem = (s) => {
    setSemEditId(s.id);
    setSemForm({
      academic_year: s.academic_year,
      semester: String(s.semester),
      start_date: s.start_date || '',
      end_date: s.end_date || '',
    });
  };

  const deleteSem = async (s) => {
    if (!window.confirm(`Ștergi configurarea ${s.academic_year} sem ${s.semester}?`)) return;
    try {
      await api.del(`/api/semester-config/${s.id}`);
    } catch (err) {
      console.error(err);
      return flash('error', 'Eroare la ștergere.');
    }
    flash('success', 'Șters.');
    if (semEditId === s.id) {
      setSemEditId(null);
      setSemForm(BLANK_SEM);
    }
    load();
  };

  // ----- vacations -----
  const submitVac = async (e) => {
    e.preventDefault();
    if (!vacForm.name.trim() || !vacForm.start_date || !vacForm.end_date) {
      flash('error', 'Toate câmpurile vacanței sunt obligatorii.');
      return;
    }
    const payload = {
      name: vacForm.name.trim(),
      start_date: vacForm.start_date,
      end_date: vacForm.end_date,
    };
    try {
      if (vacEditId) await api.put(`/api/vacations/${vacEditId}`, payload);
      else await api.post('/api/vacations', payload);
    } catch (err) {
      console.error(err);
      flash('error', 'Eroare la salvarea vacanței.');
      return;
    }
    flash('success', vacEditId ? 'Vacanță actualizată.' : 'Vacanță adăugată.');
    setVacForm(BLANK_VAC);
    setVacEditId(null);
    load();
  };

  const editVac = (v) => {
    setVacEditId(v.id);
    setVacForm({ name: v.name, start_date: v.start_date, end_date: v.end_date });
  };

  const deleteVac = async (v) => {
    if (!window.confirm(`Ștergi vacanța „${v.name}"?`)) return;
    try {
      await api.del(`/api/vacations/${v.id}`);
    } catch (err) {
      console.error(err);
      return flash('error', 'Eroare la ștergere.');
    }
    flash('success', 'Șters.');
    if (vacEditId === v.id) {
      setVacEditId(null);
      setVacForm(BLANK_VAC);
    }
    load();
  };

  if (loading) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă calendarul…</div>
      </section>
    );
  }

  return (
    <div className="admin-section-grid calendar-page">
      {/* Semesters */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="calendar_month" />
            Ani universitari / Semestre
          </h2>
        </div>
        <form className="card-body" onSubmit={submitSem}>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">An universitar</span>
              <div className="input-wrap">
                <input
                  type="text"
                  value={semForm.academic_year}
                  onChange={(e) => setSemForm({ ...semForm, academic_year: e.target.value })}
                  placeholder="ex. 2026-2027"
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Semestru</span>
              <div className="input-wrap">
                <select
                  className="select-bare"
                  value={semForm.semester}
                  onChange={(e) => setSemForm({ ...semForm, semester: e.target.value })}
                >
                  <option value="1">Semestrul 1</option>
                  <option value="2">Semestrul 2</option>
                </select>
              </div>
            </label>
          </div>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Data de start</span>
              <div className="input-wrap">
                <input
                  type="date"
                  style={{ textTransform: 'uppercase' }}
                  value={semForm.start_date}
                  onChange={(e) => setSemForm({ ...semForm, start_date: e.target.value })}
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Data de final</span>
              <div className="input-wrap">
                <input
                  type="date"
                  style={{ textTransform: 'uppercase' }}
                  value={semForm.end_date}
                  onChange={(e) => setSemForm({ ...semForm, end_date: e.target.value })}
                />
              </div>
            </label>
          </div>
          <div className="form-actions">
            {semEditId && (
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setSemEditId(null);
                  setSemForm(BLANK_SEM);
                }}
              >
                Anulează
              </button>
            )}
            {semEditId && (
              <button
                type="button"
                className="btn btn-outline"
                style={{ color: 'var(--error)', borderColor: 'var(--error)' }}
                onClick={() => deleteSem({ id: semEditId, academic_year: semForm.academic_year, semester: semForm.semester })}
              >
                <Icon name="delete" />
                Șterge
              </button>
            )}
            <button type="submit" className="btn btn-primary">
              {semEditId ? 'Salvează' : 'Adaugă semestru'}
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>An</th>
                  <th className="col-sem">Sem</th>
                  <th>Start</th>
                  <th>Final</th>
                  <th className="col-edit"></th>
                </tr>
              </thead>
              <tbody>
                {semesters.map((s) => (
                  <tr key={s.id}>
                    <td>{s.academic_year}</td>
                    <td className="col-sem">{s.semester}</td>
                    <td style={{ textTransform: 'uppercase' }}>{fmtDate(s.start_date)}</td>
                    <td style={{ textTransform: 'uppercase' }}>{fmtDate(s.end_date)}</td>
                    <td className="col-edit">
                      <button type="button" className="icon-btn icon-btn-sm" onClick={() => editSem(s)}>
                        <Icon name="edit" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </form>
      </section>

      {/* Vacations */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="beach_access" />
            Vacanțe
          </h2>
        </div>
        <form className="card-body" onSubmit={submitVac}>
          <label className="field">
            <span className="field-label">Denumire</span>
            <div className="input-wrap">
              <input
                type="text"
                value={vacForm.name}
                onChange={(e) => setVacForm({ ...vacForm, name: e.target.value })}
                placeholder="ex. Vacanța de Paște"
              />
            </div>
          </label>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">De la</span>
              <div className="input-wrap">
                <input
                  type="date"
                  style={{ textTransform: 'uppercase' }}
                  value={vacForm.start_date}
                  onChange={(e) => setVacForm({ ...vacForm, start_date: e.target.value })}
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Până la</span>
              <div className="input-wrap">
                <input
                  type="date"
                  style={{ textTransform: 'uppercase' }}
                  value={vacForm.end_date}
                  onChange={(e) => setVacForm({ ...vacForm, end_date: e.target.value })}
                />
              </div>
            </label>
          </div>
          <div className="form-actions">
            {vacEditId && (
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setVacEditId(null);
                  setVacForm(BLANK_VAC);
                }}
              >
                Anulează
              </button>
            )}
            {vacEditId && (
              <button
                type="button"
                className="btn btn-outline"
                style={{ color: 'var(--error)', borderColor: 'var(--error)' }}
                onClick={() => deleteVac({ id: vacEditId })}
              >
                <Icon name="delete" />
                Șterge
              </button>
            )}
            <button type="submit" className="btn btn-primary">
              {vacEditId ? 'Salvează' : 'Adaugă vacanță'}
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Denumire</th>
                  <th>De la</th>
                  <th>Până la</th>
                  <th className="col-edit"></th>
                </tr>
              </thead>
              <tbody>
                {vacations.map((v) => (
                  <tr key={v.id}>
                    <td>{v.name}</td>
                    <td style={{ textTransform: 'uppercase' }}>{fmtDate(v.start_date)}</td>
                    <td style={{ textTransform: 'uppercase' }}>{fmtDate(v.end_date)}</td>
                    <td className="col-edit">
                      <button type="button" className="icon-btn icon-btn-sm" onClick={() => editVac(v)}>
                        <Icon name="edit" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </form>
      </section>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
