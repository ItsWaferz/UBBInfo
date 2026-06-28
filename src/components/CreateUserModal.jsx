import { useEffect, useState } from 'react';
import { supabase } from '../supabaseClient'; // kept: user creation is an auth op (create-user Edge Function)
import { api } from '../api';
import Icon from './Icon';

const ACADEMIC_RANKS = [
  'Doctorand',
  'Asistent',
  'Lector',
  'Conferențiar',
  'Profesor',
];

const BLANK = {
  email: '',
  password: '',
  full_name: '',
  role_name: 'student',
  student_id: '',
  faculty: '',
  specialization: '',
  study_year: '',
  group_name: '',
  financing: 'BUGET',
  academic_rank: 'Lector',
  honorifics: '',
};

export default function CreateUserModal({ open, onClose, onCreated }) {
  const [form, setForm] = useState(BLANK);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [courses, setCourses] = useState([]);
  const [selectedCourses, setSelectedCourses] = useState([]);

  // Load course list once the modal opens (for professor course assignment)
  useEffect(() => {
    if (!open) return;
    let active = true;
    api
      .get('/api/courses')
      .then((data) => {
        if (active) setCourses(data || []);
      })
      .catch((err) => console.error('Load courses failed:', err));
    return () => {
      active = false;
    };
  }, [open]);

  if (!open) return null;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const close = () => {
    setForm(BLANK);
    setSelectedCourses([]);
    setError('');
    setSubmitting(false);
    onClose();
  };

  const isStudent = form.role_name === 'student';
  const isProfessor = form.role_name === 'profesor';

  const toggleCourse = (id) =>
    setSelectedCourses((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.email.trim() || !form.password || !form.full_name.trim()) {
      setError('Email, parolă și nume sunt obligatorii.');
      return;
    }
    if (form.password.length < 6) {
      setError('Parola trebuie să aibă cel puțin 6 caractere.');
      return;
    }

    const profile = {};
    if (isStudent) {
      for (const k of ['student_id', 'faculty', 'specialization', 'study_year', 'group_name', 'financing']) {
        if (form[k]?.trim?.()) profile[k] = form[k].trim();
      }
    }
    if (isProfessor) {
      profile.faculty = 'Matematică și Informatică';
      if (form.academic_rank?.trim()) profile.academic_rank = form.academic_rank.trim();
      if (form.honorifics?.trim()) profile.honorifics = form.honorifics.trim();
    }

    setSubmitting(true);
    try {
      const { data, error: fnError } = await supabase.functions.invoke('create-user', {
        body: {
          email: form.email.trim(),
          password: form.password,
          full_name: form.full_name.trim(),
          role_name: form.role_name,
          profile,
          courses: isProfessor ? selectedCourses : [],
        },
      });

      if (fnError) {
        // The Edge Function returns a JSON body with { error } on non-2xx
        let msg = fnError.message;
        try {
          const ctx = await fnError.context?.json?.();
          if (ctx?.error) msg = ctx.error;
        } catch {
          /* ignore parse errors */
        }
        throw new Error(msg);
      }
      if (data && data.ok === false && data.error) {
        throw new Error(data.error);
      }

      const created = { ...form };
      close();
      onCreated?.(created);
    } catch (err) {
      console.error('Create user failed:', err);
      setError(err.message || 'Nu am putut crea contul.');
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={close}>
      <div className="modal-card modal-card-lg" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Adaugă utilizator</h3>
          <button type="button" className="modal-close" onClick={close} aria-label="Închide">
            <Icon name="close" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Nume complet *</span>
              <div className="input-wrap">
                <Icon name="person" className="input-icon" />
                <input type="text" value={form.full_name} onChange={set('full_name')} placeholder="Nume Prenume" />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Rol *</span>
              <div className="input-wrap">
                <Icon name="badge" className="input-icon" />
                <select className="select-bare" value={form.role_name} onChange={set('role_name')}>
                  <option value="student">Student</option>
                  <option value="profesor">Profesor</option>
                  <option value="administrator">Administrator</option>
                </select>
              </div>
            </label>
          </div>

          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Email instituțional *</span>
              <div className="input-wrap">
                <Icon name="mail" className="input-icon" />
                <input type="email" autoComplete="off" value={form.email} onChange={set('email')} placeholder="nume@ubbcluj.ro" />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Parolă inițială *</span>
              <div className="input-wrap">
                <Icon name="lock" className="input-icon" />
                <input type="text" autoComplete="off" value={form.password} onChange={set('password')} placeholder="Minim 6 caractere" />
              </div>
            </label>
          </div>

          {isStudent && (
            <>
              <div className="form-grid-2">
                <label className="field">
                  <span className="field-label">Nr. matricol</span>
                  <div className="input-wrap">
                    <input type="text" className="mono" value={form.student_id} onChange={set('student_id')} placeholder="ex. CSIM1234567" />
                  </div>
                </label>
                <label className="field">
                  <span className="field-label">An studiu</span>
                  <div className="input-wrap">
                    <input type="text" value={form.study_year} onChange={set('study_year')} placeholder="ex. II" />
                  </div>
                </label>
              </div>
              <div className="form-grid-2">
                <label className="field">
                  <span className="field-label">Facultate</span>
                  <div className="input-wrap">
                    <input type="text" value={form.faculty} onChange={set('faculty')} placeholder="ex. Matematică și Informatică" />
                  </div>
                </label>
                <label className="field">
                  <span className="field-label">Specializare</span>
                  <div className="input-wrap">
                    <input type="text" value={form.specialization} onChange={set('specialization')} placeholder="ex. Informatică" />
                  </div>
                </label>
              </div>
              <div className="form-grid-2">
                <label className="field">
                  <span className="field-label">Grupa</span>
                  <div className="input-wrap">
                    <input type="text" value={form.group_name} onChange={set('group_name')} placeholder="ex. 221/1" />
                  </div>
                </label>
                <label className="field">
                  <span className="field-label">Finanțare</span>
                  <div className="input-wrap">
                    <select className="select-bare" value={form.financing} onChange={set('financing')}>
                      <option value="BUGET">BUGET</option>
                      <option value="TAXĂ">TAXĂ</option>
                    </select>
                  </div>
                </label>
              </div>
            </>
          )}

          {isProfessor && (
            <>
              <div className="form-grid-2">
                <label className="field">
                  <span className="field-label">Grad didactic</span>
                  <div className="input-wrap">
                    <Icon name="workspace_premium" className="input-icon" />
                    <select className="select-bare" value={form.academic_rank} onChange={set('academic_rank')}>
                      {ACADEMIC_RANKS.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  </div>
                </label>
                <label className="field">
                  <span className="field-label">Titluri onorifice</span>
                  <div className="input-wrap">
                    <Icon name="military_tech" className="input-icon" />
                    <input
                      type="text"
                      value={form.honorifics}
                      onChange={set('honorifics')}
                      placeholder="ex. dr. habil."
                    />
                  </div>
                </label>
              </div>

              <div className="field">
                <span className="field-label">
                  Discipline predate ({selectedCourses.length} selectate)
                </span>
                <div className="course-multiselect">
                  {courses.map((c) => (
                    <label key={c.id} className="course-check">
                      <input
                        type="checkbox"
                        checked={selectedCourses.includes(c.id)}
                        onChange={() => toggleCourse(c.id)}
                      />
                      <span>{c.name}</span>
                    </label>
                  ))}
                  {courses.length === 0 && <span className="muted">Se încarcă disciplinele…</span>}
                </div>
              </div>
            </>
          )}

          {error && <p className="modal-error">{error}</p>}

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={close}>
              Anulează
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <span className="spinner" /> : 'Creează cont'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
