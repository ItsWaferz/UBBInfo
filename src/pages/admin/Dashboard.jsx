import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { formatRomanianDate } from '../../utils/format';
import Icon from '../../components/Icon';
import Overview from './Overview';

// Admin landing page. The former tabs are now sidebar nav routes
// (see nav.js NAV_ITEMS_ADMIN and App.jsx /admin/* routes).
export default function AdminDashboard() {
  const { profile } = useAuth();
  const { t } = useLanguage();

  return (
    <div className="page">
      <section className="welcome-card">
        <Icon name="admin_panel_settings" className="welcome-bg-icon" />
        <h1 className="welcome-title">{t('admin.title')}</h1>
        <p className="welcome-subtitle">
          {profile?.full_name} · {formatRomanianDate()}
        </p>
      </section>

      <Overview />
    </div>
  );
}
