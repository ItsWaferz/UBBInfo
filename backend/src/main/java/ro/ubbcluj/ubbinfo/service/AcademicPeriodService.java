package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.SemesterConfig;
import ro.ubbcluj.ubbinfo.repository.SemesterConfigRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Single source of truth for the CURRENT academic period (year + semester),
 * derived from the semester_config table instead of constants scattered across
 * services and pages (which silently go stale at every rollover).
 *
 * Rule: the semester whose [start_date, end_date] contains today; during breaks,
 * the most recently STARTED semester (so summer still belongs to sem 2 until
 * sem 1 of the next year begins). Falls back to a constant only when the table
 * is empty. Cached for a few minutes — it changes twice a year.
 */
@Service
public class AcademicPeriodService {

    public record Period(String academicYear, int semester) {}

    private static final Period FALLBACK = new Period("2025-2026", 2);
    private static final long TTL_SECONDS = 300;

    private final SemesterConfigRepository semesterRepository;

    private volatile Period cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    public AcademicPeriodService(SemesterConfigRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public Period current() {
        if (cached != null && Instant.now().isBefore(cachedAt.plusSeconds(TTL_SECONDS))) {
            return cached;
        }
        Period p = compute(LocalDate.now());
        cached = p;
        cachedAt = Instant.now();
        return p;
    }

    private Period compute(LocalDate today) {
        List<SemesterConfig> rows = semesterRepository.findAll().stream()
                .filter(s -> s.getStartDate() != null && s.getAcademicYear() != null && s.getSemester() != null)
                .sorted(Comparator.comparing(SemesterConfig::getStartDate))
                .toList();
        if (rows.isEmpty()) {
            return FALLBACK;
        }
        SemesterConfig best = null;
        for (SemesterConfig s : rows) {
            if (!s.getStartDate().isAfter(today)) {
                best = s; // last semester that has started
            }
        }
        if (best == null) {
            best = rows.get(0); // everything is in the future — take the first
        }
        return new Period(best.getAcademicYear(), best.getSemester());
    }
}
