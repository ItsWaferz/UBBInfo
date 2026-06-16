import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { formatRomanianDate } from '../../utils/format';
import Icon from '../../components/Icon';
import Overview from './Overview';
import Users from './Users';
import Courses from './Courses';
import Links from './Links';
import Calendar from './Calendar';
import OrarEditor from './OrarEditor';
import ConturiAdmisi from './ConturiAdmisi';
import Evaluari from './Evaluari';

const TABS = [
  { key: 'overview', labelKey: 'admin.tab.overview', icon: 'dashboard' },
  { key: 'users', labelKey: 'admin.tab.users', icon: 'group' },
  { key: 'courses', labelKey: 'admin.tab.courses', icon: 'menu_book' },
  { key: 'orar', labelKey: 'admin.tab.orar', icon: 'calendar_view_week' },
  { key: 'calendar', labelKey: 'admin.tab.calendar', icon: 'calendar_month' },
  { key: 'evaluari', labelKey: 'admin.tab.evaluari', icon: 'reviews' },
  { key: 'links', labelKey: 'admin.tab.links', icon: 'link' },
  { key: 'admisi', labelKey: 'admin.tab.admisi', icon: 'group_add' },
];

export default function AdminDashboard() {
  const { profile } = useAuth();
  const { t } = useLanguage();
  const [tab, setTab] = useState('overview');

  return (
    <div className="page">
      <section className="welcome-card">
        <Icon name="admin_panel_settings" className="welcome-bg-icon" />
        <h1 className="welcome-title">{t('admin.title')}</h1>
        <p className="welcome-subtitle">
          {profile?.full_name} · {formatRomanianDate()}
        </p>
      </section>

      <div className="admin-tabs">
        {TABS.map((tabItem) => (
          <button
            key={tabItem.key}
            type="button"
            className={`admin-tab ${tab === tabItem.key ? 'active' : ''}`}
            onClick={() => setTab(tabItem.key)}
          >
            <Icon name={tabItem.icon} />
            {t(tabItem.labelKey)}
          </button>
        ))}
      </div>

      {tab === 'overview' && <Overview />}
      {tab === 'users' && <Users />}
      {tab === 'courses' && <Courses />}
      {tab === 'orar' && <OrarEditor />}
      {tab === 'calendar' && <Calendar />}
      {tab === 'evaluari' && <Evaluari />}
      {tab === 'links' && <Links />}
      {tab === 'admisi' && <ConturiAdmisi />}
    </div>
  );
}
