import { useState } from 'react';
import { supabase } from '../supabaseClient';
import { useAuth } from '../contexts/AuthContext';
import Icon from './Icon';
import { useLanguage } from '../i18n/LanguageContext';

export default function PasswordModal({ open, onClose, onSuccess }) {
  const { user, profile } = useAuth();
  const { t } = useLanguage();
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!open) return null;

  const reset = () => {
    setNewPw('');
    setConfirmPw('');
    setError('');
    setSubmitting(false);
  };

  const close = () => {
    reset();
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (newPw.length < 6) {
      setError(t('password.tooShort'));
      return;
    }
    if (newPw !== confirmPw) {
      setError(t('password.mismatch'));
      return;
    }

    setSubmitting(true);
    try {
      const { error: updateError } = await supabase.auth.updateUser({
        password: newPw,
      });
      if (updateError) throw updateError;

      // Offer the browser's password manager a save prompt
      if (window.PasswordCredential) {
        try {
          const cred = new window.PasswordCredential({
            id: user.email,
            password: newPw,
            name: profile?.full_name || user.email,
          });
          await navigator.credentials.store(cred);
        } catch (credErr) {
          console.warn('PasswordCredential store failed:', credErr);
        }
      }

      reset();
      onClose();
      onSuccess?.();
    } catch (err) {
      console.error('Password change failed:', err);
      setError(t('password.error') || 'Nu am putut schimba parola. Încearcă din nou.');
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={close}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{t('password.title')}</h3>
          <button type="button" className="modal-close" onClick={close} aria-label={t('common.close')}>
            <Icon name="close" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          {/* Hidden username for password managers */}
          <input
            type="text"
            autoComplete="username"
            value={user?.email || ''}
            readOnly
            hidden
          />

          <label className="field">
            <span className="field-label">{t('password.new')}</span>
            <div className="input-wrap">
              <Icon name="lock" className="input-icon" />
              <input
                type="password"
                autoComplete="new-password"
                minLength={6}
                value={newPw}
                onChange={(e) => setNewPw(e.target.value)}
                placeholder={t('password.minChars')}
                required
              />
            </div>
          </label>

          <label className="field">
            <span className="field-label">{t('password.confirm')}</span>
            <div className="input-wrap">
              <Icon name="lock" className="input-icon" />
              <input
                type="password"
                autoComplete="new-password"
                minLength={6}
                value={confirmPw}
                onChange={(e) => setConfirmPw(e.target.value)}
                placeholder={t('password.repeatPlaceholder')}
                required
              />
            </div>
          </label>

          {error && <p className="modal-error">{error}</p>}

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={close}>
              {t('common.cancel')}
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <span className="spinner" /> : t('password.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
