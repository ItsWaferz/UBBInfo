import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Icon from './Icon';
import { useLanguage, LANG_FLAGS } from '../i18n/LanguageContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { t, lang, setLang, SUPPORTED_LANGS } = useLanguage();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(false);
  const [langOpen, setLangOpen] = useState(false);
  const langRef = useRef(null);

  useEffect(() => {
    function onClick(e) {
      if (langRef.current && !langRef.current.contains(e.target)) setLangOpen(false);
    }
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setError(false);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate('/', { replace: true });
    } catch (err) {
      console.error('Login failed:', err);
      setError(true);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      {/* Left brand panel */}
      <aside className="login-brand">
        <div className="login-brand-inner">
          <Icon name="school" size={64} className="login-crest" />
          <h1 className="login-brand-title">{t('brand.university')}</h1>
          <p className="login-brand-subtitle">UBB Info — {t('brand.portal')}</p>
          <div className="login-gold-line" />
          <p className="login-tagline">{t('brand.tagline')}</p>
        </div>
      </aside>

      {/* Right form panel */}
      <main className="login-form-panel">
        <div className="login-lang-bar">
          <div className="lang-switcher" ref={langRef}>
            <button
              type="button"
              className="lang-switcher-btn"
              onClick={() => setLangOpen((o) => !o)}
              aria-label="Language"
              aria-expanded={langOpen}
            >
              <span className="lang-flag">{LANG_FLAGS[lang]}</span>
              <span>{t(`lang.${lang}`)}</span>
              <Icon name={langOpen ? 'expand_less' : 'expand_more'} size={16} />
            </button>
            {langOpen && (
              <ul className="lang-switcher-menu">
                {SUPPORTED_LANGS.map((code) => (
                  <li key={code}>
                    <button
                      type="button"
                      className={`lang-switcher-option ${code === lang ? 'active' : ''}`}
                      onClick={() => { setLang(code); setLangOpen(false); }}
                    >
                      <span className="lang-flag">{LANG_FLAGS[code]}</span>
                      <span>{t(`lang.${code}`)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        <div className="login-card">
          <h2 className="login-card-title">{t('login.title')}</h2>
          <p className="login-card-subtitle">
            {t('login.subtitle')}
          </p>

          <form onSubmit={handleSubmit} className="login-form">
            <label className="field">
              <span className="field-label">{t('login.email')}</span>
              <div className="input-wrap">
                <Icon name="mail" className="input-icon" />
                <input
                  type="email"
                  autoComplete="username"
                  placeholder={t('login.emailPlaceholder')}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </label>

            <label className="field">
              <span className="field-label">{t('login.password')}</span>
              <div className="input-wrap">
                <Icon name="lock" className="input-icon" />
                <input
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </label>

            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
              {submitting ? <span className="spinner" /> : t('login.submit')}
            </button>
          </form>
        </div>

        <div className={`login-error-toast ${error ? 'visible' : ''}`}>
          <Icon name="error" />
          <span>{t('login.error')}</span>
        </div>
      </main>
    </div>
  );
}
