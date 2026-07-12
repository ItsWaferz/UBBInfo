// Admin: complete tuition & fees statistic — who owes what and what they've paid.
import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { lei } from '../../utils/format';
import Icon from '../../components/Icon';

/** Split "Program (Limba X)" -> { program, language } (mirrors the edit modal). */
function parseSpec(s) {
  const str = s || '';
  const m = str.match(/^(.*?)\s*\(\s*limba\s+([^)]+?)\s*\)\s*$/i);
  if (m) return { program: m[1].trim(), language: m[2].trim() };
  return { program: str.trim() || 'Fără specializare', language: '' };
}

const amountCell = (r) => (Number(r.outstanding) > 0
  ? <span className="text-danger">{lei(r.outstanding)}</span>
  : <span className="badge status-pass">achitat</span>);

export default function Taxe() {
  const [data, setData] = useState(null);
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('all'); // all | outstanding | paid
  const [detail, setDetail] = useState(null);
  const [openProgram, setOpenProgram] = useState(null);
  const [openLang, setOpenLang] = useState(null);
  const [openYear, setOpenYear] = useState(null);

  useEffect(() => {
    (async () => {
      try { setData(await api.get('/api/tuition/admin/overview')); }
      catch (e) { console.error(e); }
    })();
  }, []);

  const rows = useMemo(() => {
    const all = data?.rows ?? [];
    const term = q.trim().toLowerCase();
    return all.filter((r) => {
      if (filter === 'outstanding' && Number(r.outstanding) <= 0) return false;
      if (filter === 'paid' && Number(r.outstanding) > 0) return false;
      if (!term) return true;
      return (r.name || '').toLowerCase().includes(term) || (r.code || '').toLowerCase().includes(term);
    });
  }, [data, q, filter]);

  const searching = q.trim().length > 0;

  // Group rows into Specializare -> Limbă -> An (only groups that have students).
  const tree = useMemo(() => {
    const prog = new Map();
    for (const r of rows) {
      const { program, language } = parseSpec(r.specialization);
      const lang = language || 'Fără limbă';
      const year = r.study_year ? `Anul ${r.study_year}` : 'An nespecificat';
      if (!prog.has(program)) prog.set(program, new Map());
      const langMap = prog.get(program);
      if (!langMap.has(lang)) langMap.set(lang, new Map());
      const yearMap = langMap.get(lang);
      if (!yearMap.has(year)) yearMap.set(year, []);
      yearMap.get(year).push(r);
    }
    const sortEntries = (m) => Array.from(m.entries()).sort(([a], [b]) => a.localeCompare(b, 'ro'));
    return sortEntries(prog).map(([program, langMap]) => [
      program,
      langMap.size,
      sortEntries(langMap).map(([lang, yearMap]) => [
        lang,
        Array.from(yearMap.values()).reduce((n, l) => n + l.length, 0),
        sortEntries(yearMap),
      ]),
    ]);
  }, [rows]);

  const countProgram = (langs) => langs.reduce((n, [, c]) => n + c, 0);

  const toggleProgram = (p) => {
    setOpenProgram((prev) => (prev === p ? null : p));
    setOpenLang(null);
    setOpenYear(null);
  };
  const toggleLang = (k) => { setOpenLang((prev) => (prev === k ? null : k)); setOpenYear(null); };
  const toggleYear = (k) => setOpenYear((prev) => (prev === k ? null : k));

  const studentTable = (list) => (
    <div className="table-wrap no-overflow">
      <table className="data-table">
        <thead>
          <tr>
            <th>Nume</th>
            <th className="td-num">Plătit</th>
            <th className="td-num">Rămas</th>
            <th className="col-info"></th>
          </tr>
        </thead>
        <tbody>
          {list.map((r) => (
            <tr key={r.student_id}>
              <td>{r.name}</td>
              <td className="mono td-num">{lei(r.total_paid)}</td>
              <td className="mono td-num">{amountCell(r)}</td>
              <td className="col-info">
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setDetail(r)}>
                  <Icon name="info" /> Detalii
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <div className="page taxe-page">
      <section className="header-card">
        <h1 className="page-title">Statistică taxe</h1>
        <p className="page-subtitle">Cine ce taxe are de plătit (școlarizare + restanțe) și cât a plătit până acum.</p>
      </section>

      <div className="stats-row">
        <div className="stat-card"><div className="stat-label">Studenți cu taxe</div><div className="stat-value">{data?.students ?? '—'}</div></div>
        <div className="stat-card"><div className="stat-label">Total încasat</div><div className="stat-value">{lei(data?.total_paid)}</div></div>
        <div className="stat-card"><div className="stat-label">Total restant</div><div className="stat-value">{lei(data?.total_outstanding)}</div></div>
      </div>

      <section className="card">
        <div className="card-header" style={{ gap: 12 }}>
          <label className="field" style={{ maxWidth: 320, margin: 0 }}>
            <div className="input-wrap">
              <Icon name="search" className="input-icon" />
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Caută nume sau matricol" />
            </div>
          </label>
          <div className="facility-tabs">
            {[['all', 'Toți'], ['outstanding', 'Cu restanțe de plată'], ['paid', 'Achitat integral']].map(([k, l]) => (
              <button key={k} type="button" className={`facility-tab ${filter === k ? 'active' : ''}`} onClick={() => setFilter(k)}>{l}</button>
            ))}
          </div>
        </div>

        {rows.length === 0 ? (
          <div className="card-body muted center">Niciun student.</div>
        ) : searching ? (
          /* Search results: flat list (groups would hide the matches). */
          studentTable(rows)
        ) : (
          /* Browse: Specializare → Limbă → An */
          <div className="card-body" style={{ padding: 0, gap: 0 }}>
            {tree.map(([program, , langs]) => (
              <div key={program} className={`users-accordion ${openProgram === program ? 'open' : ''}`}>
                <button type="button" className={`users-accordion-trigger${openProgram === program ? ' open' : ''}`}
                  onClick={() => toggleProgram(program)}>
                  <div className="users-accordion-left">
                    <Icon name="school" />
                    <span className="users-accordion-label">{program}</span>
                    <span className="users-accordion-count">{countProgram(langs)}</span>
                  </div>
                  <Icon name="expand_more" className={`users-accordion-chevron${openProgram === program ? ' rotated' : ''}`} />
                </button>

                {openProgram === program && (
                  <div className="users-accordion-body">
                    {langs.map(([lang, langCount, years]) => {
                      const langKey = `${program}|${lang}`;
                      return (
                        <div key={lang} className={`users-accordion sub ${openLang === langKey ? 'open' : ''}`}>
                          <button type="button" className={`users-accordion-trigger sub${openLang === langKey ? ' open' : ''}`}
                            onClick={() => toggleLang(langKey)}>
                            <div className="users-accordion-left">
                              <span className="users-accordion-label">{lang}</span>
                              <span className="users-accordion-count">{langCount}</span>
                            </div>
                            <Icon name="expand_more" className={`users-accordion-chevron${openLang === langKey ? ' rotated' : ''}`} />
                          </button>

                          {openLang === langKey && (
                            <div className="users-accordion-body">
                              {years.map(([year, list]) => {
                                const yearKey = `${langKey}|${year}`;
                                return (
                                  <div key={year} className={`users-accordion sub ${openYear === yearKey ? 'open' : ''}`}>
                                    <button type="button" className={`users-accordion-trigger sub${openYear === yearKey ? ' open' : ''}`}
                                      onClick={() => toggleYear(yearKey)}>
                                      <div className="users-accordion-left">
                                        <span className="users-accordion-label">{year}</span>
                                        <span className="users-accordion-count">{list.length}</span>
                                      </div>
                                      <Icon name="expand_more" className={`users-accordion-chevron${openYear === yearKey ? ' rotated' : ''}`} />
                                    </button>
                                    {openYear === yearKey && (
                                      <div className="users-accordion-body">{studentTable(list)}</div>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {detail && (
        <div className="modal-overlay" onClick={() => setDetail(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{detail.name}</h3>
              <button type="button" className="modal-close" onClick={() => setDetail(null)} aria-label="Închide">
                <Icon name="close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="tax-detail-stats">
                <div className="tax-detail-stat">
                  <span className="tax-detail-stat-label">Plătit</span>
                  <span className="tax-detail-stat-value">{lei(detail.total_paid)}</span>
                </div>
                <div className={`tax-detail-stat ${Number(detail.outstanding) > 0 ? 'danger' : 'ok'}`}>
                  <span className="tax-detail-stat-label">Rămas de plată</span>
                  <span className="tax-detail-stat-value">
                    {Number(detail.outstanding) > 0 ? lei(detail.outstanding) : 'Achitat integral'}
                  </span>
                </div>
              </div>

              <dl className="tax-detail-list">
                <div><dt>Matricol</dt><dd className="mono">{detail.code || '—'}</dd></div>
                <div><dt>Specializare</dt><dd>{detail.specialization || '—'}</dd></div>
                <div><dt>An</dt><dd>{detail.study_year ? `Anul ${detail.study_year}` : '—'}</dd></div>
                <div>
                  <dt>Finanțare</dt>
                  <dd><span className={`badge ${detail.fee_paying ? 'badge-optional' : ''}`}>{detail.financing || '—'}</span></dd>
                </div>
                <div>
                  <dt>Școlarizare (rate)</dt>
                  <dd>{detail.fee_paying ? `${detail.installments_paid}/${detail.installments_total} plătite` : 'Buget — fără taxă'}</dd>
                </div>
                <div>
                  <dt>Restanțe</dt>
                  <dd>{detail.restante_total > 0 ? `${detail.restante_paid}/${detail.restante_total} achitate` : 'Fără restanțe'}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
