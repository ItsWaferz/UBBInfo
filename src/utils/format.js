// Shared formatting helpers

// "Sâmbătă, 14 Iunie 2025" — Romanian locale, first letter capitalized
export function formatRomanianDate(date = new Date()) {
  const formatted = new Intl.DateTimeFormat('ro-RO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date);
  return formatted.charAt(0).toUpperCase() + formatted.slice(1);
}

// Extract first name: text after the last space in the full name
export function firstNameOf(fullName = '') {
  const parts = fullName.trim().split(/\s+/);
  return parts.length ? parts[parts.length - 1] : '';
}

/**
 * Whether an enrollment counts toward the academic average. Only FACULTATIV
 * courses (e.g. Limba Engleză) are excluded — they're graded and shown, but
 * never affect the media or its credit base. Obligatorii + opționale count.
 */
export function countsTowardMedia(e) {
  return e.courses?.category !== 'facultativ';
}

/** Badge text for a course category ('Facultativ' / 'Opțional'), or null for obligatoriu. */
export function categoryLabel(course) {
  const cat = course?.category;
  if (cat === 'facultativ') return 'Facultativ';
  if (cat === 'optional') return 'Opțional';
  return null;
}

// Weighted average: Σ(grade × credits) / Σ(credits) over graded, media-counting courses.
export function weightedAverage(enrollments) {
  let sumGC = 0;
  let sumC = 0;
  for (const e of enrollments) {
    if (e.grade === null || e.grade === undefined) continue;
    if (!countsTowardMedia(e)) continue;
    const credits = e.courses?.credits ?? 0;
    sumGC += e.grade * credits;
    sumC += credits;
  }
  if (sumC === 0) return null;
  return sumGC / sumC;
}

// Sum credits that count toward the media (excludes optionals + language courses).
export function sumCredits(enrollments) {
  return enrollments.reduce(
    (acc, e) => (countsTowardMedia(e) ? acc + (e.courses?.credits ?? 0) : acc),
    0
  );
}

/** Money formatter for tuition/fees pages: 1250 -> "1.250 lei". */
export function lei(n) {
  return `${Number(n ?? 0).toLocaleString('ro-RO')} lei`;
}
