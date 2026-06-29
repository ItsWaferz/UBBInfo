import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
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
    return <span className="muted" style={{ display: 'inline-flex', height: '28px', alignItems: 'center' }}>—</span>;
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
      try {
        const data = await api.get('/api/enrollments/me');
        if (!active) return;
        // Use the computed final grade (decimal) as the effective grade when present.
        setEnrollments(
          (data || []).map((e) => ({
            ...e,
            grade: e.final_grade != null ? e.final_grade : e.grade,
          }))
        );
      } catch (err) {
        if (active) console.error('Load grades failed:', err);
      }
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
                    <th style={{ width: '70%' }}>{t('table.discipline')}</th>
                    <th style={{ width: '15%' }}>{t('table.credits')}</th>
                    <th style={{ width: '15%' }}>{t('table.grade')}</th>
                  </tr>
                </thead>
                <tbody>
                  {courses.map((e) => (
                    <tr key={e.id}>
                      <td>
                        {e.courses?.name}
                        {e.courses?.is_optional && <span className="badge badge-optional">Opțional</span>}
                        {e.is_restanta && (
                          <span className="restanta-tag"> {t('status.restanta')}</span>
                        )}
                        {e.grade_breakdown?.components && (
                          <div className="muted" style={{ fontSize: 11, marginTop: 2 }}>
                            {e.grade_breakdown.components
                              .filter((c) => c.value != null)
                              .map((c, i) => (
                                <span key={i}>
                                  {i > 0 ? ' · ' : ''}
                                  {c.name} {c.value}
                                  {c.weight ? ` (${c.weight}%${c.is_bonus ? ' bonus' : ''})` : ''}
                                </span>
                              ))}
                          </div>
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
                        {e.courses?.is_optional && <span className="badge badge-optional">Opțional</span>}
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
      {semesters.length > 0 && activeYear === CURRENT_YEAR && (() => {
        // Find courses for the current semester in the active year
        const currentSem = semesters.find(([sem]) => sem === 2) || semesters[semesters.length - 1];
        if (!currentSem) return null;
        // Exclude optional courses from the calculator
        const calcCourses = currentSem[1].filter(e => !e.courses?.is_optional);
        return <GradeCalculator courses={calcCourses} semester={currentSem[0]} t={t} />;
      })()}
    </div>
  );
}

function GradeCalculator({ courses, semester, t }) {
  const [simulatedGrades, setSimulatedGrades] = useState({});

  useEffect(() => {
    const initial = {};
    courses.forEach(e => {
      initial[e.id] = e.grade !== null && e.grade !== undefined ? e.grade.toString() : '';
    });
    setSimulatedGrades(initial);
  }, [courses]);

  const handleGradeChange = (id, val) => {
    setSimulatedGrades(prev => ({ ...prev, [id]: val }));
  };

  let sumGC = 0;
  let sumC = 0;
  let totalSimCredits = 0;
  
  courses.forEach(e => {
    const val = simulatedGrades[e.id];
    const credits = e.courses?.credits ?? 0;
    totalSimCredits += credits;
    
    const isEnglish = e.courses?.name?.toLowerCase().includes('engleza');
    if (!isEnglish && val && val.trim() !== '') {
      const gradeNum = parseFloat(val);
      if (!isNaN(gradeNum)) {
        sumGC += gradeNum * credits;
        sumC += credits;
      }
    }
  });

  const avg = sumC > 0 ? sumGC / sumC : null;

  return (
    <section className="card" style={{ marginTop: 32 }}>
      <div className="card-header semester-header">
        <h2 className="card-title">Calculator medie (Semestrul {semester})</h2>
        <div className="semester-meta">
          <span>
            {t('grades.average')}: <strong>{fmtAvg(avg)}</strong>
          </span>
          <span>
            {t('grades.totalCredits')}: <strong>{totalSimCredits}</strong>
          </span>
        </div>
      </div>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: '70%' }}>{t('table.discipline')}</th>
              <th style={{ width: '15%' }}>{t('table.credits')}</th>
              <th style={{ width: '15%' }}>{t('table.grade')}</th>
            </tr>
          </thead>
          <tbody>
            {courses.map((e) => (
              <tr key={e.id}>
                <td>
                  {e.courses?.name}
                  {e.courses?.is_optional && <span className="badge badge-optional">Opțional</span>}
                </td>
                <td>{e.courses?.credits}</td>
                <td>
                  <input
                    type="number"
                    className="form-control"
                    style={{ 
                      padding: '2px 4px', 
                      width: '60px', 
                      height: '28px',
                      background: 'transparent', 
                      border: '1px solid var(--outline-variant)', 
                      borderRadius: 'var(--radius)',
                      color: 'var(--on-surface)'
                    }}
                    min="1"
                    max="10"
                    step="1"
                    value={simulatedGrades[e.id] ?? ''}
                    onChange={(ev) => handleGradeChange(e.id, ev.target.value)}
                    placeholder="—"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
