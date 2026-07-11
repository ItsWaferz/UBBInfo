import { useEffect, useState } from 'react';
import { api } from '../api';
import Icon from './Icon';

// Editable profile fields (admin can change anything on the profile).
const FIELDS = [
  'full_name', 'email', 'student_id', 'faculty', 'specialization',
  'study_year', 'group_name', 'financing', 'transport_id',
  'academic_rank', 'honorifics', 'phone', 'personal_email', 'address',
];

function deriveNames(fullName = '') {
  const t = fullName.trim().split(/\s+/).filter(Boolean);
  if (!t.length) return { short_name: fullName, initials: '' };
  if (t.length === 1) return { short_name: t[0], initials: t[0][0].toUpperCase() };
  return {
    short_name: `${t[0][0]}. ${t.slice(1).join(' ')}`,
    initials: (t[0][0] + t[1][0]).toUpperCase(),
  };
}

export default function EditUserModal({ open, user, roles = [], onClose, onSaved }) {
  const [form, setForm] = useState({});
  const [roleIds, setRoleIds] = useState(new Set());
  const [primaryRoleId, setPrimaryRoleId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      const next = {};
      for (const f of FIELDS) next[f] = user[f] ?? '';
      setForm(next);
      setRoleIds(new Set(user.roleIds || []));
      setPrimaryRoleId(user.primaryRoleId || null);
    }
  }, [user]);

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
    // keep derived display fields in sync with full_name
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

  const field = (key, label, opts = {}) => (
    <label className="field">
      <span className="field-label">{label}</span>
      <div className="input-wrap">
        {opts.select ? (
          <select className="select-bare" value={form[key] || ''} onChange={set(key)}>
            {opts.select.map((o) => (
              <option key={o} value={o}>
                {o || '—'}
              </option>
            ))}
          </select>
        ) : (
          <input
            type={opts.type || 'text'}
            className={opts.mono ? 'mono' : ''}
            value={form[key] || ''}
            onChange={set(key)}
          />
        )}
      </div>
    </label>
  );

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
            {field('full_name', 'Nume complet')}
            <label className="field">
              <span className="field-label">Email instituțional</span>
              <div className="input-wrap input-readonly">
                <span className="mono">{form.email || '—'}</span>
              </div>
            </label>
          </div>
          <div className="form-grid-2">
            {field('student_id', 'Nr. matricol', { mono: true })}
            {field('study_year', 'An studiu')}
          </div>
          <div className="form-grid-2">
            {field('faculty', 'Facultate')}
            {field('specialization', 'Specializare')}
          </div>
          <div className="form-grid-2">
            {field('group_name', 'Grupa')}
            {field('financing', 'Finanțare', { select: ['', 'buget', 'taxă'] })}
          </div>
          <div className="form-grid-2">
            {field('academic_rank', 'Grad didactic', {
              select: ['', 'Doctorand', 'Asistent', 'Lector', 'Conferențiar', 'Profesor'],
            })}
            {field('honorifics', 'Titluri onorifice')}
          </div>
          <div className="form-grid-2">
            {field('phone', 'Telefon', { type: 'tel' })}
            {field('personal_email', 'Email personal', { type: 'email' })}
          </div>
          {field('address', 'Adresă')}

          <div className="field">
            <span className="field-label">Roluri</span>
            <div className="course-multiselect">
              {roles.map((r) => {
                const checked = roleIds.has(r.id);
                return (
                  <div key={r.id} className="role-assign-row" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                    <label className="course-check">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleRole(r.id)}
                      />
                      <span>{r.label || r.name}</span>
                    </label>
                    {checked && (
                      <label className="course-check" style={{ fontSize: 12, opacity: 0.85 }}>
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
