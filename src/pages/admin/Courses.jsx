import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK = { name: '', credits: '', profile: '', category: 'obligatoriu', teaching_language: '' };

const CATEGORIES = [
  ['obligatoriu', 'Obligatoriu'],
  ['optional', 'Opțional'],
  ['facultativ', 'Facultativ'],
];
const LANGUAGES = ['Română', 'Engleză', 'Germană', 'Maghiară'];

export default function Courses() {
  const [courses, setCourses] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(BLANK);
  const [editingId, setEditingId] = useState(null); // null = create mode
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  // Accordion state
  const [openProfile, setOpenProfile] = useState(null);
  const [openCat, setOpenCat] = useState(null); // `${profile}|${category}`

  const load = async () => {
    setLoading(true);
    try {
      const [data, specs] = await Promise.all([
        api.get('/api/courses'),
        api.get('/api/courses/specializations').catch(() => []),
      ]);
      setCourses(data || []);
      setSpecializations(specs || []);
    } catch (err) {
      console.error('Load courses failed:', err);
    } finally {
      setLoading(false);
    }
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
      profile: c.profile || '',
      category: c.category || 'obligatoriu',
      teaching_language: c.teaching_language || '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
      profile: form.profile.trim() || null,
      category: form.category,
      teaching_language: form.teaching_language.trim() || null,
    };

    try {
      if (editingId) await api.put(`/api/courses/${editingId}`, payload);
      else await api.post('/api/courses', payload);
    } catch (err) {
      console.error('Save course failed:', err);
      setSaving(false);
      flashToast('error', 'Eroare la salvarea disciplinei.');
      return;
    }
    setSaving(false);
    flashToast('success', editingId ? 'Disciplină actualizată.' : 'Disciplină adăugată.');
    resetForm();
    load();
  };

  const handleDelete = async (c) => {
    if (!window.confirm(`Ștergi disciplina „${c.name}"?`)) return;
    try {
      await api.del(`/api/courses/${c.id}`);
    } catch (err) {
      console.error('Delete course failed:', err);
      flashToast('error', 'Eroare la ștergere (poate are înscrieri asociate).');
      return;
    }
    flashToast('success', 'Disciplină ștearsă.');
    if (editingId === c.id) resetForm();
    load();
  };

  // Profile options: DB specializations + any profile already on a course +
  // the one currently being edited (so it never falls out of the dropdown).
  const profileOptions = useMemo(() => {
    const set = new Set(specializations.filter(Boolean));
    for (const c of courses) if (c.profile) set.add(c.profile);
    if (form.profile) set.add(form.profile);
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [specializations, courses, form.profile]);

  // Group courses by profile (accordion L1), then by category (L2).
  const profileGroups = useMemo(() => {
    const map = new Map();
    for (const c of courses) {
      const p = c.profile || 'Fără profil';
      if (!map.has(p)) map.set(p, []);
      map.get(p).push(c);
    }
    return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [courses]);

  const toggleProfile = (p) => {
    setOpenProfile((prev) => (prev === p ? null : p));
    setOpenCat(null);
  };
  const toggleCat = (key) => setOpenCat((prev) => (prev === key ? null : key));

  return (
    <div className="admin-section-grid">
      {/* Form */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name={editingId ? 'edit' : 'add'} />
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
            <span className="field-label">Categorie</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
              >
                {CATEGORIES.map(([v, l]) => (
                  <option key={v} value={v}>{l}</option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span className="field-label">Profil</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.profile}
                onChange={(e) => setForm({ ...form, profile: e.target.value })}
              >
                <option value="">— alege —</option>
                {profileOptions.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span className="field-label">Limba de predare</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.teaching_language}
                onChange={(e) => setForm({ ...form, teaching_language: e.target.value })}
              >
                <option value="">— alege —</option>
                {LANGUAGES.map((l) => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
            </div>
          </label>
          <div className="form-actions">
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm}>
                Anulează
              </button>
            )}
            {editingId && (
              <button
                type="button"
                className="btn btn-outline"
                style={{ color: 'var(--error)', borderColor: 'var(--error)' }}
                onClick={() => handleDelete({ id: editingId, name: form.name })}
              >
                <Icon name="delete" />
                Șterge
              </button>
            )}
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? <span className="spinner" /> : editingId ? 'Salvează' : 'Adaugă'}
            </button>
          </div>
        </form>
      </section>

      {/* List — accordion: Profil → Categorie → discipline */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="menu_book" />
            Discipline ({courses.length})
          </h2>
        </div>

        {loading ? (
          <div className="card-body muted center">Se încarcă…</div>
        ) : profileGroups.length === 0 ? (
          <div className="card-body muted center">Nicio disciplină înregistrată.</div>
        ) : (
          <div className="card-body" style={{ padding: 0, gap: 0 }}>
            {profileGroups.map(([profile, list]) => (
              <div key={profile} className={`users-accordion ${openProfile === profile ? 'open' : ''}`}>
                <button
                  type="button"
                  className={`users-accordion-trigger${openProfile === profile ? ' open' : ''}`}
                  onClick={() => toggleProfile(profile)}
                >
                  <div className="users-accordion-left">
                    <Icon name="school" />
                    <span className="users-accordion-label">{profile}</span>
                    <span className="users-accordion-count">{list.length}</span>
                  </div>
                  <Icon name="expand_more" className={`users-accordion-chevron${openProfile === profile ? ' rotated' : ''}`} />
                </button>

                {openProfile === profile && (
                  <div className="users-accordion-body">
                    {CATEGORIES.map(([cat, label]) => {
                      const catCourses = list.filter((c) => (c.category || 'obligatoriu') === cat);
                      if (catCourses.length === 0) return null;
                      const key = `${profile}|${cat}`;
                      return (
                        <div key={cat} className={`users-accordion sub ${openCat === key ? 'open' : ''}`}>
                          <button
                            type="button"
                            className={`users-accordion-trigger sub${openCat === key ? ' open' : ''}`}
                            onClick={() => toggleCat(key)}
                          >
                            <div className="users-accordion-left">
                              <span className="users-accordion-label">{label}</span>
                              <span className="users-accordion-count">{catCourses.length}</span>
                            </div>
                            <Icon name="expand_more" className={`users-accordion-chevron${openCat === key ? ' rotated' : ''}`} />
                          </button>

                          {openCat === key && (
                            <div className="users-accordion-body">
                              <div className="table-wrap no-overflow">
                                <table className="data-table staff-table students-table">
                                  <thead>
                                    <tr>
                                      <th>Nume</th>
                                      <th className="col-credits">Credite</th>
                                      <th className="col-lang">Limbă</th>
                                      <th style={{ width: 44 }}></th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {catCourses.map((c) => (
                                      <tr key={c.id}>
                                        <td>{c.name}</td>
                                        <td className="col-credits">{c.credits}</td>
                                        <td className="col-lang">{c.teaching_language || '—'}</td>
                                        <td className="col-download">
                                          <button
                                            type="button"
                                            className="icon-btn icon-btn-sm"
                                            onClick={() => startEdit(c)}
                                            aria-label="Editează"
                                          >
                                            <Icon name="edit" />
                                          </button>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <Toast
        visible={!!toast}
        variant={toast?.variant || 'success'}
        message={toast?.message || ''}
      />
    </div>
  );
}
