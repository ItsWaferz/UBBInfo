import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { formatRomanianDate } from '../../utils/format';
import Icon from '../../components/Icon';

// Only links that actually go somewhere are listed.
const QUICK_ACTIONS = [
  { icon: 'edit_note', label: 'Introducere note', to: '/catalog' },
  { icon: 'event', label: 'Programare examene', to: '/examene' },
];

function typeBadgeClass(type) {
  if (type === 'CURS + LAB') return 'badge-courselab';
  if (type === 'SEMINAR') return 'badge-seminar';
  return 'badge-course';
}

export default function ProfessorDashboard() {
  const { user, profile } = useAuth();
  const { t } = useLanguage();
  const [courses, setCourses] = useState([]);
  const [exams, setExams] = useState([]);

  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      try {
        const [courses, exams] = await Promise.all([
          api.get('/api/professor-courses/mine'),
          api.get('/api/exams/teaching'),
        ]);
        if (!active) return;
        setCourses(courses || []);
        setExams(exams || []);
      } catch (err) {
        if (active) console.error('Load professor dashboard failed:', err);
      }
    })();
    return () => {
      active = false;
    };
  }, [user]);

  return (
    <div className="page">
      <section className="welcome-card">
        <Icon name="person" className="welcome-bg-icon" />
        <h1 className="welcome-title">{t('prof.welcome', { name: profile?.full_name })}</h1>
        <p className="welcome-subtitle">
          Anul universitar 2025-2026 · {formatRomanianDate()}
        </p>
      </section>

      <div className="grid-7-5">
        {/* My courses */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="school" />
              {t('prof.courses')}
            </h2>
          </div>
          <div className="card-body">
            <ul className="course-list">
              {courses.map((c) => (
                <li key={c.id} className="course-item">
                  <div className="course-item-main">
                    <span className="course-item-name">{c.courses?.name}</span>
                    <span className="course-item-meta">
                      {c.study_year_label} · {c.student_count} {t('prof.students')}
                    </span>
                  </div>
                  <span className={`badge ${typeBadgeClass(c.type)}`}>{c.type}</span>
                </li>
              ))}
              {courses.length === 0 && <li className="muted">Niciun curs alocat.</li>}
            </ul>
          </div>
        </section>

        {/* Quick actions */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="bolt" />
              {t('prof.quickActions')}
            </h2>
          </div>
          <div className="card-body">
            <ul className="links-list">
              {QUICK_ACTIONS.map((a) => (
                <li key={a.label}>
                  <Link to={a.to} className="link-item">
                    <Icon name={a.icon} className="link-icon" />
                    <span className="link-title">{a.label}</span>
                    <Icon name="arrow_forward" className="link-arrow" />
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      </div>

      {/* Scheduled exams */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="event_available" />
            {t('prof.exams')} — Sesiune Vară
          </h2>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Disciplina</th>
                <th>Data</th>
                <th>Ora</th>
                <th>Sala</th>
                <th style={{ width: '70px', textAlign: 'center', fontSize: '11px' }}>Înscriși</th>
              </tr>
            </thead>
            <tbody>
              {exams.map((e) => (
                <tr key={e.id}>
                  <td>{e.courses?.name}</td>
                  <td>{e.exam_date}</td>
                  <td>{e.exam_time ? e.exam_time.slice(0, 5) : '—'}</td>
                  <td>{e.room}</td>
                  <td className="center">{e.enrolled_count}</td>
                </tr>
              ))}
              {exams.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted center">
                    Niciun examen programat.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
