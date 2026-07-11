import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import { weightedAverage, sumCredits, countsTowardMedia, categoryLabel } from '../../utils/format';
import { getCurrentPeriod, FALLBACK_PERIOD } from '../../utils/academicPeriod';
import { useLanguage } from '../../i18n/LanguageContext';

function fmtAvg(value) {
  return value === null ? '—' : value.toFixed(2);
}

function GradeBadge({ grade }) {
  if (grade === null || grade === undefined)
    return <span className="grade-badge grade-empty">—</span>;
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
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState(FALLBACK_PERIOD);
  const [activeYear, setActiveYear] = useState(FALLBACK_PERIOD.academic_year);
  const currentYear = period.academic_year;
  const currentSemester = period.semester;

  useEffect(() => {
    let active = true;
    getCurrentPeriod().then((p) => {
      if (active && p) {
        setPeriod(p);
        setActiveYear((y) => (y === FALLBACK_PERIOD.academic_year ? p.academic_year : y));
      }
    });
    return () => { active = false; };
  }, []);

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
      } finally {
        if (active) setLoading(false);
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
      setActiveYear(years.includes(currentYear) ? currentYear : years[years.length - 1]);
    }
  }, [years, activeYear, currentYear]);

  // Stat cards
  const semesterAvg = useMemo(
    () =>
      weightedAverage(
        enrollments.filter(
          (e) => e.academic_year === currentYear && e.semester === currentSemester
        )
      ),
    [enrollments, currentYear, currentSemester]
  );
  const yearAvg = useMemo(
    () =>
      weightedAverage(enrollments.filter((e) => e.academic_year === currentYear)),
    [enrollments, currentYear]
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

  // Unresolved restanțe carried over from earlier semesters — the rule itself
  // is computed SERVER-SIDE (carried_restanta on each enrollment), so this page
  // never disagrees with tuition or the professor catalog. Current-year rows
  // already render in their own semester card, so they're not listed again.
  const carriedRestante = useMemo(
    () =>
      enrollments.filter(
        (e) => e.carried_restanta && e.academic_year !== currentYear
      ),
    [enrollments, currentYear]
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
        // Surface each carried restanță in the current year's semester that
        // matches its ORIGINAL semester (a sem-1 restanță shows under sem 1,
        // as if it were a course you have to retake this year in that semester).
        // Restanțe whose semester has no card this year fall back to the last
        // card so they never silently disappear.
        const semNums = semesters.map(([s]) => s);
        const isLastCard = semester === semNums[semNums.length - 1];
        const carried =
          activeYear === currentYear
            ? carriedRestante.filter(
                (e) =>
                  e.semester === semester ||
                  (isLastCard && !semNums.includes(e.semester))
              )
            : [];
        // Credits from carried restanțe shown on this card — added to the total,
        // in red, so the workload (this year + restanțe) is clear.
        const restanteCredits = carried.reduce(
          (acc, e) => acc + (e.courses?.credits ?? 0),
          0
        );
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
                  {restanteCredits > 0 && (
                    <strong className="text-danger"> + {restanteCredits}</strong>
                  )}
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
                        {categoryLabel(e.courses) && <span className="badge badge-optional">{categoryLabel(e.courses)}</span>}
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
                        {categoryLabel(e.courses) && <span className="badge badge-optional">{categoryLabel(e.courses)}</span>}
                        <span className="restanta-tag">
                          {' '}
                          {t('grades.restantaFrom', { year: e.academic_year, sem: e.semester })}
                        </span>
                      </td>
                      <td>{e.courses?.credits}</td>
                      <td><GradeBadge grade={null} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        );
      })}

      {/* While loading show a spinner, not the "no grades" empty state —
          it used to flash for half a second on every visit. */}
      {loading && (
        <section className="card">
          <div className="card-body" style={{ display: 'flex', justifyContent: 'center', padding: 32 }}>
            <span className="spinner-page" />
          </div>
        </section>
      )}
      {!loading && semesters.length === 0 && (
        <section className="card">
          <div className="card-body muted center">
            {t('grades.noGrades')}
          </div>
        </section>
      )}
      {semesters.length > 0 && activeYear === currentYear && (() => {
        // Find courses for the current semester in the active year
        const currentSem = semesters.find(([sem]) => sem === currentSemester) || semesters[semesters.length - 1];
        if (!currentSem) return null;
        // Only media-counting courses (excludes optionals + language courses like English).
        const calcCourses = currentSem[1].filter(countsTowardMedia);
        return <GradeCalculator courses={calcCourses} semester={currentSem[0]} t={t} />;
      })()}
    </div>
  );
}

function GradeCalculator({ courses, semester, t }) {
  const [simulatedGrades, setSimulatedGrades] = useState({});

  // Pre-fill each row with the student's existing grade (editable, so they can
  // simulate a "mărire"). Re-seed only when the course set or grades actually
  // change — not on every re-render — so the user's edits aren't wiped.
  const initKey = courses.map((e) => `${e.id}:${e.grade ?? ''}`).join('|');
  useEffect(() => {
    const initial = {};
    courses.forEach(e => {
      initial[e.id] = e.grade !== null && e.grade !== undefined ? e.grade.toString() : '';
    });
    setSimulatedGrades(initial);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initKey]);

  const handleGradeChange = (id, val) => {
    setSimulatedGrades(prev => ({ ...prev, [id]: val }));
  };

  let sumGC = 0;
  let sumC = 0;
  let totalSimCredits = 0;

  // English/optionals are already filtered out upstream, so every row counts.
  courses.forEach(e => {
    const val = simulatedGrades[e.id];
    const credits = e.courses?.credits ?? 0;
    totalSimCredits += credits;

    if (val && val.trim() !== '') {
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
                  {categoryLabel(e.courses) && <span className="badge badge-optional">{categoryLabel(e.courses)}</span>}
                </td>
                <td>{e.courses?.credits}</td>
                <td>
                  <input
                    type="number"
                    className="grade-sim-input"
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
