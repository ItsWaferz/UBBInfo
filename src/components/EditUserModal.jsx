import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import Icon from './Icon';

// Editable profile fields (admin can change anything on the profile).
const FIELDS = [
  'full_name', 'email', 'student_id', 'faculty', 'specialization',
  'study_year', 'group_name', 'financing', 'transport_id',
  'academic_rank', 'honorifics', 'phone', 'personal_email', 'address',
];

const FINANCING = ['buget', 'taxă'];
const ACADEMIC_RANKS = ['Doctorand', 'Asistent', 'Lector', 'Conferențiar', 'Profesor'];

function deriveNames(fullName = '') {
  const t = fullName.trim().split(/\s+/).filter(Boolean);
  if (!t.length) return { short_name: fullName, initials: '' };
  if (t.length === 1) return { short_name: t[0], initials: t[0][0].toUpperCase() };
  return {
    short_name: `${t[0][0]}. ${t.slice(1).join(' ')}`,
    initials: (t[0][0] + t[1][0]).toUpperCase(),
  };
}

/** Split "Program (Limba X)" -> { program, language }. */
function parseSpec(s) {
  const str = s || '';
  const m = str.match(/^(.*?)\s*\(\s*limba\s+([^)]+?)\s*\)\s*$/i);
  if (m) return { program: m[1].trim(), language: m[2].trim() };
  return { program: str.trim(), language: '' };
}
const composeSpec = (program, language) =>
  program ? (language ? `${program} (Limba ${language})` : program) : '';

