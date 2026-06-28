package ro.ubbcluj.ubbinfo.dto;

/** Aggregate counts for the admin Overview dashboard. */
public record OverviewDto(
        long students,
        long professors,
        long admins,
        long profiles,
        long courses,
        long enrollments,
        long graded,
        long pending,
        long evaluatedProfs
) {
}
