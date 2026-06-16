import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { navItemsForRole } from '../nav';
import RoleSwitcher from './RoleSwitcher';
import Icon from './Icon';
import { useLanguage } from '../i18n/LanguageContext';

export default function Sidebar({ open, onClose }) {
  const { profile, currentRole, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const items = navItemsForRole(currentRole);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <>
      <div
        className={`sidebar-overlay ${open ? 'visible' : ''}`}
        onClick={onClose}
      />
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <Icon name="school" className="sidebar-brand-icon" />
          <div>
            <div className="sidebar-brand-title">UBB Info</div>
            <div className="sidebar-brand-subtitle">{t('brand.portal')}</div>
          </div>
        </div>

        <nav className="sidebar-nav">
          {items.map((item) => (
            <NavLink
              key={item.key}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? 'active' : ''}`
              }
              onClick={onClose}
            >
              <Icon name={item.icon} />
              <span>{t(item.labelKey)}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-user">
          <div className="sidebar-user-row">
            <div className="avatar">{profile?.initials || '—'}</div>
            <div className="sidebar-user-info">
              <div className="sidebar-user-name">{profile?.short_name || profile?.full_name}</div>
              <div className="sidebar-user-id">
                {profile?.student_id || profile?.email}
              </div>
            </div>
          </div>

          <RoleSwitcher />

          <button type="button" className="sidebar-action">
            <Icon name="settings" />
            <span>{t('nav.settings')}</span>
          </button>

          <button
            type="button"
            className="sidebar-action sidebar-action-danger"
            onClick={handleLogout}
          >
            <Icon name="logout" />
            <span>{t('nav.logout')}</span>
          </button>
        </div>
      </aside>
    </>
  );
}
