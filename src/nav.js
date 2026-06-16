// Navigation model. Page labelKeys drive i18n lookups in the sidebar / topbar.

export const NAV_ITEMS_STUDENT = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
  { to: '/identitate', key: 'identitate', labelKey: 'nav.identity', icon: 'badge' },
  { to: '/note', key: 'note', labelKey: 'nav.grades', icon: 'bar_chart' },
  { to: '/orar', key: 'orar', labelKey: 'nav.schedule', icon: 'calendar_month' },
  { to: '/evaluare', key: 'evaluare', labelKey: 'nav.evaluare', icon: 'rate_review' },
  { to: '/examen', key: 'examen', labelKey: 'nav.examen', icon: 'event_available' },
  { to: '/taxe', key: 'taxe', labelKey: 'nav.taxe', icon: 'payments' },
];

export const NAV_ITEMS_PROFESSOR = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
  { to: '/catalog', key: 'catalog', labelKey: 'nav.catalog', icon: 'edit_note' },
  { to: '/examene', key: 'examene', labelKey: 'nav.examene', icon: 'event' },
];

export const NAV_ITEMS_OTHER = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
];

export function navItemsForRole(roleName) {
  if (roleName === 'student') return NAV_ITEMS_STUDENT;
  if (roleName === 'profesor') return NAV_ITEMS_PROFESSOR;
  return NAV_ITEMS_OTHER;
}

// Map a pathname to a breadcrumb labelKey (search across all role navs)
const ALL_ITEMS = [
  ...NAV_ITEMS_STUDENT,
  ...NAV_ITEMS_PROFESSOR,
];

export function breadcrumbForPath(pathname) {
  const match = ALL_ITEMS.filter((i) => !i.end).find((i) =>
    pathname.startsWith(i.to)
  );
  return match ? match.labelKey : 'nav.home';
}
