import { createContext, useContext, useState, useCallback } from 'react';
import translations from './translations';

const SUPPORTED_LANGS = ['ro', 'en', 'hu', 'de'];
const STORAGE_KEY = 'ubb-info-lang';

function getInitialLang() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && SUPPORTED_LANGS.includes(stored)) return stored;
  } catch (_) {/* ignore */}
  return 'ro';
}

const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(getInitialLang);

  const setLang = useCallback((code) => {
    if (SUPPORTED_LANGS.includes(code)) {
      setLangState(code);
      try { localStorage.setItem(STORAGE_KEY, code); } catch (_) {/* ignore */}
    }
  }, []);

  /**
   * Translate a key, optionally interpolating placeholders like {name}.
   * Example: t('dashboard.welcome', { name: 'Corneliu' })
   */
  const t = useCallback((key, params) => {
    const entry = translations[key];
    if (!entry) return key; // fallback to key itself
    let text = entry[lang] ?? entry.ro ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        text = text.replace(`{${k}}`, v);
      }
    }
    return text;
  }, [lang]);

  return (
    <LanguageContext.Provider value={{ lang, setLang, t, SUPPORTED_LANGS }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider');
  return ctx;
}

/** Flag emoji for each language */
export const LANG_FLAGS = { ro: '🇷🇴', en: '🇬🇧', hu: '🇭🇺', de: '🇩🇪' };
