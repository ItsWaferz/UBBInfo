import { useLanguage } from '../../i18n/LanguageContext';
import MockupPage from '../../components/MockupPage';

export default function PlataTaxe() {
  const { t } = useLanguage();

  return (
    <MockupPage
      icon="payments"
      title={t('mockup.taxe.title')}
      description={t('mockup.taxe.desc')}
      features={[
        { icon: 'school', label: t('mockup.taxe.f1') },
        { icon: 'event_busy', label: t('mockup.taxe.f2') },
        { icon: 'credit_card', label: t('mockup.taxe.f3') },
        { icon: 'receipt_long', label: t('mockup.taxe.f4') },
      ]}
    />
  );
}
