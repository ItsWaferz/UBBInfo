import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { supabase } from '../../supabaseClient';
import { useAuth } from '../../contexts/AuthContext';
import { formatRoom } from '../../utils/rooms';
import { parseDate } from '../../utils/calendar';
import Icon from '../../components/Icon';
import RoomPicker from '../../components/RoomPicker';
import Toast from '../../components/Toast';

const KINDS = {
  principal: 'Principal',
  secundar: 'Secundar',
  restanta_marire: 'Restanță / mărire',
};
const SESSIONS = { vara: 'Vară', iarna: 'Iarnă', restante: 'Restanțe' };

const hhmm = (t) => (t ? t.slice(0, 5) : '');
const fmtDate = (d) =>
  d
    ? new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' }).format(
        parseDate(d)
      )
    : '—';

const BLANK = {
  course_id: '',
  kind: 'principal',
  session_type: 'vara',
  exam_date: '',
  exam_time: '09:00',
  room_id: '',
  enrolled_count: '',
};

export default function Examene() {
  const { user, currentRole } = useAuth();
  const [courses, setCourses] = useState([]); // professor_courses
  const [exams, setExams] = useState([]);
  const [form, setForm] = useState(BLANK);
  const [editId, setEditId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const load = async () => {
    setLoading(true);
    const [pcRes, exRes] = await Promise.all([
      supabase
        .from('professor_courses')
        .select('course_id, courses(name)')
        .eq('professor_id', user.id),
      supabase
        .from('exams')
        .select('*, rooms(code, note, buildings(name)), courses(name)')
        .eq('professor_id', user.id)
        .order('exam_date'),
    ]);
    const cs = pcRes.data || [];
    setCourses(cs);
    setExams(exRes.data || []);
    setForm((f) => ({ ...f, course_id: f.course_id || cs[0]?.course_id || '' }));
    setLoading(false);
  };

  useEffect(() => {
    if (user) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const resetForm = () =>
    setForm({ ...BLANK, course_id: courses[0]?.course_id || '' }) || setEditId(null);

  const startEdit = (ex) => {
    setEditId(ex.id);
    setForm({
      course_id: ex.course_id,
      kind: ex.kind || 'principal',
      session_type: ex.session_type || 'vara',
      exam_date: ex.exam_date || '',
      exam_time: hhmm(ex.exam_time),
      room_id: ex.room_id || '',
      enrolled_count: ex.enrolled_count ?? '',
    });
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!form.course_id || !form.exam_date) {
      flash('error', 'Disciplina și data sunt obligatorii.');
      return;
    }
    const payload = {
      course_id: form.course_id,
      professor_id: user.id,
      kind: form.kind,
      session_type: form.session_type,
      exam_date: form.exam_date,
      exam_time: form.exam_time || null,
      room_id: form.room_id || null,
      room: null,
      enrolled_count: form.enrolled_count === '' ? null : Number(form.enrolled_count),
    };
    const { error } = editId
      ? await supabase.from('exams').update(payload).eq('id', editId)
      : await supabase.from('exams').insert(payload);
    if (error) {
      console.error('Save exam failed:', error);
      flash('error', 'Eroare la salvare.');
      return;
    }
    flash('success', editId ? 'Examen actualizat.' : 'Examen adăugat.');
    setEditId(null);
    setForm({ ...BLANK, course_id: form.course_id });
    load();
  };

  const remove = async (ex) => {
    if (!window.confirm('Ștergi acest examen?')) return;
    const { error } = await supabase.from('exams').delete().eq('id', ex.id);
    if (error) return flash('error', 'Eroare la ștergere.');
    flash('success', 'Șters.');
    if (editId === ex.id) {
      setEditId(null);
      setForm({ ...BLANK, course_id: courses[0]?.course_id || '' });
    }
    load();
  };

  if (currentRole && currentRole !== 'profesor') return <Navigate to="/" replace />;

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Examene</h1>
        <p className="page-subtitle">
          Programează și gestionează examenele pentru disciplinele tale.
        </p>
      </section>

      <div className="admin-section-grid">
        {/* Form */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name={editId ? 'edit' : 'add_circle'} />
              {editId ? 'Editează examenul' : 'Adaugă examen'}
            </h2>
          </div>
          <form className="card-body" onSubmit={submit}>
            <label className="field">
              <span className="field-label">Disciplina</span>
              <div className="input-wrap">
                <Icon name="menu_book" className="input-icon" />
                <select
                  className="select-bare"
                  value={form.course_id}
                  onChange={(e) => setForm({ ...form, course_id: e.target.value })}
                >
                  {courses.map((c) => (
                    <option key={c.course_id} value={c.course_id}>
                      {c.courses?.name}
                    </option>
                  ))}
                </select>
              </div>
            </label>

            <div className="form-grid-2">
              <label className="field">
                <span className="field-label">Tip examen</span>
                <div className="input-wrap">
                  <select
                    className="select-bare"
                    value={form.kind}
                    onChange={(e) => setForm({ ...form, kind: e.target.value })}
                  >
                    {Object.entries(KINDS).map(([v, l]) => (
                      <option key={v} value={v}>
                        {l}
                      </option>
                    ))}
                  </select>
                </div>
              </label>
              <label className="field">
                <span className="field-label">Sesiune</span>
                <div className="input-wrap">
                  <select
                    className="select-bare"
                    value={form.session_type}
                    onChange={(e) => setForm({ ...form, session_type: e.target.value })}
                  >
                    {Object.entries(SESSIONS).map(([v, l]) => (
                      <option key={v} value={v}>
                        {l}
                      </option>
                    ))}
                  </select>
                </div>
              </label>
            </div>

            <div className="form-grid-2">
              <label className="field">
                <span className="field-label">Data</span>
                <div className="input-wrap">
                  <input
                    type="date"
                    value={form.exam_date}
                    onChange={(e) => setForm({ ...form, exam_date: e.target.value })}
                  />
                </div>
              </label>
              <label className="field">
                <span className="field-label">Ora</span>
                <div className="input-wrap">
                  <input
                    type="time"
                    value={form.exam_time}
                    onChange={(e) => setForm({ ...form, exam_time: e.target.value })}
                  />
                </div>
              </label>
            </div>

            <RoomPicker value={form.room_id} onChange={(rid) => setForm({ ...form, room_id: rid })} />

            <label className="field">
              <span className="field-label">Studenți înscriși (estimativ)</span>
              <div className="input-wrap">
                <Icon name="group" className="input-icon" />
                <input
                  type="number"
                  min="0"
                  value={form.enrolled_count}
                  onChange={(e) => setForm({ ...form, enrolled_count: e.target.value })}
                  placeholder="ex. 28"
                />
              </div>
            </label>

            <div className="form-actions">
              {editId && (
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => {
                    setEditId(null);
                    setForm({ ...BLANK, course_id: courses[0]?.course_id || '' });
                  }}
                >
                  Anulează
                </button>
              )}
              <button type="submit" className="btn btn-primary">
                {editId ? 'Salvează' : 'Adaugă examen'}
              </button>
            </div>
          </form>
        </section>

        {/* List */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="event_available" />
              Examene programate ({exams.length})
            </h2>
          </div>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Disciplina</th>
                  <th>Tip</th>
                  <th>Data</th>
                  <th>Ora</th>
                  <th>Sala</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} className="muted center">
                      Se încarcă…
                    </td>
                  </tr>
                ) : exams.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted center">
                      Niciun examen programat.
                    </td>
                  </tr>
                ) : (
                  exams.map((ex) => (
                    <tr key={ex.id}>
                      <td>{ex.courses?.name}</td>
                      <td>{KINDS[ex.kind] || ex.kind}</td>
                      <td>{fmtDate(ex.exam_date)}</td>
                      <td>{hhmm(ex.exam_time)}</td>
                      <td>{formatRoom(ex.rooms, ex.room)}</td>
                      <td>
                        <div className="row-actions">
                          <button type="button" className="icon-btn" onClick={() => startEdit(ex)}>
                            <Icon name="edit" />
                          </button>
                          <button
                            type="button"
                            className="icon-btn icon-btn-danger"
                            onClick={() => remove(ex)}
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
      </div>

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
