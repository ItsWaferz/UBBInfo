import { useEffect, useMemo, useState } from 'react';
import { supabase } from '../../supabaseClient';
import { useAuth } from '../../contexts/AuthContext';
import { weightedAverage, sumCredits } from '../../utils/format';
import { useLanguage } from '../../i18n/LanguageContext';

const CURRENT_YEAR = '2025-2026';
const CURRENT_SEMESTER = 2;

function fmtAvg(value) {
  return value === null ? '—' : value.toFixed(2);
}

function GradeBadge({ grade }) {
  if (grade === null || grade === undefined)
    return <span className="muted">—</span>;
  const cls = grade >= 5 ? 'grade-pass' : 'grade-fail';
  return <span className={`grade-badge ${cls}`}>{grade}</span>;
}

function StatusCell({ grade, t }) {
  if (grade === null || grade === undefined)
    return <span className="status-inprogress">{t('grades.inProgress')}</span>;
  if (grade >= 5) return <span className="badge status-pass">{t('status.passed')}</span>;
  return <span className="badge status-fail">{t('status.failed')}</span>;
}

export default function Grades() {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [enrollments, setEnrollments] = useState([]);
  const [activeYear, setActiveYear] = useState(CURRENT_YEAR);

  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      const { data, error } = await supabase
        .from('enrollments')
        .select('*, courses(*)')
        .eq('student_id', user.id)
        .order('academic_year', { ascending: true })
        .order('semester', { ascending: true });
      if (!active) return;
      if (error) {
        console.error('Load grades failed:', error);
        return;
      }
      setEnrollments(data || []);
    })();
    return () => {
      active = false;
    };
  }, [user]);

  const years = useMemo(() => {
    const set = new Set(enrollments.map((e) => e.academic_year));
    return Array.from(set).sort();
  }, [enrollments]);

  // Ensure the active tab exists once data loads
  useEffect(() => {
    if (years.length && !years.includes(activeYear)) {
      setActiveYear(years.includes(CURRENT_YEAR) ? CURRENT_YEAR : years[years.length - 1]);
    }
  }, [years, activeYear]);

  // Stat cards
  const semesterAvg = useMemo(
    () =>
      weightedAverage(
        enrollments.filter(
          (e) => e.academic_year === CURRENT_YEAR && e.semester === CURRENT_SEMESTER
        )
      ),
    [enrollments]
  );
  const yearAvg = useMemo(
    () =>
      weightedAverage(enrollments.filter((e) => e.academic_year === CURRENT_YEAR)),
    [enrollments]
  );
  const generalAvg = useMemo(() => weightedAverage(enrollments), [enrollments]);

  // Semesters within selected year
  const semesters = useMemo(() => {
    const yearEnrollments = enrollments.filter((e) => e.academic_year === activeYear);
    const bySemester = new Map();
    for (const e of yearEnrollments) {
      if (!bySemester.has(e.semester)) bySemester.set(e.semester, []);
      bySemester.get(e.semester).push(e);
    }
    return Array.from(bySemester.entries()).sort((a, b) => a[0] - b[0]);
  }, [enrollments, activeYear]);

  // Unresolved restanțe carried over from earlier semesters — these are
  // surfaced inside the current semester (an 2, sem 2), not their original one.
  const carriedRestante = useMemo(
    () =>
      enrollments.filter(
        (e) =>
          e.is_restanta &&
          (e.grade === null || e.grade < 5) &&
          !(e.academic_year === CURRENT_YEAR && e.semester === CURRENT_SEMESTER)
      ),
    [enrollments]
  );

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">{t('grades.title')}</h1>
        <p className="page-subtitle">
          {t('grades.subtitle')}
        </p>
      </section>

      {/* Stats row */}
      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-label">{t('grades.semesterAvg')}</div>
          <div className="stat-value">{fmtAvg(semesterAvg)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">{t('grades.yearAvg')}</div>
          <div className="stat-value">{fmtAvg(yearAvg)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">{t('grades.generalAvg')}</div>
          <div className="stat-value">{fmtAvg(generalAvg)}</div>
        </div>
      </div>

      {/* Year tabs */}
      <div className="year-tabs">
        {years.map((y) => (
          <button
            key={y}
            type="button"
            className={`year-tab ${y === activeYear ? 'active' : ''}`}
            onClick={() => setActiveYear(y)}
          >
            {y}
          </button>
        ))}
      </div>

      {/* Semester cards */}
      {semesters.map(([semester, courses]) => {
        const avg = weightedAverage(courses);
        const credits = sumCredits(courses);
        const isCurrent =
          activeYear === CURRENT_YEAR && semester === CURRENT_SEMESTER;
        const carried = isCurrent ? carriedRestante : [];
        return (
          <section className="card" key={semester}>
            <div className="card-header semester-header">
              <h2 className="card-title">{t('grades.semester', { n: semester })}</h2>
              <div className="semester-meta">
                <span>
                  {t('grades.average')}: <strong>{fmtAvg(avg)}</strong>
                </span>
                <span>
                  {t('grades.totalCredits')}: <strong>{credits}</strong>
                </span>
              </div>
            </div>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{t('table.discipline')}</th>
                    <th>{t('table.credits')}</th>
                    <th>{t('table.grade')}</th>
                  </tr>
                </thead>
                <tbody>
                  {courses.map((e) => (
                    <tr key={e.id}>
                      <td>
                        {e.courses?.name}
                        {e.is_restanta && (
                          <span className="restanta-tag"> {t('status.restanta')}</span>
                        )}
                      </td>
                      <td>{e.courses?.credits}</td>
                      <td>
                        <GradeBadge grade={e.grade} />
                      </td>
                    </tr>
                  ))}

                  {/* Carried-over restanțe from previous semesters */}
                  {carried.map((e) => (
                    <tr key={`carry-${e.id}`}>
                      <td className="text-danger">
                        {e.courses?.name}
                        <span className="restanta-tag">
                          {' '}
                          {t('grades.restantaFrom', { year: e.academic_year, sem: e.semester })}
                        </span>
                      </td>
                      <td>{e.courses?.credits}</td>
                      <td className="muted">—</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        );
      })}

      {semesters.length === 0 && (
        <section className="card">
          <div className="card-body muted center">
            {t('grades.noGrades')}
          </div>
        </section>
      )}
    </div>
  );
}
