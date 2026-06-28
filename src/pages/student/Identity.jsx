import { useEffect, useState } from 'react';
import { api } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import Icon from '../../components/Icon';
import { useLanguage } from '../../i18n/LanguageContext';

const FIELDS = ['phone', 'personal_email', 'iban', 'cnp', 'id_series', 'address'];

const EMPTY = {
  phone: '',
  personal_email: '',
  iban: '',
  cnp: '',
  id_series: '',
  address: '',
};

export default function Identity() {
  const { user, profile, setProfile } = useAuth();
  const { t } = useLanguage();
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState(null); // { type: 'success'|'error', text }

  useEffect(() => {
    if (!user) return;
    let active = true;
    (async () => {
      try {
        const me = await api.get('/api/me/profile');
        if (!active) return;
        const data = me.profile || {};
        const next = { ...EMPTY };
        for (const f of FIELDS) next[f] = data[f] ?? '';
        setForm(next);
      } catch (err) {
        if (active) console.error('Load identity failed:', err);
      }
    })();
    return () => {
      active = false;
    };
  }, [user]);

  const update = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setStatus(null);

    // Empty strings -> null
    const payload = {};
    for (const f of FIELDS) {
      const v = form[f]?.trim();
      payload[f] = v === '' ? null : v;
    }

    try {
      await api.put('/api/me/profile', payload);
    } catch (err) {
      console.error('Save identity failed:', err);
      setSaving(false);
      setStatus({ type: 'error', text: t('identity.error') });
      return;
    }

    setSaving(false);

    // Keep global profile in sync
    setProfile((p) => (p ? { ...p, ...payload } : p));
    setStatus({ type: 'success', text: t('identity.success') });
    setTimeout(() => setStatus(null), 3000);
  };

  return (
    <div className="page">
      <section className="header-card">
        <h1 className="page-title">{t('identity.title')}</h1>
        <p className="page-subtitle">
          {t('identity.subtitle')}
        </p>
      </section>

      <form onSubmit={handleSubmit} className="identity-form">
        {/* 1. Contact */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="contact_phone" />
              {t('identity.contact')}
            </h2>
          </div>
          <div className="card-body form-grid-2">
            <label className="field">
              <span className="field-label">{t('identity.phone')}</span>
              <div className="input-wrap">
                <Icon name="phone" className="input-icon" />
                <input
                  type="tel"
                  value={form.phone}
                  onChange={update('phone')}
                  placeholder="07XX XXX XXX"
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">{t('identity.personalEmail')}</span>
              <div className="input-wrap">
                <Icon name="mail" className="input-icon" />
                <input
                  type="email"
                  value={form.personal_email}
                  onChange={update('personal_email')}
                  placeholder="nume@exemplu.com"
                />
              </div>
            </label>
          </div>
        </section>

        {/* 2. Financial */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="account_balance" />
              {t('identity.financial')}
            </h2>
          </div>
          <div className="card-body">
            <label className="field">
              <span className="field-label">{t('identity.iban')}</span>
              <div className="input-wrap">
                <Icon name="credit_card" className="input-icon" />
                <input
                  type="text"
                  className="mono"
                  value={form.iban}
                  onChange={update('iban')}
                  placeholder="RO00 BANK 0000 0000 0000 0000"
                />
              </div>
              <span className="field-hint">
                {t('identity.ibanHint')}
              </span>
            </label>
          </div>
        </section>

        {/* 3. Identity documents */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="badge" />
              {t('identity.documents')}
            </h2>
          </div>
          <div className="card-body form-grid-2">
            <label className="field">
              <span className="field-label">{t('identity.cnp')}</span>
              <div className="input-wrap">
                <Icon name="fingerprint" className="input-icon" />
                <input
                  type="text"
                  className="mono"
                  maxLength={13}
                  value={form.cnp}
                  onChange={update('cnp')}
                  placeholder="1234567890123"
                />
              </div>
            </label>
            <label className="field">
              <span className="field-label">{t('identity.series')}</span>
              <div className="input-wrap">
                <Icon name="badge" className="input-icon" />
                <input
                  type="text"
                  value={form.id_series}
                  onChange={update('id_series')}
                  placeholder="CJ 123456"
                />
              </div>
            </label>
          </div>
        </section>

        {/* 4. Address */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="home_pin" />
              {t('identity.address')}
            </h2>
          </div>
          <div className="card-body">
            <label className="field">
              <span className="field-label">{t('identity.fullAddress')}</span>
              <div className="input-wrap input-wrap-textarea">
                <Icon name="location_on" className="input-icon" />
                <textarea
                  rows={3}
                  value={form.address}
                  onChange={update('address')}
                  placeholder="Strada, număr, oraș, județ"
                />
              </div>
            </label>
          </div>
        </section>

        <div className="identity-footer">
          {status && (
            <span className={`status-msg status-${status.type}`}>{status.text}</span>
          )}
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? (
              <span className="spinner" />
            ) : (
              <>
                <Icon name="save" />
                {t('identity.save')}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
