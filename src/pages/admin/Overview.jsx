import { useEffect, useState } from 'react';
import { supabase } from '../../supabaseClient';
import Icon from '../../components/Icon';

export default function Overview() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const [rolesRes, profilesRes, coursesRes, enrollRes, evalsRes] = await Promise.all([
          supabase.from('user_roles').select('user_id, roles(name)'),
          supabase.from('profiles').select('id', { count: 'exact', head: true }),
          supabase.from('courses').select('id', { count: 'exact', head: true }),
          supabase.from('enrollments').select('grade'),
          supabase.from('professor_evaluations').select('professor_id'),
        ]);

        if (rolesRes.error) throw rolesRes.error;

        // Distinct users per role
        const perRole = { student: new Set(), profesor: new Set(), administrator: new Set() };
        for (const r of rolesRes.data || []) {
          const name = r.roles?.name;
          if (perRole[name]) perRole[name].add(r.user_id);
        }

        const enrollments = enrollRes.data || [];
        const graded = enrollments.filter((e) => e.grade !== null && e.grade !== undefined);

        if (!active) return;
        setStats({
          students: perRole.student.size,
          professors: perRole.profesor.size,
          admins: perRole.administrator.size,
          profiles: profilesRes.count ?? 0,
          courses: coursesRes.count ?? 0,
          enrollments: enrollments.length,
          graded: graded.length,
          pending: enrollments.length - graded.length,
          evaluatedProfs: new Set((evalsRes.data || []).map(e => e.professor_id).filter(Boolean)).size,
        });
      } catch (err) {
        console.error('Overview stats failed:', err);
        if (active) setError(true);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  if (error) {
    return (
      <section className="card">
        <div className="card-body muted center">
          Nu am putut încărca statisticile. Verifică politicile RLS pentru admin.
        </div>
      </section>
    );
  }

  if (!stats) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă statisticile…</div>
      </section>
    );
  }

  const percentageGraded = stats.enrollments > 0
    ? Math.round((stats.graded / stats.enrollments) * 100)
    : 0;

  const cards = [
    { icon: 'groups', value: stats.students, label: 'Studenți', tone: 'blue' },
    { icon: 'co_present', value: stats.professors, label: 'Cadre didactice', tone: 'gold' },
    { icon: 'admin_panel_settings', value: stats.admins, label: 'Administratori', tone: 'green' },
    { icon: 'menu_book', value: stats.courses, label: 'Discipline', tone: 'blue' },
    { icon: 'star', value: stats.evaluatedProfs, label: 'Cadre didactice evaluate', tone: 'gold' },
    { icon: 'grading', value: `${percentageGraded}%`, label: 'Note acordate', tone: 'green' },
  ];

  return (
    <>
      <div className="admin-stats-grid">
        {cards.map((c) => (
          <div className="stat-card stat-card-icon" key={c.label}>
            <div className={`stat-icon stat-icon-${c.tone}`}>
              <Icon name={c.icon} />
            </div>
            <div>
              <div className="stat-value">{c.value}</div>
              <div className="stat-label">{c.label}</div>
            </div>
          </div>
        ))}
      </div>

      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="insights" />
            Stadiu notare
          </h2>
        </div>
        <div className="card-body">
          <div className="progress-row">
            <div className="progress-label">
              <span>Note acordate</span>
              <span>
                {stats.graded} / {stats.enrollments}
              </span>
            </div>
            <div className="progress-track">
              <div
                className="progress-fill"
                style={{
                  width:
                    stats.enrollments > 0
                      ? `${(stats.graded / stats.enrollments) * 100}%`
                      : '0%',
                }}
              />
            </div>
            <p className="muted" style={{ fontSize: 13 }}>
              {stats.pending} înscrieri fără notă (în curs).
            </p>
          </div>
        </div>
      </section>
    </>
  );
}
