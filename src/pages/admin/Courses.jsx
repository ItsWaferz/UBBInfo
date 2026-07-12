import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import Icon from '../../components/Icon';
import Toast from '../../components/Toast';

const BLANK = { name: '', credits: '', profile: '', category: 'obligatoriu', teaching_language: '', study_year: '', semester: '' };

const SEMESTERS = ['1', '2'];

const CATEGORIES = [
  ['obligatoriu', 'Obligatoriu'],
  ['optional', 'Opțional'],
  ['facultativ', 'Facultativ'],
];
const LANGUAGES = ['Română', 'Engleză', 'Germană', 'Maghiară'];

/** Diacritic-insensitive key, so ș/ş/ț/ţ variants still match the reference table. */
const norm = (s) => (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

export default function Courses() {
  const [courses, setCourses] = useState([]);
  const [specs, setSpecs] = useState([]); // [{code,name,language,faculty,duration_years}]
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(BLANK);
  const [editingId, setEditingId] = useState(null); // null = create mode
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  // Accordion state: L1 program, L2 …|language, L3 …|year, L4 …|semester, L5 …|category
  const [openProgram, setOpenProgram] = useState(null);
  const [openLang, setOpenLang] = useState(null);
  const [openYear, setOpenYear] = useState(null);
  const [openSem, setOpenSem] = useState(null);
  const [openCat, setOpenCat] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const [data, meta] = await Promise.all([
        api.get('/api/courses'),
        api.get('/api/specializations').catch(() => []),
      ]);
      setCourses(data || []);
      setSpecs(meta || []);
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
      study_year: c.study_year != null ? String(c.study_year) : '',
      semester: c.semester != null ? String(c.semester) : '',
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
      study_year: form.study_year ? Number(form.study_year) : null,
      semester: form.semester ? Number(form.semester) : null,
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

  // Program (profile) options for the form: reference programs + any already on a
  // course + the one being edited (so it never falls out of the dropdown).
  const profileOptions = useMemo(() => {
    const set = new Set(specs.map((s) => s.name).filter(Boolean));
    for (const c of courses) if (c.profile) set.add(c.profile);
    if (form.profile) set.add(form.profile);
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [specs, courses, form.profile]);

  // Per-program metadata from the specializations table: its languages and how
  // many years it runs (max duration across its language variants).
  const programMeta = useMemo(() => {
    const map = new Map(); // name -> { languages:Set, duration:int }
    for (const s of specs) {
      if (!s.name) continue;
      const e = map.get(s.name) || { languages: new Set(), duration: 3 };
      if (s.language) e.languages.add(s.language);
      e.duration = Math.max(e.duration, s.duration_years || 3);
      map.set(s.name, e);
    }
    return map;
  }, [specs]);

  // Diacritic-insensitive lookup by program name, for matching courses to the tree.
  const programByNorm = useMemo(() => {
    const m = new Map();
    for (const [name, e] of programMeta) {
      m.set(norm(name), {
        name,
        duration: e.duration,
        languages: Array.from(e.languages).sort((a, b) => a.localeCompare(b)),
        languagesNorm: new Set(Array.from(e.languages).map(norm)),
      });
    }
    return m;
  }, [programMeta]);

  // Menu tree: program -> { languages, years[1..duration] }. Every combo shows
  // up, even with no courses yet.
  const programTree = useMemo(() => (
    Array.from(programMeta.entries())
      .map(([name, e]) => [name, {
        languages: Array.from(e.languages).sort((a, b) => a.localeCompare(b)),
        years: Array.from({ length: e.duration }, (_, i) => i + 1),
      }])
      .sort(([a], [b]) => a.localeCompare(b))
  ), [programMeta]);

  // Courses that don't fit any reference program+language+year+semester (incl.
  // year/semester-less ones) stay visible + editable under "Neîncadrate".
  const orphanCourses = useMemo(() => (
    courses.filter((c) => {
      const p = programByNorm.get(norm(c.profile));
      if (!p || !p.languagesNorm.has(norm(c.teaching_language))) return true;
      const y = Number(c.study_year);
      const s = Number(c.semester);
      return !(Number.isInteger(y) && y >= 1 && y <= p.duration && (s === 1 || s === 2));
    })
  ), [courses, programByNorm]);

  const coursesFor = (program, language, year, sem, cat) =>
    courses.filter(
      (c) => norm(c.profile) === norm(program)
        && norm(c.teaching_language) === norm(language)
        && Number(c.study_year) === year
        && Number(c.semester) === sem
        && (c.category || 'obligatoriu') === cat
    );
  const countProgram = (program) =>
    courses.filter((c) => norm(c.profile) === norm(program)).length;
  const countLang = (program, language) =>
    courses.filter(
      (c) => norm(c.profile) === norm(program) && norm(c.teaching_language) === norm(language)
    ).length;
  const countYear = (program, language, year) =>
    courses.filter(
      (c) => norm(c.profile) === norm(program)
        && norm(c.teaching_language) === norm(language)
        && Number(c.study_year) === year
    ).length;
  const countSem = (program, language, year, sem) =>
    courses.filter(
      (c) => norm(c.profile) === norm(program)
        && norm(c.teaching_language) === norm(language)
        && Number(c.study_year) === year
        && Number(c.semester) === sem
    ).length;

  // Form: choosing a profile filters the Limbă + An options to that program.
  const selectedProgram = programByNorm.get(norm(form.profile));
  const formLangOptions = selectedProgram ? selectedProgram.languages : LANGUAGES;
  const formYearOptions = selectedProgram
    ? Array.from({ length: selectedProgram.duration }, (_, i) => String(i + 1))
    : ['1', '2', '3', '4'];

  const onProfileChange = (e) => {
    const profile = e.target.value;
    const p = programByNorm.get(norm(profile));
    setForm((f) => ({
      ...f,
      profile,
      teaching_language: !p || p.languagesNorm.has(norm(f.teaching_language)) ? f.teaching_language : '',
      study_year: !p || (f.study_year && Number(f.study_year) <= p.duration) ? f.study_year : '',
    }));
  };

  const toggleProgram = (p) => {
    setOpenProgram((prev) => (prev === p ? null : p));
    setOpenLang(null);
    setOpenYear(null);
    setOpenSem(null);
    setOpenCat(null);
  };
  const toggleLang = (key) => {
    setOpenLang((prev) => (prev === key ? null : key));
    setOpenYear(null);
    setOpenSem(null);
    setOpenCat(null);
  };
  const toggleYear = (key) => {
    setOpenYear((prev) => (prev === key ? null : key));
    setOpenSem(null);
    setOpenCat(null);
  };
  const toggleSem = (key) => {
    setOpenSem((prev) => (prev === key ? null : key));
    setOpenCat(null);
  };
  const toggleCat = (key) => setOpenCat((prev) => (prev === key ? null : key));

  const courseTable = (list) => (
    <div className="table-wrap no-overflow">
      <table className="data-table staff-table students-table">
        <thead>
          <tr>
            <th>Nume</th>
            <th className="col-credits">Credite</th>
            <th style={{ width: 44 }}></th>
          </tr>
        </thead>
        <tbody>
          {list.map((c) => (
            <tr key={c.id}>
              <td>{c.name}</td>
              <td className="col-credits">{c.credits}</td>
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
  );

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
                onChange={onProfileChange}
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
                {formLangOptions.map((l) => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span className="field-label">Anul</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.study_year}
                onChange={(e) => setForm({ ...form, study_year: e.target.value })}
              >
                <option value="">— alege —</option>
                {formYearOptions.map((y) => (
                  <option key={y} value={y}>{`Anul ${y}`}</option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span className="field-label">Semestrul</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.semester}
                onChange={(e) => setForm({ ...form, semester: e.target.value })}
              >
                <option value="">— alege —</option>
                {SEMESTERS.map((s) => (
                  <option key={s} value={s}>{`Semestrul ${s}`}</option>
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

      {/* List — accordion: Specializare → Limbă → An → Semestru → Categorie → discipline */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="menu_book" />
            Discipline ({courses.length})
          </h2>
        </div>

        {loading ? (
          <div className="card-body muted center">Se încarcă…</div>
        ) : programTree.length === 0 ? (
          <div className="card-body muted center">Nicio specializare configurată.</div>
        ) : (
          <div className="card-body" style={{ padding: 0, gap: 0 }}>
            {programTree.map(([program, { languages, years }]) => (
              <div key={program} className={`users-accordion ${openProgram === program ? 'open' : ''}`}>
                <button
                  type="button"
                  className={`users-accordion-trigger${openProgram === program ? ' open' : ''}`}
                  onClick={() => toggleProgram(program)}
                >
                  <div className="users-accordion-left">
                    <Icon name="school" />
                    <span className="users-accordion-label">{program}</span>
                    <span className="users-accordion-count">{countProgram(program)}</span>
                  </div>
                  <Icon name="expand_more" className={`users-accordion-chevron${openProgram === program ? ' rotated' : ''}`} />
                </button>

                {openProgram === program && (
                  <div className="users-accordion-body">
                    {languages.map((lang) => {
                      const langKey = `${program}|${lang}`;
                      return (
                        <div key={lang} className={`users-accordion sub ${openLang === langKey ? 'open' : ''}`}>
                          <button
                            type="button"
                            className={`users-accordion-trigger sub${openLang === langKey ? ' open' : ''}`}
                            onClick={() => toggleLang(langKey)}
                          >
                            <div className="users-accordion-left">
                              <span className="users-accordion-label">{lang}</span>
                              <span className="users-accordion-count">{countLang(program, lang)}</span>
                            </div>
                            <Icon name="expand_more" className={`users-accordion-chevron${openLang === langKey ? ' rotated' : ''}`} />
                          </button>

                          {openLang === langKey && (
                            <div className="users-accordion-body">
                              {years.map((year) => {
                                const yearKey = `${program}|${lang}|${year}`;
                                return (
                                  <div key={year} className={`users-accordion sub ${openYear === yearKey ? 'open' : ''}`}>
                                    <button
                                      type="button"
                                      className={`users-accordion-trigger sub${openYear === yearKey ? ' open' : ''}`}
                                      onClick={() => toggleYear(yearKey)}
                                    >
                                      <div className="users-accordion-left">
                                        <span className="users-accordion-label">{`Anul ${year}`}</span>
                                        <span className="users-accordion-count">{countYear(program, lang, year)}</span>
                                      </div>
                                      <Icon name="expand_more" className={`users-accordion-chevron${openYear === yearKey ? ' rotated' : ''}`} />
                                    </button>

                                    {openYear === yearKey && (
                                      <div className="users-accordion-body">
                                        {SEMESTERS.map((sem) => {
                                          const semNum = Number(sem);
                                          const semKey = `${yearKey}|${sem}`;
                                          return (
                                            <div key={sem} className={`users-accordion sub ${openSem === semKey ? 'open' : ''}`}>
                                              <button
                                                type="button"
                                                className={`users-accordion-trigger sub${openSem === semKey ? ' open' : ''}`}
                                                onClick={() => toggleSem(semKey)}
                                              >
                                                <div className="users-accordion-left">
                                                  <span className="users-accordion-label">{`Semestrul ${sem}`}</span>
                                                  <span className="users-accordion-count">{countSem(program, lang, year, semNum)}</span>
                                                </div>
                                                <Icon name="expand_more" className={`users-accordion-chevron${openSem === semKey ? ' rotated' : ''}`} />
                                              </button>

                                              {openSem === semKey && (
                                                <div className="users-accordion-body">
                                                  {CATEGORIES.map(([cat, label]) => {
                                                    const catCourses = coursesFor(program, lang, year, semNum, cat);
                                                    const catKey = `${semKey}|${cat}`;
                                                    return (
                                                      <div key={cat} className={`users-accordion sub ${openCat === catKey ? 'open' : ''}`}>
                                                        <button
                                                          type="button"
                                                          className={`users-accordion-trigger sub${openCat === catKey ? ' open' : ''}`}
                                                          onClick={() => toggleCat(catKey)}
                                                        >
                                                          <div className="users-accordion-left">
                                                            <span className="users-accordion-label">{label}</span>
                                                            <span className="users-accordion-count">{catCourses.length}</span>
                                                          </div>
                                                          <Icon name="expand_more" className={`users-accordion-chevron${openCat === catKey ? ' rotated' : ''}`} />
                                                        </button>

                                                        {openCat === catKey && (
                                                          <div className="users-accordion-body">
                                                            {catCourses.length === 0 ? (
                                                              <div className="card-body muted center" style={{ padding: '12px' }}>
                                                                Nicio disciplină.
                                                              </div>
                                                            ) : (
                                                              courseTable(catCourses)
                                                            )}
                                                          </div>
                                                        )}
                                                      </div>
                                                    );
                                                  })}
                                                </div>
                                              )}
                                            </div>
                                          );
                                        })}
                                      </div>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}

            {/* Safety net: courses not matching any reference program+language. */}
            {orphanCourses.length > 0 && (
              <div className={`users-accordion ${openProgram === '__orphans__' ? 'open' : ''}`}>
                <button
                  type="button"
                  className={`users-accordion-trigger${openProgram === '__orphans__' ? ' open' : ''}`}
                  onClick={() => toggleProgram('__orphans__')}
                >
                  <div className="users-accordion-left">
                    <Icon name="school" />
                    <span className="users-accordion-label">Neîncadrate</span>
                    <span className="users-accordion-count">{orphanCourses.length}</span>
                  </div>
                  <Icon name="expand_more" className={`users-accordion-chevron${openProgram === '__orphans__' ? ' rotated' : ''}`} />
                </button>
                {openProgram === '__orphans__' && (
                  <div className="users-accordion-body">{courseTable(orphanCourses)}</div>
                )}
              </div>
            )}
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
