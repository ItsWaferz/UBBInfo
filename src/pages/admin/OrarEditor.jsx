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
const hhmm = (t) => (t ? t.slice(0, 5) : '');

function typeClass(type) {
  if (type === 'CURS') return 'badge-courselab';
  if (type === 'SEMINAR') return 'badge-seminar';
  return 'badge-lab';
}

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
  const [allEntries, setAllEntries] = useState([]);
  const [courses, setCourses] = useState([]);
  const [optionalCourses, setOptionalCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  // Accordion state
  const [openGroup, setOpenGroup] = useState(null);

  // Edit modal state
  const [editEntry, setEditEntry] = useState(null); // entry being edited, or BLANK for new
  const [editTarget, setEditTarget] = useState(null); // semigroup for new entries
  const [form, setForm] = useState(BLANK);
  const [saving, setSaving] = useState(false);

  // New semigroup
  const [newGroup, setNewGroup] = useState('');

  const flash = (variant, message) => {
    setToast({ variant, message });
    setTimeout(() => setToast(null), 3000);
  };

  const loadAll = async () => {
    setLoading(true);
    const [orarRes, cRes, optRes] = await Promise.all([
      supabase
        .from('orar')
        .select('*, rooms(code, note, location, buildings(name, code))')
        .order('day_of_week')
        .order('start_time'),
      supabase.from('courses').select('name').order('name'),
      supabase.from('courses').select('name').eq('is_optional', true),
    ]);

    if (orarRes.error) console.error(orarRes.error);
    if (cRes.error) console.error(cRes.error);

    const data = orarRes.data || [];
    setAllEntries(data);
    setCourses((cRes.data || []).map((c) => c.name));
    setOptionalCourses((optRes.data || []).map((c) => c.name));

    const groups = [
      ...new Set(data.map((r) => r.semigroup || r.group_name).filter(Boolean)),
    ].sort();
    setSemigroups(groups);
    setLoading(false);
  };

  useEffect(() => {
    loadAll();
  }, []);

  // Group entries by semigroup
  const entriesByGroup = useMemo(() => {
    const map = {};
    for (const e of allEntries) {
      const g = e.semigroup || e.group_name || '—';
      if (!map[g]) map[g] = [];
      map[g].push(e);
    }
    return map;
  }, [allEntries]);

  // Group a semigroup's entries by day
  const entriesByDay = (groupEntries) => {
    const map = new Map(DAYS.map((d) => [d.n, []]));
    for (const e of groupEntries) {
      if (!map.has(e.day_of_week)) map.set(e.day_of_week, []);
      map.get(e.day_of_week).push(e);
    }
    return map;
  };

  const toggleGroup = (g) => {
    setOpenGroup((prev) => (prev === g ? null : g));
  };

  const addSemigroup = () => {
    const g = newGroup.trim();
    if (!g) return;
    if (!semigroups.includes(g)) setSemigroups((prev) => [...prev, g].sort());
    setOpenGroup(g);
    setNewGroup('');
  };

  // ── Edit modal ──
  const openEdit = (entry, group) => {
    setEditEntry(entry);
    setEditTarget(group);
    setForm({
      day_of_week: String(entry.day_of_week),
      start_time: hhmm(entry.start_time),
      end_time: hhmm(entry.end_time),
      course_name: entry.course_name || '',
      type: entry.type || 'CURS',
      week_parity: entry.week_parity || 'saptamanal',
      room_id: entry.room_id || '',
      professor: entry.professor || '',
    });
  };

  const openNew = (group) => {
    setEditEntry({ _new: true });
    setEditTarget(group);
    setForm(BLANK);
  };

  const closeModal = () => {
    setEditEntry(null);
    setEditTarget(null);
    setForm(BLANK);
  };

  const submitEdit = async (e) => {
    e.preventDefault();
    if (!form.course_name.trim()) {
      flash('error', 'Disciplina este obligatorie.');
      return;
    }
    const payload = {
      group_name: editTarget,
      semigroup: editTarget,
      day_of_week: Number(form.day_of_week),
      start_time: form.start_time,
      end_time: form.end_time,
      course_name: form.course_name.trim(),
      type: form.type,
      week_parity: form.week_parity,
      room_id: form.room_id || null,
      room: null,
      professor: form.professor.trim() || null,
    };
    setSaving(true);
    const { error } = editEntry._new
      ? await supabase.from('orar').insert(payload)
      : await supabase.from('orar').update(payload).eq('id', editEntry.id);
    setSaving(false);
    if (error) {
      console.error(error);
      flash('error', 'Eroare la salvare.');
      return;
    }
    flash('success', editEntry._new ? 'Oră adăugată.' : 'Oră actualizată.');
    closeModal();
    loadAll();
  };

  const remove = async (entry) => {
    if (!window.confirm('Ștergi această oră din orar?')) return;
    const { error } = await supabase.from('orar').delete().eq('id', entry.id);
    if (error) return flash('error', 'Eroare la ștergere.');
    flash('success', 'Șters.');
    loadAll();
  };

  const courseOptionsId = useMemo(() => 'orar-course-list', []);

  if (loading) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă orarul…</div>
      </section>
    );
  }

  return (
    <>
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="calendar_view_week" />
            Orar
          </h2>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <div className="input-wrap" style={{ minWidth: 140 }}>
              <input
                type="text"
                value={newGroup}
                onChange={(e) => setNewGroup(e.target.value)}
                placeholder="Semigrupă nouă…"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addSemigroup();
                  }
                }}
                style={{ fontSize: 13 }}
              />
              <button type="button" className="icon-btn" onClick={addSemigroup} aria-label="Adaugă">
                <Icon name="add" />
              </button>
            </div>
          </div>
        </div>

        <div className="card-body" style={{ padding: 0, gap: 0 }}>
          {semigroups.map((group) => {
            const groupEntries = entriesByGroup[group] || [];
            const byDay = entriesByDay(groupEntries);
            const isOpen = openGroup === group;

            // Find which days actually have entries
            const activeDays = DAYS.filter((d) => (byDay.get(d.n) || []).length > 0);

            return (
              <div key={group} className={`users-accordion ${isOpen ? 'open' : ''}`}>
                <button
                  type="button"
                  className={`users-accordion-trigger${isOpen ? ' open' : ''}`}
                  onClick={() => toggleGroup(group)}
                >
                  <div className="users-accordion-left">
                    <Icon name="groups" />
                    <span className="users-accordion-label">{group}</span>
                    <span className="users-accordion-count">{groupEntries.length} ore</span>
                  </div>
                  <Icon name="expand_more" className={`users-accordion-chevron${isOpen ? ' rotated' : ''}`} />
                </button>

                {isOpen && (
                  <div className="users-accordion-body" style={{ padding: '12px 16px 16px' }}>
                    {/* Add button for this group */}
                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => openNew(group)}
                      >
                        <Icon name="add_circle" />
                        Adaugă oră
                      </button>
                    </div>

                    {groupEntries.length === 0 ? (
                      <p className="muted center" style={{ padding: 16 }}>Nicio oră pentru această semigrupă.</p>
                    ) : (
                      <div className="orar-grid">
                        {(() => {
                          const daysToRender = activeDays.length > 0 ? activeDays : DAYS.slice(0, 5);
                          const maxSlots = Math.max(1, ...daysToRender.map((day) => (byDay.get(day.n) || []).length));
                          return daysToRender.map((day) => {
                            const slots = byDay.get(day.n) || [];
                            return (
                              <section className="orar-day" key={day.n} style={{ gridRow: `span ${maxSlots + 1}` }}>
                              <div className="orar-day-header">{day.label}</div>
                              <div className="orar-day-body">
                                {slots.length === 0 ? (
                                  <div className="orar-empty">Liber</div>
                                ) : (
                                  slots.map((entry) => (
                                    <div className={`orar-slot type-${entry.type}`} key={entry.id}>
                                      <div className="orar-slot-time">
                                        <Icon name="schedule" />
                                        {hhmm(entry.start_time)}–{hhmm(entry.end_time)}
                                        {/* Edit button inline */}
                                        <button
                                          type="button"
                                          className="orar-slot-edit-btn"
                                          onClick={() => openEdit(entry, group)}
                                          aria-label="Editează"
                                        >
                                          <Icon name="edit" />
                                        </button>
                                      </div>
                                      <div className="orar-slot-course">
                                        {entry.course_name}
                                      </div>
                                      <div className="orar-slot-meta">
                                        <span className={`badge ${typeClass(entry.type)}`}>{entry.type}</span>
                                        {optionalCourses.includes(entry.course_name) && (
                                          <span className="badge badge-optional">Opțional</span>
                                        )}
                                        {entry.week_parity !== 'saptamanal' && (
                                          <span className={`badge ${entry.week_parity === 'par' ? 'badge-week-par' : 'badge-week-impar'}`}>
                                            {entry.week_parity === 'par' ? 'Săpt. pară' : 'Săpt. impară'}
                                          </span>
                                        )}
                                      </div>
                                      <div className="orar-slot-details">
                                        <span>
                                          <Icon name="meeting_room" />
                                          {formatRoom(entry.rooms, entry.room)}
                                        </span>
                                        {entry.professor && (
                                          <span>
                                            <Icon name="person" />
                                            {entry.professor}
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                  ))
                                )}
                              </div>
                            </section>
                          );
                          });
                        })()}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
          {semigroups.length === 0 && (
            <p className="muted center" style={{ padding: 24 }}>
              Nicio semigrupă. Adaugă una din câmpul de sus.
            </p>
          )}
        </div>
      </section>

      {/* ── Edit / Add Modal ── */}
      {editEntry && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-card modal-card-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editEntry._new ? 'Adaugă oră' : 'Editează ora'}</h3>
              <button type="button" className="modal-close" onClick={closeModal} aria-label="Închide">
                <Icon name="close" />
              </button>
            </div>

            <form onSubmit={submitEdit} className="modal-body">
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

              {!editEntry._new && (
                <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    style={{ color: '#c62828' }}
                    onClick={() => {
                      remove(editEntry);
                      closeModal();
                    }}
                  >
                    <Icon name="delete" />
                    Șterge ora
                  </button>
                </div>
              )}

              <div className="modal-actions">
                <button type="button" className="btn btn-ghost" onClick={closeModal}>
                  Anulează
                </button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? <span className="spinner" /> : editEntry._new ? 'Adaugă' : 'Salvează'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <Toast visible={!!toast} variant={toast?.variant || 'success'} message={toast?.message || ''} />
    </>
  );
}
