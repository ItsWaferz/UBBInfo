import { useEffect, useState } from 'react';
import { supabase } from '../../supabaseClient';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK_SEM = { academic_year: '', semester: '1', start_date: '', end_date: '' };
const BLANK_VAC = { name: '', start_date: '', end_date: '' };

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
    const [semRes, vacRes] = await Promise.all([
      supabase.from('semester_config').select('*').order('academic_year').order('semester'),
      supabase.from('vacations').select('*').order('start_date'),
    ]);
    if (semRes.error || vacRes.error) console.error(semRes.error || vacRes.error);
    setSemesters(semRes.data || []);
    setVacations(vacRes.data || []);
    setLoading(false);
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
    const { error } = semEditId
      ? await supabase.from('semester_config').update(payload).eq('id', semEditId)
      : await supabase.from('semester_config').insert(payload);
    if (error) {
      console.error(error);
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
    const { error } = await supabase.from('semester_config').delete().eq('id', s.id);
    if (error) return flash('error', 'Eroare la ștergere.');
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
    const { error } = vacEditId
      ? await supabase.from('vacations').update(payload).eq('id', vacEditId)
      : await supabase.from('vacations').insert(payload);
    if (error) {
      console.error(error);
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
    const { error } = await supabase.from('vacations').delete().eq('id', v.id);
    if (error) return flash('error', 'Eroare la ștergere.');
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
    <div className="admin-section-grid">
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
            <button type="submit" className="btn btn-primary">
              {semEditId ? 'Salvează' : 'Adaugă semestru'}
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>An</th>
                  <th>Sem</th>
                  <th>Start</th>
                  <th>Final</th>
                  <th style={{ width: '80px' }}></th>
                </tr>
              </thead>
              <tbody>
                {semesters.map((s) => (
                  <tr key={s.id}>
                    <td>{s.academic_year}</td>
                    <td>{s.semester}</td>
                    <td>{s.start_date}</td>
                    <td>{s.end_date || '—'}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="icon-btn icon-btn-sm" onClick={() => editSem(s)}>
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-btn icon-btn-sm icon-btn-danger"
                          onClick={() => deleteSem(s)}
                        >
                          <Icon name="delete" />
                        </button>
                      </div>
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
                  <th style={{ width: '80px' }}></th>
                </tr>
              </thead>
              <tbody>
                {vacations.map((v) => (
                  <tr key={v.id}>
                    <td>{v.name}</td>
                    <td>{v.start_date}</td>
                    <td>{v.end_date}</td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="icon-btn icon-btn-sm" onClick={() => editVac(v)}>
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-btn icon-btn-sm icon-btn-danger"
                          onClick={() => deleteVac(v)}
                        >
                          <Icon name="delete" />
                        </button>
                      </div>
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
