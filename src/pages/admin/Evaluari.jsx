import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { EVAL_CRITERIA } from '../../utils/evaluationCriteria';
import Icon from '../../components/Icon';

function professorTitle(p) {
  if (!p) return 'Profesor necunoscut';
  const bits = [p.academic_rank, p.honorifics].filter(Boolean).join(' ');
  return bits ? `${bits} ${p.full_name}` : p.full_name;
}

function fmtDate(d) {
  if (!d) return '';
  try {
    return new Intl.DateTimeFormat('ro-RO', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    }).format(new Date(d));
  } catch {
    return '';
  }
}

function StarRating({ value }) {
  const rounded = Math.round(value);
  return (
    <span className="stars-readonly" title={value.toFixed(2)}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Icon key={n} name="star" className={n <= rounded ? 'on' : ''} />
      ))}
    </span>
  );
}

export default function Evaluari() {
  const [evals, setEvals] = useState([]);
  const [profMap, setProfMap] = useState({});
  const [courseMap, setCourseMap] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        // Backend returns anonymized evaluations (never any student_id).
        const data = await api.get('/api/admin/evaluations');
        if (!active) return;
        setEvals(data.evaluations || []);
        setCourseMap(Object.fromEntries((data.courses || []).map((c) => [c.id, c.name])));
        setProfMap(Object.fromEntries((data.professors || []).map((p) => [p.id, p])));
      } catch (err) {
        console.error('Load evaluations failed:', err);
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  // Aggregate per professor
  const perProfessor = useMemo(() => {
    const map = new Map();
    for (const ev of evals) {
      if (!map.has(ev.professor_id)) {
        map.set(ev.professor_id, { count: 0, sums: {}, comments: [] });
      }
      const agg = map.get(ev.professor_id);
      agg.count += 1;
      for (const c of EVAL_CRITERIA) {
        const v = ev.ratings?.[c.key];
        if (typeof v === 'number') {
          agg.sums[c.key] = (agg.sums[c.key] || { total: 0, n: 0 });
          agg.sums[c.key].total += v;
          agg.sums[c.key].n += 1;
        }
      }
      if (ev.comment) {
        agg.comments.push({
          comment: ev.comment,
          course: courseMap[ev.course_id] || null,
          date: ev.created_at,
        });
      }
    }
    // build sortable list with overall average
    return [...map.entries()]
      .map(([profId, agg]) => {
        const averages = {};
        let overallTotal = 0;
        let overallN = 0;
        for (const c of EVAL_CRITERIA) {
          const s = agg.sums[c.key];
          averages[c.key] = s ? s.total / s.n : null;
          if (s) {
            overallTotal += s.total;
            overallN += s.n;
          }
        }
        return {
          profId,
          professor: profMap[profId],
          count: agg.count,
          averages,
          overall: overallN ? overallTotal / overallN : null,
          comments: agg.comments,
        };
      })
      .sort((a, b) => (b.overall || 0) - (a.overall || 0));
  }, [evals, profMap, courseMap]);

  if (loading) {
    return (
      <section className="card">
        <div className="card-body muted center">Se încarcă evaluările…</div>
      </section>
    );
  }

  if (perProfessor.length === 0) {
    return (
      <section className="card">
        <div className="card-body muted center">
          Nu există evaluări trimise de studenți încă.
        </div>
      </section>
    );
  }

  return (
    <>
      <section className="card">
        <div className="card-body">
          <p className="muted">
            <Icon name="visibility_off" style={{ verticalAlign: 'middle', marginRight: 6 }} size={18} />
            Evaluările sunt <strong>anonime</strong>. Identitatea studenților nu este
            stocată în acest raport.
          </p>
        </div>
      </section>

      {perProfessor.map((p) => (
        <section className="card" key={p.profId}>
          <div className="card-header">
            <h2 className="card-title">
              {professorTitle(p.professor)}
            </h2>
            <div className="eval-overall">
              <StarRating value={p.overall || 0} />
              <span className="eval-overall-num">
                {p.overall != null ? p.overall.toFixed(2) : '—'}
              </span>
              <span className="badge badge-course">{p.count} evaluări</span>
            </div>
          </div>
          <div className="card-body">
            <div className="eval-criteria">
              {EVAL_CRITERIA.map((c) => (
                <div className="eval-criterion" key={c.key}>
                  <span className="eval-criterion-label">{c.label}</span>
                  <span className="eval-criterion-score">
                    <StarRating value={p.averages[c.key] || 0} />
                    <span className="eval-criterion-num">
                      {p.averages[c.key] != null ? p.averages[c.key].toFixed(2) : '—'}
                    </span>
                  </span>
                </div>
              ))}
            </div>

            {p.comments.length > 0 && (
              <div className="eval-comments">
                <div className="eval-comments-title">
                  Comentarii ({p.comments.length})
                </div>
                {p.comments.map((c, i) => (
                  <div className="eval-comment" key={i}>
                    <Icon name="format_quote" />
                    <div>
                      <p>{c.comment}</p>
                      <span className="eval-comment-meta">
                        {c.course ? `${c.course} · ` : ''}
                        {fmtDate(c.date)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      ))}
    </>
  );
}
