import { useEffect, useState } from 'react';
import { supabase } from '../../supabaseClient';
import { useAuth } from '../../contexts/AuthContext';
import { formatRomanianDate, firstNameOf } from '../../utils/format';
import PasswordModal from '../../components/PasswordModal';
import Toast from '../../components/Toast';
import Icon from '../../components/Icon';
import { useLanguage } from '../../i18n/LanguageContext';

const ACADEMIC_YEAR = '2025-2026';
const CURRENT_SEMESTER = 2;

export default function StudentDashboard() {
  const { user, profile } = useAuth();
  const { t, lang } = useLanguage();
  const [links, setLinks] = useState([]);
  const [currentCourses, setCurrentCourses] = useState([]);
  const [restante, setRestante] = useState([]);
  const [copied, setCopied] = useState(false);
  const [pwModalOpen, setPwModalOpen] = useState(false);
  const [pwToast, setPwToast] = useState(false);

  useEffect(() => {
    if (!user) return;
    let active = true;

    (async () => {
      const [linksRes, currentRes, restanteRes] = await Promise.all([
        supabase
          .from('useful_links')
          .select('*')
          .eq('is_active', true)
          .order('sort_order'),
        supabase
          .from('enrollments')
          .select('*, courses(*)')
          .eq('student_id', user.id)
          .eq('academic_year', ACADEMIC_YEAR)
          .eq('semester', CURRENT_SEMESTER),
        supabase
          .from('enrollments')
          .select('*, courses(*)')
          .eq('student_id', user.id)
          .eq('is_restanta', true),
      ]);

      if (!active) return;

      setLinks(linksRes.data || []);
      setCurrentCourses(currentRes.data || []);

      // Past restanțe: exclude current semester, only grade < 5 OR null
      const past = (restanteRes.data || []).filter((e) => {
        const isCurrent =
          e.academic_year === ACADEMIC_YEAR && e.semester === CURRENT_SEMESTER;
        const failingOrUngraded = e.grade === null || e.grade < 5;
        return !isCurrent && failingOrUngraded;
      });
      setRestante(past);
    })();

    return () => {
      active = false;
    };
  }, [user]);

  const handleCopyEmail = async () => {
    try {
      await navigator.clipboard.writeText(profile?.email || '');
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch (err) {
      console.error('Copy failed:', err);
    }
  };

  const totalCredits =
    currentCourses.reduce((acc, e) => acc + (e.courses?.credits ?? 0), 0) +
    restante.reduce((acc, e) => acc + (e.courses?.credits ?? 0), 0);

  return (
    <div className="page">
      {/* A. Welcome card */}
      <section className="welcome-card">
        <Icon name="school" className="welcome-bg-icon" />
        <h1 className="welcome-title">
          {t('dashboard.welcome', { name: firstNameOf(profile?.full_name) })}
        </h1>
        <p className="welcome-subtitle">
          {t('dashboard.yearSemester')} · {formatRomanianDate()}
        </p>
      </section>

      {/* B. Two-column grid 7:5 */}
      <div className="grid-7-5">
        {/* Account info */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="account_circle" />
              {t('dashboard.account')}
            </h2>
            <span className="badge badge-student">{t('role.student')}</span>
          </div>
          <div className="card-body">
            <div className="info-field">
              <span className="info-label">{t('dashboard.email')}</span>
              <div className="copy-row">
                <span className="info-value mono">{profile?.email}</span>
                <button
                  type="button"
                  className="copy-btn"
                  onClick={handleCopyEmail}
                  aria-label={t('common.copyEmail')}
                >
                  <Icon name={copied ? 'check' : 'content_copy'} />
                </button>
              </div>
            </div>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setPwModalOpen(true)}
            >
              <Icon name="key" />
              {t('dashboard.changePassword')}
            </button>
          </div>
        </section>

        {/* Useful links */}
        <section className="card">
          <div className="card-header">
            <h2 className="card-title">
              <Icon name="link" />
              {t('dashboard.usefulLinks')}
            </h2>
          </div>
          <div className="card-body">
            <ul className="links-list">
              {links.map((l) => {
                const urlMap = { ro: l.url, en: l.url_en, hu: l.url_hu, de: l.url_de };
                const linkUrl = (urlMap[lang] && urlMap[lang] !== '#') ? urlMap[lang] : l.url;
                const titleMap = { ro: l.title, en: l.title_en, hu: l.title_hu, de: l.title_de };
                const linkTitle = (titleMap[lang] && titleMap[lang].trim()) ? titleMap[lang] : l.title;
                return (
                  <li key={l.id}>
                    <a
                      href={linkUrl || '#'}
                      className="link-item"
                      target={linkUrl && linkUrl !== '#' ? '_blank' : undefined}
                      rel="noreferrer"
                    >
                      <Icon name={l.icon} className="link-icon" />
                      <span className="link-title">{linkTitle}</span>
                      <Icon name="arrow_forward" className="link-arrow" />
                    </a>
                  </li>
                );
              })}
              {links.length === 0 && <li className="muted">{t('dashboard.noLinks')}</li>}
            </ul>
          </div>
        </section>
      </div>

      {/* C. Student ID card */}
      <section className="id-card">
        <div className="id-card-left">
          <h3 className="id-card-title">{t('dashboard.idCard')}</h3>
          <div className="id-gold-line" />
          <div className="id-code-label">{t('dashboard.idCode')}</div>
          <div className="id-code-value mono">{profile?.student_id || '—'}</div>
        </div>
        <div className="id-card-right">
          <div className="id-grid">
            <div className="id-detail">
              <div className="id-detail-label">{t('dashboard.faculty')}</div>
              <div className="id-detail-value">{profile?.faculty || '—'}</div>
            </div>
            <div className="id-detail">
              <div className="id-detail-label">{t('dashboard.studyYear')}</div>
              <div className="id-detail-value">{profile?.study_year || '—'}</div>
            </div>
            <div className="id-detail">
              <div className="id-detail-label">{t('dashboard.specialization')}</div>
              <div className="id-detail-value">{profile?.specialization || '—'}</div>
            </div>
            <div className="id-detail">
              <div className="id-detail-label">{t('dashboard.transportId')}</div>
              <div className="id-detail-value">{profile?.transport_id || '—'}</div>
            </div>
          </div>
          <div className="id-qr">
            <Icon name="qr_code_2" />
            <span className="id-qr-note">{t('dashboard.idValid')}</span>
          </div>
        </div>
      </section>

      {/* D. Academic table */}
      <section className="card">
        <div className="card-header">
          <h2 className="card-title">
            <Icon name="table_chart" />
            {t('dashboard.academicTable')}
          </h2>
          <button type="button" className="btn btn-outline btn-sm">
            <Icon name="download" />
            {t('dashboard.downloadCert')}
          </button>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{t('table.discipline')}</th>
                <th>{t('table.credits')}</th>
                <th>{t('table.grade')}</th>
                <th className="hide-mobile">{t('table.status')}</th>
                <th>{t('table.profile')}</th>
                <th>{t('table.group')}</th>
              </tr>
            </thead>
            <tbody>
              {currentCourses.map((e) => (
                <tr key={e.id}>
                  <td>{e.courses?.name}</td>
                  <td>{e.courses?.credits}</td>
                  <td className="muted">—</td>
                  <td className="hide-mobile">
                    <span className="status-inprogress">{t('status.inProgress')}</span>
                  </td>
                  <td>{e.courses?.profile || '—'}</td>
                  <td>{e.group_name || '—'}</td>
                </tr>
              ))}

              {restante.map((e) => (
                <tr key={e.id}>
                  <td className="text-danger">
                    {e.courses?.name} <span className="restanta-tag">{t('status.restanta')}</span>
                  </td>
                  <td>{e.courses?.credits}</td>
                  <td className="muted">—</td>
                  <td className="hide-mobile">
                    <span className="status-inprogress">{t('status.inProgress')}</span>
                  </td>
                  <td>{e.courses?.profile || '—'}</td>
                  <td>{e.group_name || '—'}</td>
                </tr>
              ))}

              {currentCourses.length === 0 && restante.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted center">
                    {t('dashboard.noEnrollments')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="table-footer">
          {t('dashboard.totalCredits')}: <strong>{totalCredits}</strong>
        </div>
      </section>

      <PasswordModal
        open={pwModalOpen}
        onClose={() => setPwModalOpen(false)}
        onSuccess={() => {
          setPwToast(true);
          setTimeout(() => setPwToast(false), 3000);
        }}
      />
      <Toast
        visible={pwToast}
        variant="success"
        message={t('password.success')}
      />
    </div>
  );
}
