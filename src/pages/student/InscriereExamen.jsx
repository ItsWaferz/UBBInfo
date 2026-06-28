import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import { formatRoom } from '../../utils/rooms';
import { parseDate } from '../../utils/calendar';
import Toast from '../../components/Toast';
import Icon from '../../components/Icon';

const hhmm = (t) => (t ? t.slice(0, 5) : '');
function fmtDate(d) {
  if (!d) return '—';
  return new Intl.DateTimeFormat('ro-RO', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(parseDate(d));
}

function ExamSlot({ exam, selected, disabled, onSelect }) {
  return (
    <button
      type="button"
      className={`exam-slot ${selected ? 'selected' : ''}`}
      onClick={onSelect}
      disabled={disabled}
    >
      <span className="exam-slot-radio">
        <Icon name={selected ? 'radio_button_checked' : 'radio_button_unchecked'} />
      </span>
      <span className="exam-slot-body">
        <span className="exam-slot-date">{fmtDate(exam.exam_date)}</span>
        <span className="exam-slot-meta">
          {hhmm(exam.exam_time)} · {formatRoom(exam.rooms, exam.room)}
        </span>
      </span>
    </button>
  );
}

export default function InscriereExamen() {
  const { user, currentRole } = useAuth();
  const [courses, setCourses] = useState([]); // { course_id, name, principal, secundar, restanta }
  const [registrations, setRegistrations] = useState({}); // course_id -> exam_id
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(null); // course_id being saved
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const load = async () => {
    setLoading(true);
    try {
      const [exams, regs] = await Promise.all([
        api.get('/api/exams/mine'),
        api.get('/api/exam-registrations/me'),
      ]);

      const byCourse = new Map();
      for (const ex of exams || []) {
        if (!byCourse.has(ex.course_id)) {
          byCourse.set(ex.course_id, {
            course_id: ex.course_id,
            name: ex.courses?.name || '—',
            principal: null,
            secundar: null,
            restanta: null,
          });
        }
        const entry = byCourse.get(ex.course_id);
        if (ex.kind === 'principal') entry.principal = ex;
        else if (ex.kind === 'secundar') entry.secundar = ex;
        else entry.restanta = ex;
      }

      const regMap = {};
      for (const r of regs || []) regMap[r.course_id] = r.exam_id;

      setCourses([...byCourse.values()].sort((a, b) => a.name.localeCompare(b.name)));
      setRegistrations(regMap);
    } catch (err) {
      console.error('Load exams failed:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const register = async (courseId, exam) => {
    setBusy(courseId);
    try {
      await api.put('/api/exam-registrations', {
        course_id: courseId,
        exam_id: exam.id,
      });
    } catch (err) {
      console.error('Register exam failed:', err);
      setBusy(null);
      flash('error', 'Eroare la înscriere.');
      return;
    }
    setBusy(null);
    setRegistrations((r) => ({ ...r, [courseId]: exam.id }));
    flash('success', 'Înscriere salvată.');
  };

  const cancel = async (courseId) => {
    setBusy(courseId);
    try {
      await api.del(`/api/exam-registrations?courseId=${encodeURIComponent(courseId)}`);
    } catch (err) {
      console.error('Cancel registration failed:', err);
      setBusy(null);
      flash('error', 'Eroare la anulare.');
      return;
    }
    setBusy(null);
    setRegistrations((r) => {
      const next = { ...r };
      delete next[courseId];
      return next;
    });
    flash('success', 'Înscriere anulată.');
  };

  if (currentRole && currentRole !== 'student') return <Navigate to="/" replace />;

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Înscriere Examen</h1>
        <p className="page-subtitle">
          Pentru fiecare disciplină alege data principală sau secundară. Data de
          restanță/mărire este afișată informativ.
        </p>
      </section>

      {loading ? (
        <section className="card">
          <div className="card-body muted center">Se încarcă examenele…</div>
        </section>
      ) : courses.length === 0 ? (
        <section className="card">
          <div className="card-body muted center">
            Nu există examene programate pentru disciplinele tale.
          </div>
        </section>
      ) : (
        courses.map((c) => {
          const selectedExamId = registrations[c.course_id];
          const isBusy = busy === c.course_id;
          return (
            <section className="card" key={c.course_id}>
              <div className="card-header">
                <h2 className="card-title">
                  <Icon name="event_available" />
                  {c.name}
                </h2>
                {selectedExamId ? (
                  <span className="badge status-pass">Înscris</span>
                ) : (
                  <span className="badge badge-course">Neînscris</span>
                )}
              </div>
              <div className="card-body">
                <div className="exam-slots">
                  {c.principal && (
                    <div className="exam-option">
                      <div className="exam-option-label">Data principală</div>
                      <ExamSlot
                        exam={c.principal}
                        selected={selectedExamId === c.principal.id}
                        disabled={isBusy}
                        onSelect={() => register(c.course_id, c.principal)}
                      />
                    </div>
                  )}
                  {c.secundar && (
                    <div className="exam-option">
                      <div className="exam-option-label">Data secundară</div>
                      <ExamSlot
                        exam={c.secundar}
                        selected={selectedExamId === c.secundar.id}
                        disabled={isBusy}
                        onSelect={() => register(c.course_id, c.secundar)}
                      />
                    </div>
                  )}
                  {c.restanta && (
                    <div className="exam-option">
                      <div className="exam-option-label">Restanță / mărire</div>
                      <div className="exam-slot exam-slot-info">
                        <span className="exam-slot-radio">
                          <Icon name="info" />
                        </span>
                        <span className="exam-slot-body">
                          <span className="exam-slot-date">{fmtDate(c.restanta.exam_date)}</span>
                          <span className="exam-slot-meta">
                            {hhmm(c.restanta.exam_time)} ·{' '}
                            {formatRoom(c.restanta.rooms, c.restanta.room)}
                          </span>
                        </span>
                      </div>
                    </div>
                  )}
                </div>

                {selectedExamId && (
                  <div className="form-actions">
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => cancel(c.course_id)}
                      disabled={isBusy}
                    >
                      <Icon name="close" />
                      Anulează înscrierea
                    </button>
                  </div>
                )}
              </div>
            </section>
          );
        })
      )}

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
