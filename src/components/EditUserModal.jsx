import { useEffect, useState } from 'react';
import { supabase } from '../supabaseClient';
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

export default function EditUserModal({ open, user, onClose, onSaved }) {
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      const next = {};
      for (const f of FIELDS) next[f] = user[f] ?? '';
      setForm(next);
    }
  }, [user]);

  if (!open || !user) return null;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.full_name?.trim()) {
      setError('Numele este obligatoriu.');
      return;
    }
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
    const { error: upErr } = await supabase
      .from('profiles')
      .update(payload)
      .eq('id', user.id);
    setSaving(false);
    if (upErr) {
      console.error('Update profile failed:', upErr);
      setError('Eroare la salvare.');
      return;
    }
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
            {field('email', 'Email instituțional', { type: 'email' })}
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
            {field('financing', 'Finanțare', { select: ['', 'BUGET', 'TAXĂ'] })}
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

          <p className="muted" style={{ fontSize: 12 }}>
            Notă: schimbarea emailului aici actualizează doar profilul, nu și emailul de
            autentificare.
          </p>

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
