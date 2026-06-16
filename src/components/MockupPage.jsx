// Reusable "page under construction" mockup.
// features: [{ icon, label }]
import Icon from './Icon';
import { useLanguage } from '../i18n/LanguageContext';

export default function MockupPage({ icon, title, description, features = [] }) {
  const { t } = useLanguage();

  return (
    <div className="mockup-page">
      <div className="mockup-card">
        <div className="mockup-icon-circle">
          <Icon name={icon} />
        </div>
        <h2 className="mockup-title">{title}</h2>
        <p className="mockup-description">{description}</p>

        <div className="mockup-features">
          {features.map((f) => (
            <div className="mockup-feature" key={f.label}>
              <Icon name={f.icon} />
              <span>{f.label}</span>
            </div>
          ))}
        </div>

        <div className="mockup-badge">
          <Icon name="construction" />
          {t('mockup.construction')}
        </div>
      </div>
    </div>
  );
}
