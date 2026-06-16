import { useState, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { breadcrumbForPath } from '../nav';
import Icon from './Icon';
import { useLanguage, LANG_FLAGS } from '../i18n/LanguageContext';

export default function Topbar({ onMenuClick }) {
  const { profile, activeRole } = useAuth();
  const { t, lang, setLang, SUPPORTED_LANGS } = useLanguage();
  const location = useLocation();
  const crumbKey = breadcrumbForPath(location.pathname);

  const [langOpen, setLangOpen] = useState(false);
  const langRef = useRef(null);

  useEffect(() => {
    function onClick(e) {
      if (langRef.current && !langRef.current.contains(e.target)) setLangOpen(false);
    }
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="topbar-menu-btn"
          onClick={onMenuClick}
          aria-label="Menu"
        >
          <Icon name="menu" />
        </button>
        <nav className="breadcrumb">
          <span className="breadcrumb-root">UBB Info</span>
          <Icon name="chevron_right" size={16} className="breadcrumb-sep" />
          <span className="breadcrumb-current">{t(crumbKey)}</span>
        </nav>
      </div>

      <div className="topbar-right">
        <div className="topbar-search">
          <Icon name="search" />
          <input type="text" placeholder={t('topbar.search')} />
        </div>

        {/* Language switcher */}
        <div className="lang-switcher" ref={langRef}>
          <button
            type="button"
            className="lang-switcher-btn"
            onClick={() => setLangOpen((o) => !o)}
            aria-label="Language"
            aria-expanded={langOpen}
          >
            <span className="lang-flag">{LANG_FLAGS[lang]}</span>
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

        <button type="button" className="topbar-bell" aria-label="Notifications">
          <Icon name="notifications" />
          <span className="topbar-bell-dot" />
        </button>

        <div className="topbar-divider" />

        <div className="topbar-user">
          <div className="topbar-user-text">
            <span className="topbar-user-name">{profile?.short_name || profile?.full_name}</span>
            {activeRole && (
              <span className={`badge ${activeRole.badge_class}`}>{t(`role.${activeRole.name}`)}</span>
            )}
          </div>
          <div className="avatar avatar-sm">{profile?.initials || '—'}</div>
        </div>
      </div>
    </header>
  );
}
