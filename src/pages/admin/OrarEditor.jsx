import { useEffect, useMemo, useState } from 'react';
import { supabase } from '../../supabaseClient';
import { formatRoom } from '../../utils/rooms';
import Icon from '../../components/Icon';
import RoomPicker from '../../components/RoomPicker';
import Toast from '../../components/Toast';

const DAYS = [
  { n: 1, label: 'Luni' },
  { n: 2, label: 'Marți' },
  { n: 3, label: 'Miercuri' },
  { n: 4, label: 'Joi' },
  { n: 5, label: 'Vineri' },
  { n: 6, label: 'Sâmbătă' },
  { n: 7, label: 'Duminică' },
];
const dayLabel = (n) => DAYS.find((d) => d.n === n)?.label || n;
const hhmm = (t) => (t ? t.slice(0, 5) : '');

const BLANK = {
  day_of_week: '1',
  start_time: '08:00',
  end_time: '10:00',
  course_name: '',
  type: 'CURS',
  week_parity: 'saptamanal',
  room_id: '',
  professor: '',
};

export default function OrarEditor() {
  const [semigroups, setSemigroups] = useState([]);
  const [selected, setSelected] = useState('');
  const [newGroup, setNewGroup] = useState('');
  const [courses, setCourses] = useState([]);
  const [entries, setEntries] = useState([]);
  const [form, setForm] = useState(BLANK);
  const [editId, setEditId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  // initial: list semigroups + course names
  useEffect(() => {
    (async () => {
      const [sgRes, cRes] = await Promise.all([
        supabase.from('orar').select('semigroup, group_name'),
        supabase.from('courses').select('name').order('name'),
      ]);
      const groups = [
        ...new Set(
          (sgRes.data || []).map((r) => r.semigroup || r.group_name).filter(Boolean)
        ),
      ].sort();
      setSemigroups(groups);
      setSelected(groups[0] || '');
      setCourses((cRes.data || []).map((c) => c.name));
    })();
  }, []);

  const loadEntries = async (group) => {
    if (!group) {
      setEntries([]);
      return;
    }
    setLoading(true);
    const { data, error } = await supabase
      .from('orar')
      .select('*, rooms(code, note, buildings(name))')
      .or(`semigroup.eq.${group},and(semigroup.is.null,group_name.eq.${group})`)
      .order('day_of_week')
      .order('start_time');
    if (error) console.error(error);
    setEntries(data || []);
    setLoading(false);
  };

  useEffect(() => {
    loadEntries(selected);
    setEditId(null);
    setForm(BLANK);
  }, [selected]);

  const resetForm = () => {
    setForm(BLANK);
    setEditId(null);
  };

  const edit = (e) => {
    setEditId(e.id);
    setForm({
      day_of_week: String(e.day_of_week),
      start_time: hhmm(e.start_time),
      end_time: hhmm(e.end_time),
      course_name: e.course_name || '',
      type: e.type || 'CURS',
      week_parity: e.week_parity || 'saptamanal',
      room_id: e.room_id || '',
      professor: e.professor || '',
    });
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!selected) {
      flash('error', 'Alege o semigrupă.');
      return;
    }
    if (!form.course_name.trim()) {
      flash('error', 'Disciplina este obligatorie.');
      return;
    }
    const payload = {
      group_name: selected,
      semigroup: selected,
      day_of_week: Number(form.day_of_week),
      start_time: form.start_time,
      end_time: form.end_time,
      course_name: form.course_name.trim(),
      type: form.type,
      week_parity: form.week_parity,
      room_id: form.room_id || null,
      room: null, // display comes from room_id join
      professor: form.professor.trim() || null,
    };
    const { error } = editId
      ? await supabase.from('orar').update(payload).eq('id', editId)
      : await supabase.from('orar').insert(payload);
    if (error) {
      console.error(error);
      flash('error', 'Eroare la salvare.');
      return;
    }
    flash('success', editId ? 'Oră actualizată.' : 'Oră adăugată.');
    resetForm();
    loadEntries(selected);
  };

  const remove = async (e) => {
    if (!window.confirm('Ștergi această oră din orar?')) return;
    const { error } = await supabase.from('orar').delete().eq('id', e.id);
    if (error) return flash('error', 'Eroare la ștergere.');
    flash('success', 'Șters.');
    if (editId === e.id) resetForm();
    loadEntries(selected);
  };

  const addSemigroup = () => {
    const g = newGroup.trim();
    if (!g) return;
    if (!semigroups.includes(g)) setSemigroups((prev) => [...prev, g].sort());
    setSelected(g);
    setNewGroup('');
  };

  const courseOptionsId = useMemo(() => 'orar-course-list', []);

  return (
    <div className="admin-section-grid">
      {/* Editor form */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name={editId ? 'edit' : 'add_circle'} />
            {editId ? 'Editează ora' : 'Adaugă oră'}
          </h2>
        </div>
        <form className="card-body" onSubmit={submit}>
          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Semigrupa</span>
              <div className="input-wrap">
                <Icon name="groups" className="input-icon" />
                <select
                  className="select-bare"
                  value={selected}
                  onChange={(e) => setSelected(e.target.value)}
                >
                  {semigroups.map((g) => (
                    <option key={g} value={g}>
                      {g}
                    </option>
                  ))}
                </select>
              </div>
            </label>
            <label className="field">
              <span className="field-label">Adaugă semigrupă nouă</span>
              <div className="input-wrap">
                <input
                  type="text"
                  value={newGroup}
                  onChange={(e) => setNewGroup(e.target.value)}
                  placeholder="ex. 222/1"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      addSemigroup();
                    }
                  }}
                />
                <button type="button" className="icon-btn" onClick={addSemigroup} aria-label="Adaugă">
                  <Icon name="add" />
                </button>
              </div>
            </label>
          </div>

          <label className="field">
            <span className="field-label">Disciplina</span>
            <div className="input-wrap">
              <input
                type="text"
                list={courseOptionsId}
                value={form.course_name}
                onChange={(e) => setForm({ ...form, course_name: e.target.value })}
                placeholder="ex. Programare web"
              />
              <datalist id={courseOptionsId}>
                {courses.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </div>
          </label>

          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Zi</span>
              <div className="input-wrap">
                <select
                  className="select-bare"
                  value={form.day_of_week}
                  onChange={(e) => setForm({ ...form, day_of_week: e.target.value })}
                >
                  {DAYS.map((d) => (
                    <option key={d.n} value={d.n}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </div>
            </label>
            <label className="field">
              <span className="field-label">Tip</span>
              <div className="input-wrap">
                <select
                  className="select-bare"
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}
                >
                  <option>CURS</option>
                  <option>SEMINAR</option>
                  <option>LABORATOR</option>
                </select>
              </div>
            </label>
          </div>

          <div className="form-grid-2">
            <label className="field">
              <span className="field-label">Ora început</span>
              <div className="input-wrap">
                <input
                  type="time"
                  value={form.start_time}
                  onChange={(e) => setForm({ ...form, start_time: e.target.value })}
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">Ora sfârșit</span>
              <div className="input-wrap">
                <input
                  type="time"
                  value={form.end_time}
                  onChange={(e) => setForm({ ...form, end_time: e.target.value })}
                />
              </div>
            </label>
          </div>

          <label className="field">
            <span className="field-label">Frecvență</span>
            <div className="input-wrap">
              <select
                className="select-bare"
                value={form.week_parity}
                onChange={(e) => setForm({ ...form, week_parity: e.target.value })}
              >
                <option value="saptamanal">Săptămânal</option>
                <option value="par">Doar săptămâni pare</option>
                <option value="impar">Doar săptămâni impare</option>
              </select>
            </div>
          </label>

          <RoomPicker
            value={form.room_id}
            onChange={(rid) => setForm({ ...form, room_id: rid })}
          />

          <label className="field">
            <span className="field-label">Profesor</span>
            <div className="input-wrap">
              <Icon name="person" className="input-icon" />
              <input
                type="text"
                value={form.professor}
                onChange={(e) => setForm({ ...form, professor: e.target.value })}
                placeholder="ex. Conf. Dr. Ion Popescu"
              />
            </div>
          </label>

          <div className="form-actions">
            {editId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm}>
                Anulează
              </button>
            )}
            <button type="submit" className="btn btn-primary">
              {editId ? 'Salvează' : 'Adaugă în orar'}
            </button>
          </div>
        </form>
      </section>

      {/* Current entries */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="calendar_view_week" />
            Orar {selected} ({entries.length})
          </h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Zi</th>
                <th>Ora</th>
                <th>Disciplina</th>
                <th>Tip</th>
                <th>Sala</th>
                <th>Frecv.</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="muted center">
                    Se încarcă…
                  </td>
                </tr>
              ) : entries.length === 0 ? (
                <tr>
                  <td colSpan={7} className="muted center">
                    Nicio oră pentru această semigrupă.
                  </td>
                </tr>
              ) : (
                entries.map((e) => (
                  <tr key={e.id}>
                    <td>{dayLabel(e.day_of_week)}</td>
                    <td className="mono">
                      {hhmm(e.start_time)}–{hhmm(e.end_time)}
                    </td>
                    <td>{e.course_name}</td>
                    <td>{e.type}</td>
                    <td>{formatRoom(e.rooms, e.room)}</td>
                    <td>
                      {e.week_parity === 'saptamanal'
                        ? 'săptămânal'
                        : e.week_parity === 'par'
                        ? 'pare'
                        : 'impare'}
                    </td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="icon-btn" onClick={() => edit(e)}>
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-btn icon-btn-danger"
                          onClick={() => remove(e)}
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

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </div>
  );
}
