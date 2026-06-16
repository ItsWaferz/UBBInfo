import { useEffect, useState } from 'react';
import { supabase } from '../../supabaseClient';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK = { name: '', credits: '', level: 'Licență', profile: 'Informatică' };

export default function Courses() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(BLANK);
  const [editingId, setEditingId] = useState(null); // null = create mode
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const load = async () => {
    setLoading(true);
    const { data, error } = await supabase
      .from('courses')
      .select('*')
      .order('name');
    if (error) console.error('Load courses failed:', error);
    setCourses(data || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const flashToast = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const resetForm = () => {
    setForm(BLANK);
    setEditingId(null);
  };

  const startEdit = (c) => {
    setEditingId(c.id);
    setForm({
      name: c.name || '',
      credits: c.credits ?? '',
      level: c.level || 'Licență',
      profile: c.profile || 'Informatică',
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim() || form.credits === '') {
      flashToast('error', 'Numele și creditele sunt obligatorii.');
      return;
    }
    setSaving(true);
    const payload = {
      name: form.name.trim(),
      credits: Number(form.credits),
      level: form.level.trim() || null,
      profile: form.profile.trim() || null,
    };

    const { error } = editingId
      ? await supabase.from('courses').update(payload).eq('id', editingId)
      : await supabase.from('courses').insert(payload);

    setSaving(false);
    if (error) {
      console.error('Save course failed:', error);
      flashToast('error', 'Eroare la salvarea disciplinei.');
      return;
    }
    flashToast('success', editingId ? 'Disciplină actualizată.' : 'Disciplină adăugată.');
    resetForm();
    load();
  };

  const handleDelete = async (c) => {
    if (!window.confirm(`Ștergi disciplina „${c.name}"?`)) return;
    const { error } = await supabase.from('courses').delete().eq('id', c.id);
    if (error) {
      console.error('Delete course failed:', error);
      flashToast('error', 'Eroare la ștergere (poate are înscrieri asociate).');
      return;
    }
    flashToast('success', 'Disciplină ștearsă.');
    if (editingId === c.id) resetForm();
    load();
  };

  return (
    <div className="admin-section-grid">
      {/* Form */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name={editingId ? 'edit' : 'add_circle'} />
            {editingId ? 'Editează disciplina' : 'Adaugă disciplină'}
          </h2>
        </div>
        <form className="card-body" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">Nume</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="ex. Programare web"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">Credite (ECTS)</span>
            <div className="input-wrap">
              <input
                type="number"
                min="1"
                max="30"
                value={form.credits}
                onChange={(e) => setForm({ ...form, credits: e.target.value })}
                placeholder="ex. 5"
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">Nivel</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.level}
                onChange={(e) => setForm({ ...form, level: e.target.value })}
              />
            </div>
          </label>
          <label className="field">
            <span className="field-label">Profil</span>
            <div className="input-wrap">
              <input
                type="text"
                value={form.profile}
                onChange={(e) => setForm({ ...form, profile: e.target.value })}
              />
            </div>
          </label>
          <div className="form-actions">
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm}>
                Anulează
              </button>
            )}
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? <span className="spinner" /> : editingId ? 'Salvează' : 'Adaugă'}
            </button>
          </div>
        </form>
      </section>

      {/* List */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="menu_book" />
            Discipline ({courses.length})
          </h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nume</th>
                <th>Credite</th>
                <th>Nivel</th>
                <th>Profil</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="muted center">
                    Se încarcă…
                  </td>
                </tr>
              ) : (
                courses.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.credits}</td>
                    <td>{c.level}</td>
                    <td>{c.profile}</td>
                    <td>
                      <div className="row-actions">
                        <button
                          type="button"
                          className="icon-btn"
                          onClick={() => startEdit(c)}
                          aria-label="Editează"
                        >
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-btn icon-btn-danger"
                          onClick={() => handleDelete(c)}
                          aria-label="Șterge"
                        >
                          <Icon name="delete" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <Toast
        visible={!!toast}
        variant={toast?.variant || 'success'}
        message={toast?.message || ''}
      />
    </div>
  );
}