const uniq = (arr) => Array.from(new Set(arr.filter(Boolean)));
/** Diacritic-insensitive key, so imported ș/ş/ț/ţ variants still match the reference table. */
const norm = (s) => (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

export default function EditUserModal({ open, user, roles = [], onClose, onSaved }) {
  const [form, setForm] = useState({});
  const [roleIds, setRoleIds] = useState(new Set());
  const [primaryRoleId, setPrimaryRoleId] = useState(null);
  const [specs, setSpecs] = useState([]);   // [{code,name,language,faculty}]
  const [groups, setGroups] = useState([]); // [{code,spec_code,year,semigroups[]}]
  const [program, setProgram] = useState('');
  const [language, setLanguage] = useState('');
  const [groupCode, setGroupCode] = useState('');
  const [semigroup, setSemigroup] = useState('');
  const [socialCase, setSocialCase] = useState(false);
  const [specialCase, setSpecialCase] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      const next = {};
      for (const f of FIELDS) next[f] = user[f] ?? '';
      setForm(next);
      setRoleIds(new Set(user.roleIds || []));
      setPrimaryRoleId(user.primaryRoleId || null);
      const p = parseSpec(user.specialization);
      setProgram(p.program);
      setLanguage(p.language);
      const [g, s] = String(user.group_name || '').split('/');
      setGroupCode((g || '').trim());
      setSemigroup((s || '').trim());
      setSocialCase(!!user.is_social_case);
      setSpecialCase(!!user.is_special_case);
    }
  }, [user]);

  useEffect(() => {
    if (!open) return;
    api.get('/api/specializations').then((d) => setSpecs(d || [])).catch(() => {});
    api.get('/api/groups').then((d) => setGroups(d || [])).catch(() => {});
  }, [open]);

  // Canonicalize the imported specialization/language/faculty to the reference
  // table values (diacritic-insensitive) once specs load, so the dropdowns match
  // exactly downstream and none end up empty.
  useEffect(() => {
    if (!specs.length || !program) return;
    const match = specs.find((s) =>
      norm(s.name) === norm(program) && (!language || norm(s.language) === norm(language)));
    if (!match) return;
    if (program !== match.name) setProgram(match.name);
    if (language && language !== match.language) setLanguage(match.language);
    if (form.faculty !== match.faculty) setForm((f) => ({ ...f, faculty: match.faculty }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [specs, program, language, form.faculty]);

  const roleNameById = useMemo(
    () => Object.fromEntries(roles.map((r) => [r.id, r.name])),
    [roles]
  );
  const hasRole = (name) => [...roleIds].some((id) => roleNameById[id] === name);
  const isStudent = hasRole('student');
  const isProfesor = hasRole('profesor');

  // Reference faculties; fall back to the student's own value if the
  // specializations endpoint returned nothing (so the field is never empty).
  const faculties = useMemo(() => {
    const fromSpecs = uniq(specs.map((s) => s.faculty));
    return (fromSpecs.length ? fromSpecs : uniq([form.faculty])).sort();
  }, [specs, form.faculty]);
  const programs = useMemo(
    () => uniq([...specs.filter((s) => s.faculty === form.faculty).map((s) => s.name), program]).sort(),
    [specs, form.faculty, program]
  );
  const languages = useMemo(
    () => uniq([
      ...specs.filter((s) => s.faculty === form.faculty && s.name === program).map((s) => s.language),
      language,
    ]).sort(),
    [specs, form.faculty, program, language]
  );
  const selectedSpec = useMemo(
    () => specs.find((s) => s.faculty === form.faculty && s.name === program && s.language === language) || null,
    [specs, form.faculty, program, language]
  );

  // Years available for the chosen program: 1..duration (4 for Ing. Inf. / Mate-Info 4 ani,
  // 3 otherwise). Keep the current value even if it falls outside, so nothing is lost.
  const yearOptions = useMemo(() => {
    const max = selectedSpec?.duration_years || 4;
    const arr = [];
    for (let i = 1; i <= max; i += 1) arr.push(String(i));
    if (form.study_year) arr.push(String(form.study_year));
    return uniq(arr).sort();
  }, [selectedSpec, form.study_year]);

  const groupOptions = useMemo(() => {
    const codes = groups
      .filter((g) => (!selectedSpec || g.spec_code === selectedSpec.code)
        && (!form.study_year || String(g.year) === String(form.study_year)))
      .map((g) => g.code);
    if (groupCode) codes.push(groupCode);
    return uniq(codes).sort((a, b) => a.localeCompare(b));
  }, [groups, selectedSpec, form.study_year, groupCode]);

  const semigroupOptions = useMemo(() => {
    const g = groups.find((x) => x.code === groupCode);
    const opts = g ? [...(g.semigroups || [])] : [];
    if (semigroup) opts.push(semigroup);
    return uniq(opts).sort();
  }, [groups, groupCode, semigroup]);

  if (!open || !user) return null;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const toggleRole = (id) =>
    setRoleIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
        if (primaryRoleId === id) setPrimaryRoleId(null);
      } else {
        next.add(id);
        if (!primaryRoleId) setPrimaryRoleId(id);
      }
      return next;
    });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.full_name?.trim()) {
      setError('Numele este obligatoriu.');
      return;
    }
    if (roleIds.size === 0) {
      setError('Selectează cel puțin un rol.');
      return;
    }
    const primary = primaryRoleId && roleIds.has(primaryRoleId)
      ? primaryRoleId
      : [...roleIds][0];

    const payload = {};
    for (const f of FIELDS) {
      const v = typeof form[f] === 'string' ? form[f].trim() : form[f];
      payload[f] = v === '' ? null : v;
    }
    if (isStudent) {
      payload.specialization = composeSpec(program, language) || null;
      payload.faculty = form.faculty?.trim() || null;
      payload.group_name = groupCode
        ? (semigroup ? `${groupCode}/${semigroup}` : groupCode)
        : null;
      payload.is_social_case = String(socialCase);
      payload.is_special_case = String(specialCase);
    }
    const { short_name, initials } = deriveNames(form.full_name);
    payload.short_name = short_name;
    payload.initials = initials;

    setSaving(true);
    try {
      await api.put(`/api/users/${user.id}`, payload);
      await api.put(`/api/users/${user.id}/roles`, {
        role_ids: [...roleIds],
        primary_role_id: primary,
      });
    } catch (err) {
      console.error('Update user failed:', err);
      setSaving(false);
      setError('Eroare la salvare.');
      return;
    }
    setSaving(false);
    onSaved?.();
    onClose();
  };

  // ---- field renderers ----
  const input = (key, label, opts = {}) => (
    <label className="field">
      <span className="field-label">{label}</span>
      <div className="input-wrap">
        <input
          type={opts.type || 'text'}
          className={opts.mono ? 'mono' : ''}
          value={form[key] || ''}
          onChange={set(key)}
        />
      </div>
    </label>
  );

  const selectField = (label, value, onChange, options, placeholder = '— alege —') => (
    <label className="field">
      <span className="field-label">{label}</span>
      <div className="input-wrap">
        <select className="select-bare" value={value || ''} onChange={onChange}>
          <option value="">{placeholder}</option>
          {options.map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      </div>
    </label>
  );

  const readonly = (label, value, opts = {}) => (
    <label className="field">
      <span className="field-label">{label}</span>
      <div className="input-wrap input-readonly">
        <span className={opts.mono ? 'mono ro-value' : 'ro-value'}>{value || '—'}</span>
      </div>
    </label>
  );

  // Cascade change handlers (each reset narrows the ones below it).
  const onFaculty = (e) => { setForm((f) => ({ ...f, faculty: e.target.value, study_year: '' })); setProgram(''); setLanguage(''); setGroupCode(''); setSemigroup(''); };
  const onProgram = (e) => { setProgram(e.target.value); setLanguage(''); setGroupCode(''); setSemigroup(''); };
  const onLanguage = (e) => { setLanguage(e.target.value); setGroupCode(''); setSemigroup(''); };
  const onYear = (e) => { setForm((f) => ({ ...f, study_year: e.target.value })); setGroupCode(''); setSemigroup(''); };
  const onGroup = (e) => { setGroupCode(e.target.value); setSemigroup(''); };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-lg" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Editează utilizator</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Închide">
            <Icon name="close" />
          </button>
        </div>

        <form onSubmit={submit} className="modal-body">
          <div className="form-grid-2">
            {input('full_name', 'Nume complet')}
            {readonly('Email instituțional', form.email, { mono: true })}
          </div>

          {isStudent && (
            <>
              <div className="form-grid-2">
                {readonly('Nr. matricol', form.student_id, { mono: true })}
                {selectField('Facultate', form.faculty, onFaculty, faculties)}
              </div>
              <div className="form-grid-2">
                {selectField('Specializare', program, onProgram, programs)}
                {selectField('Limba de studiu', language, onLanguage, languages)}
              </div>
              <div className="form-grid-2">
                {selectField('Anul', form.study_year, onYear, yearOptions)}
                {selectField('Grupă', groupCode, onGroup, groupOptions)}
              </div>
              <div className="form-grid-2">
                {selectField('Semigrupă', semigroup, (e) => setSemigroup(e.target.value),
                  semigroupOptions, semigroupOptions.length ? '— alege —' : 'fără')}
                {selectField('Finanțare', form.financing, set('financing'), FINANCING)}
              </div>
              <div className="field">
                <span className="field-label">Cazuri speciale</span>
                <div className="case-checks">
                  <label className="course-check">
                    <input type="checkbox" checked={socialCase} onChange={(e) => setSocialCase(e.target.checked)} />
                    <span>Caz social</span>
                  </label>
                  <label className="course-check">
                    <input type="checkbox" checked={specialCase} onChange={(e) => setSpecialCase(e.target.checked)} />
                    <span>Caz special</span>
                  </label>
                </div>
              </div>
            </>
          )}

          {isProfesor && (
            <div className="form-grid-2">
              {selectField('Grad didactic', form.academic_rank, set('academic_rank'), ACADEMIC_RANKS)}
              {input('honorifics', 'Titluri onorifice')}
            </div>
          )}

          <div className="form-grid-2">
            {input('phone', 'Telefon', { type: 'tel' })}
            {input('personal_email', 'Email personal', { type: 'email' })}
          </div>
          {input('address', 'Adresă')}

          <div className="field">
            <span className="field-label">Roluri</span>
            <div className="roles-grid">
              {roles.map((r) => {
                const checked = roleIds.has(r.id);
                return (
                  <div key={r.id} className={`role-cell${checked ? ' checked' : ''}`}>
                    <label className="course-check">
                      <input type="checkbox" checked={checked} onChange={() => toggleRole(r.id)} />
                      <span>{r.label || r.name}</span>
                    </label>
                    {checked && (
                      <label className="course-check role-primary">
                        <input
                          type="radio"
                          name="primaryRole"
                          checked={primaryRoleId === r.id}
                          onChange={() => setPrimaryRoleId(r.id)}
                        />
                        <span>Principal</span>
                      </label>
                    )}
                  </div>
                );
              })}
              {roles.length === 0 && <span className="muted">Se încarcă rolurile…</span>}
            </div>
          </div>

          {error && <p className="modal-error">{error}</p>}

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Anulează
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? <span className="spinner" /> : 'Salvează'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
