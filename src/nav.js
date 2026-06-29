// Navigation model. Page labelKeys drive i18n lookups in the sidebar / topbar.

export const NAV_ITEMS_STUDENT = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
  { to: '/identitate', key: 'identitate', labelKey: 'nav.identity', icon: 'badge' },
  { to: '/note', key: 'note', labelKey: 'nav.grades', icon: 'bar_chart' },
  { to: '/orar', key: 'orar', labelKey: 'nav.schedule', icon: 'calendar_month' },
  { to: '/documente', key: 'documente', labelKey: 'nav.documents', icon: 'description' },
  { to: '/evaluare', key: 'evaluare', labelKey: 'nav.evaluare', icon: 'rate_review' },
  { to: '/examen', key: 'examen', labelKey: 'nav.examen', icon: 'event_available' },
  { to: '/taxe', key: 'taxe', labelKey: 'nav.taxe', icon: 'payments' },
];

export const NAV_ITEMS_PROFESSOR = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
  { to: '/catalog', key: 'catalog', labelKey: 'nav.catalog', icon: 'edit_note' },
  { to: '/notare', key: 'notare', labelKey: 'nav.grading', icon: 'functions' },
  { to: '/examene', key: 'examene', labelKey: 'nav.examene', icon: 'event' },
  { to: '/disponibilitate', key: 'disponibilitate', labelKey: 'nav.availability', icon: 'event_available' },
];

export const NAV_ITEMS_ADMIN = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'dashboard', end: true },
  { to: '/admin/utilizatori', key: 'admin-users', labelKey: 'admin.tab.users', icon: 'group' },
  { to: '/admin/discipline', key: 'admin-courses', labelKey: 'admin.tab.courses', icon: 'menu_book' },
  { to: '/admin/orar', key: 'admin-orar', labelKey: 'admin.tab.orar', icon: 'calendar_view_week' },
  { to: '/admin/sali', key: 'admin-rooms', labelKey: 'admin.tab.rooms', icon: 'apartment' },
  { to: '/admin/generare', key: 'admin-generator', labelKey: 'admin.tab.generator', icon: 'auto_awesome' },
  { to: '/admin/calendar', key: 'admin-calendar', labelKey: 'admin.tab.calendar', icon: 'calendar_month' },
  { to: '/admin/evaluari', key: 'admin-evaluari', labelKey: 'admin.tab.evaluari', icon: 'reviews' },
  { to: '/admin/linkuri', key: 'admin-links', labelKey: 'admin.tab.links', icon: 'link' },
  { to: '/admin/admisi', key: 'admin-admisi', labelKey: 'admin.tab.admisi', icon: 'group_add' },
];

export const NAV_ITEMS_OTHER = [
  { to: '/', key: 'acasa', labelKey: 'nav.home', icon: 'home', end: true },
];

export function navItemsForRole(roleName) {
  if (roleName === 'student') return NAV_ITEMS_STUDENT;
  if (roleName === 'profesor') return NAV_ITEMS_PROFESSOR;
  if (roleName === 'administrator') return NAV_ITEMS_ADMIN;
  return NAV_ITEMS_OTHER;
}

// Map a pathname to a breadcrumb labelKey (search across all role navs)
const ALL_ITEMS = [
  ...NAV_ITEMS_STUDENT,
  ...NAV_ITEMS_PROFESSOR,
  ...NAV_ITEMS_ADMIN,
];

export function breadcrumbForPath(pathname) {
  const match = ALL_ITEMS.filter((i) => !i.end).find((i) =>
    pathname.startsWith(i.to)
  );
  return match ? match.labelKey : 'nav.home';
}
