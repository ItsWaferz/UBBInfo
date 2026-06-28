import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Toast from '../../components/Toast';
import Icon from '../../components/Icon';
import { EVAL_CRITERIA as CRITERIA } from '../../utils/evaluationCriteria';

function professorTitle(p) {
  const bits = [p.academic_rank, p.honorifics].filter(Boolean).join(' ');
  return bits ? `${bits} ${p.full_name}` : p.full_name;
}

function Stars({ value, onChange }) {
  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={`star ${value >= n ? 'on' : ''}`}
          onClick={() => onChange(n)}
          aria-label={`${n} din 5`}
        >
          <Icon name="star" />
        </button>
      ))}
    </div>
  );
}

function EvaluationCard({ professor, courses, existingByCourse, onSaved }) {
  const [courseId, setCourseId] = useState(courses[0]?.course_id || '');
  const existing = existingByCourse[courseId];
  const [ratings, setRatings] = useState({});
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Prefill from an existing evaluation when course changes
  useEffect(() => {
    if (existing) {
      setRatings(existing.ratings || {});
      setComment(existing.comment || '');
    } else {
      setRatings({});
      setComment('');
    }
  }, [courseId, existing]);

  const setRating = (key, n) => setRatings((r) => ({ ...r, [key]: n }));

  const allRated = CRITERIA.every((c) => ratings[c.key] >= 1);

  const submit = async () => {
    if (!allRated) return;
    setSubmitting(true);
    try {
      await api.put('/api/evaluations', {
        professor_id: professor.id,
        course_id: courseId,
        ratings,
        comment: comment.trim() || null,
      });
    } catch (err) {
      console.error('Save evaluation failed:', err);
      setSubmitting(false);
      onSaved('error', 'Eroare la trimiterea evaluării.');
      return;
    }
    setSubmitting(false);
    onSaved('success', 'Evaluare trimisă. Mulțumim!');
  };

  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          {professorTitle(professor)}
        </h2>
        {existing && (
          <span className="badge status-pass">
            <Icon name="check" size={14} />
            Evaluat
          </span>
        )}
      </div>
      <div className="card-body">
        {courses.length > 1 && (
          <label className="field" style={{ maxWidth: 380 }}>
            <span className="field-label">Disciplina</span>
            <div className="input-wrap">
              <Icon name="menu_book" className="input-icon" />
              <select
                className="select-bare"
                value={courseId}
                onChange={(e) => setCourseId(e.target.value)}
              >
                {courses.map((c) => (
                  <option key={c.course_id} value={c.course_id}>
                    {c.name} · {c.type}
                  </option>
                ))}
              </select>
            </div>
          </label>
        )}
        {courses.length === 1 && (
          <p className="muted">
            Disciplina: <strong>{courses[0].name}</strong> ({courses[0].type})
          </p>
        )}

        <div className="criteria-list">
          {CRITERIA.map((c) => (
            <div className="criterion-row" key={c.key}>
              <span>{c.label}</span>
              <Stars value={ratings[c.key] || 0} onChange={(n) => setRating(c.key, n)} />
            </div>
          ))}
        </div>

        <label className="field">
          <span className="field-label">Detalii / comentarii (opțional)</span>
          <div className="input-wrap input-wrap-textarea">
            <Icon name="comment" className="input-icon" />
            <textarea
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Feedback-ul este anonim și confidențial."
            />
          </div>
        </label>

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-primary"
            onClick={submit}
            disabled={submitting || !allRated}
          >
            {submitting ? (
              <span className="spinner" />
            ) : existing ? (
              'Actualizează evaluarea'
            ) : (
              'Trimite evaluarea'
            )}
          </button>
        </div>
      </div>
    </section>
  );
}

export default function Evaluare() {
  const { user, currentRole } = useAuth();
  const [professors, setProfessors] = useState([]); // { id, full_name, academic_rank, honorifics, _studentId, courses:[] }
  const [existing, setExisting] = useState({}); // `${profId}:${courseId}` -> row
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      // Backend aggregates: professors teaching the student's courses + existing evals.
      const data = await api.get('/api/evaluations/targets');

      const list = (data.professors || [])
        .map((p) => ({ ...p, _studentId: user.id }))
        .sort((a, b) => (a.full_name || '').localeCompare(b.full_name || ''));

      const existingMap = {};
      for (const ev of data.existing || [])
        existingMap[`${ev.professor_id}:${ev.course_id}`] = ev;

      setProfessors(list);
      setExisting(existingMap);
    } catch (err) {
      console.error('Load evaluations failed:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const existingByCourseFor = useMemo(
    () => (profId) => {
      const out = {};
      for (const [k, v] of Object.entries(existing)) {
        const [pid, cid] = k.split(':');
        if (pid === profId) out[cid] = v;
      }
      return out;
    },
    [existing]
  );

  if (currentRole && currentRole !== 'student') return <Navigate to="/" replace />;

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
    load(); // refresh "evaluat" badges
  };

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Evaluare Profesori</h1>
        <p className="page-subtitle">
          Evaluează cadrele didactice care îți predau. Notează de la 1 la 5 pe fiecare
          criteriu. Feedback-ul este anonim.
        </p>
      </section>

      {loading ? (
        <section className="card">
          <div className="card-body muted center">Se încarcă…</div>
        </section>
      ) : professors.length === 0 ? (
        <section className="card">
          <div className="card-body muted center">
            Nu există profesori de evaluat momentan.
          </div>
        </section>
      ) : (
        professors.map((p) => (
          <EvaluationCard
            key={p.id}
            professor={p}
            courses={p.courses}
            existingByCourse={existingByCourseFor(p.id)}
            onSaved={flash}
          />
        ))
      )}

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
