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

// Weighted average: Σ(grade × credits) / Σ(credits), only graded courses (excluding optionals)
export function weightedAverage(enrollments) {
  const graded = enrollments.filter(
    (e) => e.grade !== null && e.grade !== undefined && !e.courses?.is_optional
  );
  if (graded.length === 0) return null;
  let sumGC = 0;
  let sumC = 0;
  for (const e of graded) {
    // Exclude English from average calculation
    const isEnglish = e.courses?.name?.toLowerCase().includes('engleza');
    if (isEnglish) continue;

    const credits = e.courses?.credits ?? 0;
    sumGC += e.grade * credits;
    sumC += credits;
  }
  if (sumC === 0) return null;
  return sumGC / sumC;
}

// Sum credits (excluding optionals)
export function sumCredits(enrollments) {
  return enrollments.reduce((acc, e) => {
    if (e.courses?.is_optional) return acc;
    return acc + (e.courses?.credits ?? 0);
  }, 0);
}
