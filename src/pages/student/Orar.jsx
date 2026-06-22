import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { supabase } from '../../supabaseClient';
import { useAuth } from '../../contexts/AuthContext';
import { formatRoom } from '../../utils/rooms';
import {
  buildWeeks,
  currentWeekIndex,
  formatWeekRange,
  pickActiveSemester,
} from '../../utils/calendar';
import Icon from '../../components/Icon';

const DAYS = [
  { n: 1, label: 'Luni' },
  { n: 2, label: 'Marți' },
  { n: 3, label: 'Miercuri' },
  { n: 4, label: 'Joi' },
  { n: 5, label: 'Vineri' },
];

function typeClass(type) {
  if (type === 'CURS') return 'badge-courselab';
  if (type === 'SEMINAR') return 'badge-seminar';
  return 'badge-lab';
}
const hhmm = (t) => (t ? t.slice(0, 5) : '');

export default function Orar() {
  const { user, profile, currentRole } = useAuth();
  const myGroup = profile?.group_name;

  const [semester, setSemester] = useState(null);
  const [vacations, setVacations] = useState([]);
  const [semigroups, setSemigroups] = useState([]);
  const [selectedGroup, setSelectedGroup] = useState('');
  const [entries, setEntries] = useState([]);
  const [optionalCourses, setOptionalCourses] = useState([]);
  const [weekIdx, setWeekIdx] = useState(0);
  const [loading, setLoading] = useState(true);

  // Load calendar + vacations + list of semigroups once
  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      const [cfgRes, vacRes, sgRes, optRes] = await Promise.all([
        supabase.from('semester_config').select('*'),
        supabase.from('vacations').select('*'),
        supabase.from('orar').select('semigroup, group_name'),
        supabase.from('courses').select('name').eq('is_optional', true),
      ]);
      if (!active) return;

      setSemester(pickActiveSemester(cfgRes.data || []));
      setVacations(vacRes.data || []);
      setOptionalCourses((optRes.data || []).map((c) => c.name));

      const groups = [
        ...new Set(
          (sgRes.data || []).map((r) => r.semigroup || r.group_name).filter(Boolean)
        ),
      ].sort();
      setSemigroups(groups);
      setSelectedGroup(myGroup && groups.includes(myGroup) ? myGroup : groups[0] || '');
    })();
    return () => {
      active = false;
    };
  }, [user, myGroup]);

  const weeks = useMemo(
    () => (semester ? buildWeeks(semester, vacations) : []),
    [semester, vacations]
  );

  // Default the selected week to the current one once weeks are known
  useEffect(() => {
    if (weeks.length) setWeekIdx(currentWeekIndex(weeks));
  }, [weeks]);

  // Load timetable for the selected semigroup
  useEffect(() => {
    if (!selectedGroup) {
      setEntries([]);
      setLoading(false);
      return;
    }
    let active = true;
    setLoading(true);
    (async () => {
      const { data, error } = await supabase
        .from('orar')
        .select('*, rooms(code, note, location, buildings(name, code))')
        .or(`semigroup.eq.${selectedGroup},and(semigroup.is.null,group_name.eq.${selectedGroup})`)
        .order('day_of_week')
        .order('start_time');
      if (!active) return;
      if (error) {
        console.error('Load orar failed:', error);
        setEntries([]);
      } else {
        setEntries(data || []);
      }
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [selectedGroup]);

  const week = weeks[weekIdx] || null;
  const parity = week?.parity || null; // 'par' | 'impar' | null (vacation)

  // Slots visible this week: weekly ones always, parity ones only on matching weeks
  const byDay = useMemo(() => {
    const map = new Map(DAYS.map((d) => [d.n, []]));
    if (!week || !week.counted) return map; // vacation week → nothing
    for (const e of entries) {
      const show = e.week_parity === 'saptamanal' || e.week_parity === parity;
      if (!show) continue;
      if (!map.has(e.day_of_week)) map.set(e.day_of_week, []);
      map.get(e.day_of_week).push(e);
    }
    return map;
  }, [entries, week, parity]);

  if (currentRole && currentRole !== 'student') return <Navigate to="/" replace />;

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">Orar</h1>
        <p className="page-subtitle">
          Orarul săptămânal pe semigrupe.{' '}
          {semester ? `Anul ${semester.academic_year}, Semestrul ${semester.semester}.` : ''}
        </p>
      </section>

      {/* Controls: semigroup + week navigator */}
      <section className="card">
        <div className="orar-controls">
          <div className="field orar-control">
            <span className="field-label">Semigrupa</span>
            <div className="input-wrap">
              <Icon name="groups" className="input-icon" />
              <select
                className="select-bare"
                value={selectedGroup}
                onChange={(e) => setSelectedGroup(e.target.value)}
              >
                {semigroups.map((g) => (
                  <option key={g} value={g}>
                    {g}
                    {g === myGroup ? ' (grupa ta)' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="orar-week-nav">
            <div className="orar-week-nav-arrows">
              <button
                type="button"
                className="icon-btn"
                onClick={() => setWeekIdx((i) => Math.max(0, i - 1))}
                disabled={weekIdx <= 0}
                aria-label="Săptămâna anterioară"
              >
                <Icon name="chevron_left" />
              </button>

              <div className="orar-week-label">
                {week ? (
                  week.counted ? (
                    <>
                      <span className="orar-week-title">
                        Săptămâna {week.number}
                        <span className={`week-parity-badge ${parity}`}>
                          {parity === 'par' ? 'pară' : 'impară'}
                        </span>
                      </span>
                      <span className="orar-week-range">{formatWeekRange(week)}</span>
                    </>
                  ) : (
                    <>
                      <span className="orar-week-title">
                        {week.vacationName || 'Vacanță'}
                      </span>
                      <span className="orar-week-range">{formatWeekRange(week)}</span>
                    </>
                  )
                ) : (
                  '—'
                )}
              </div>

              <button
                type="button"
                className="icon-btn"
                onClick={() => setWeekIdx((i) => Math.min(weeks.length - 1, i + 1))}
                disabled={weekIdx >= weeks.length - 1}
                aria-label="Săptămâna următoare"
              >
                <Icon name="chevron_right" />
              </button>
            </div>

            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => setWeekIdx(currentWeekIndex(weeks))}
            >
              Săptămâna curentă
            </button>
          </div>
        </div>
      </section>

      {loading ? (
        <section className="card">
          <div className="card-body muted center">Se încarcă orarul…</div>
        </section>
      ) : week && !week.counted ? (
        <section className="card">
          <div className="card-body muted center">
            {week.vacationName || 'Vacanță'} — nu sunt cursuri în această săptămână.
          </div>
        </section>
      ) : (
        <div className="orar-grid">
          {(() => {
            const maxSlots = Math.max(1, ...DAYS.map((day) => (byDay.get(day.n) || []).length));
            return DAYS.map((day) => {
              const slots = byDay.get(day.n) || [];
              return (
                <section className="orar-day" key={day.n} style={{ gridRow: `span ${maxSlots + 1}` }}>
                <div className="orar-day-header">{day.label}</div>
                <div className="orar-day-body">
                  {slots.length === 0 ? (
                    <div className="orar-empty">Liber</div>
                  ) : (
                    slots.map((e) => (
                      <div className={`orar-slot type-${e.type}`} key={e.id}>
                        <div className="orar-slot-time">
                          <Icon name="schedule" />
                          {hhmm(e.start_time)}–{hhmm(e.end_time)}
                        </div>
                        <div className="orar-slot-course">
                          {e.course_name}
                        </div>
                        <div className="orar-slot-meta">
                          <span className={`badge ${typeClass(e.type)}`}>{e.type}</span>
                          {optionalCourses.includes(e.course_name) && (
                            <span className="badge badge-optional">Opțional</span>
                          )}
                          {e.week_parity !== 'saptamanal' && (
                            <span className="orar-parity">
                              {e.week_parity === 'par' ? 'săpt. pară' : 'săpt. impară'}
                            </span>
                          )}
                        </div>
                        <div className="orar-slot-details">
                          <span>
                            <Icon name="meeting_room" />
                            {formatRoom(e.rooms, e.room)}
                          </span>
                          {e.professor && (
                            <span>
                              <Icon name="person" />
                              {e.professor}
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
  );
}
