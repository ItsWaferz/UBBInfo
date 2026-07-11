import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Icon from '../../components/Icon';
import { useLanguage } from '../../i18n/LanguageContext';

export default function Documente() {
  const { user } = useAuth();
  const { t } = useLanguage();

  const [types, setTypes] = useState([]);
  const [history, setHistory] = useState([]);
  const [active, setActive] = useState(null); // type key being filled
  const [title, setTitle] = useState('');
  const [fields, setFields] = useState([]); // field schema
  const [values, setValues] = useState({}); // key -> value
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [status, setStatus] = useState(null); // { type, text }

  const loadHistory = async () => {
    try {
      const h = await api.get('/api/documents/history');
      setHistory(h || []);
    } catch (err) {
      console.error('Load history failed:', err);
    }
  };

  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      try {
        const ts = await api.get('/api/documents/types');
        if (active) setTypes(ts || []);
      } catch (err) {
        console.error('Load document types failed:', err);
      }
      if (active) loadHistory();
    })();
    return () => {
      active = false;
    };
  }, [user]);

  const openForm = async (type) => {
    setActive(type.key);
    setTitle(type.title);
    setLoading(true);
    setStatus(null);
    try {
      const data = await api.get(`/api/documents/${type.key}/prefill`);
      setFields(data.fields || []);
      const initial = {};
      (data.fields || []).forEach((f) => {
        initial[f.key] = f.value ?? '';
      });
      setValues(initial);
    } catch (err) {
      console.error('Prefill failed:', err);
      setStatus({ type: 'error', text: 'Nu am putut încărca formularul.' });
    } finally {
      setLoading(false);
    }
  };

  const closeForm = () => {
    setActive(null);
    setFields([]);
    setValues({});
    setStatus(null);
  };

  const update = (key) => (e) =>
    setValues((prev) => ({ ...prev, [key]: e.target.value }));

  const generate = async () => {
    setGenerating(true);
    setStatus(null);
    try {
      await api.download(`/api/documents/${active}/generate`, {
        method: 'POST',
        body: values,
      });
      setStatus({ type: 'success', text: 'Document generat. Verifică descărcările.' });
      loadHistory();
    } catch (err) {
      console.error('Generate failed:', err);
      setStatus({ type: 'error', text: 'Generarea a eșuat. Încearcă din nou.' });
    } finally {
      setGenerating(false);
    }
  };

  const reDownload = async (id) => {
    try {
      await api.download(`/api/documents/${id}/download`);
    } catch (err) {
      console.error('Re-download failed:', err);
    }
  };

  // Group fields by section, preserving order.
  const sections = useMemo(() => {
    const map = new Map();
    for (const f of fields) {
      const s = f.section || '';
      if (!map.has(s)) map.set(s, []);
      map.get(s).push(f);
    }
    return Array.from(map.entries());
  }, [fields]);

  const typeIcon = (key) => types.find((t) => t.key === key)?.icon || 'description';

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">{t('nav.documents')}</h1>
        <p className="page-subtitle">
          Generează documente oficiale pre-completate cu datele tale. Verifică-le, completează ce
          lipsește, apoi descarcă PDF-ul.
        </p>
      </section>

      {!active && (
        <div className="doc-type-grid">
          {types.map((tp) => (
            <button key={tp.key} type="button" className="doc-type-card" onClick={() => openForm(tp)}>
              <div className="doc-type-icon">
                <Icon name={tp.icon} />
              </div>
              <div className="doc-type-body">
                <h3>{tp.title}</h3>
                <p>{tp.description}</p>
              </div>
              <Icon name="chevron_right" className="doc-type-arrow" />
            </button>
          ))}
        </div>
      )}

      {active && (
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name={typeIcon(active)} />
              {title}
            </h2>
            <button type="button" className="btn btn-ghost" onClick={closeForm}>
              <Icon name="arrow_back" /> Înapoi
            </button>
          </div>

          {loading ? (
            <div className="card-body center muted">
              <span className="spinner" />
            </div>
          ) : (
            <div className="card-body">
              {sections.map(([section, secFields]) => (
                <div key={section} className="doc-section">
                  {section && <div className="doc-section-title">{section}</div>}
                  <div className="form-grid-2">
                    {secFields.map((f) => (
                      <label
                        key={f.key}
                        className="field"
                        style={f.full || f.type === 'textarea' ? { gridColumn: '1 / -1' } : undefined}
                      >
                        <span className="field-label">{f.label}</span>
                        {f.type === 'select' ? (
                          <select value={values[f.key] ?? ''} onChange={update(f.key)}>
                            {(f.options || []).map((opt) => (
                              <option key={opt} value={opt}>
                                {opt}
                              </option>
                            ))}
                          </select>
                        ) : f.type === 'textarea' ? (
                          <textarea rows={2} value={values[f.key] ?? ''} onChange={update(f.key)} />
                        ) : (
                          <input
                            type={f.type === 'date' ? 'date' : 'text'}
                            value={values[f.key] ?? ''}
                            onChange={update(f.key)}
                          />
                        )}
                        {f.hint && <span className="field-hint">{f.hint}</span>}
                      </label>
                    ))}
                  </div>
                </div>
              ))}

              <div className="identity-footer">
                {status && (
                  <span className={`status-msg status-${status.type}`}>{status.text}</span>
                )}
                <button type="button" className="btn btn-primary" onClick={generate} disabled={generating}>
                  {generating ? <span className="spinner" /> : (<><Icon name="picture_as_pdf" /> Generează PDF</>)}
                </button>
              </div>
            </div>
          )}
        </section>
      )}

      {/* History */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="history" />
            Documente generate
          </h2>
        </div>
        {history.length === 0 ? (
          <div className="card-body muted center">Niciun document generat încă.</div>
        ) : (
          <div className="table-wrap">
            <table className="data-table docs-table">
              <thead>
                <tr>
                  <th>Document</th>
                  <th>An / Sem.</th>
                  <th>Data</th>
                  <th style={{ width: 56 }}></th>
                </tr>
              </thead>
              <tbody>
                {history.map((d) => (
                  <tr key={d.id}>
                    <td>{d.title}</td>
                    <td>
                      {d.academic_year || '—'}
                      {d.semester ? ` / ${d.semester === 1 ? 'I' : 'II'}` : ''}
                    </td>
                    <td>{d.created_at ? new Date(d.created_at).toLocaleDateString('ro-RO') : '—'}</td>
                    <td className="col-download">
                      <button
                        type="button"
                        className="btn btn-ghost btn-icon"
                        onClick={() => reDownload(d.id)}
                        aria-label="Descarcă"
                        title="Descarcă"
                      >
                        <Icon name="download" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
