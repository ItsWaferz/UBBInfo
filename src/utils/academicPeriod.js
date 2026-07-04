import { api } from '../api';

// Used only until the backend answers (or if it can't).
export const FALLBACK_PERIOD = { academic_year: '2025-2026', semester: 2 };

let cached = null;

/**
 * The CURRENT academic period { academic_year, semester }, served by the
 * backend from the semester_config table — the single place the rollover
 * happens. Cached for the session; falls back to FALLBACK_PERIOD on error.
 */
export function getCurrentPeriod() {
  if (!cached) {
    cached = api.get('/api/calendar/current').catch(() => FALLBACK_PERIOD);
  }
  return cached;
}
