package ro.ubbcluj.ubbinfo.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTOs for student facilities (feature #5): burse, tabere, camin. */
public final class FacilityDtos {

    private FacilityDtos() {}

    public record DormDto(UUID id, String name, Integer capacity, Integer sortOrder, Boolean active) {}

    public record SettingDto(String key, String label, Integer capacity, BigDecimal reservedPercent) {}

    /** Public config the student needs to apply (dorm list + facility labels). */
    public record ConfigDto(List<DormDto> dorms, List<SettingDto> settings) {}

    /** A student's own application + its status (dashboard). */
    public record MyApplicationDto(
            String facility,
            String label,
            String status,        // none | pending | accepted | rejected
            String result,
            List<String> dormPrefs,
            Integer rank,
            BigDecimal media
    ) {}

    /** Body for applying. dormPrefs only used by camin. */
    public record ApplyRequest(List<String> dormPrefs) {}

    /** Body for creating/updating a dorm (typed so bad input 400s instead of being swallowed). */
    public record SaveDormRequest(UUID id, String name, Integer capacity, Integer sortOrder, Boolean active) {}

    /** Body for updating a facility's capacity / reserved percentage. */
    public record SaveSettingRequest(Integer capacity, BigDecimal reservedPercent) {}

    /** One row in the generated/published list (admin preview + PDF). */
    public record ApplicantRow(
            UUID studentId,
            String code,          // student_id / matricol
            String name,
            BigDecimal media,
            boolean reserved,
            String status,        // accepted | rejected
            String result,
            Integer rank
    ) {}

    /** Result of generating a list (preview) — not persisted. */
    public record GenerateResult(
            String facility,
            String label,
            int sizeX,
            int applicants,
            int accepted,
            List<ApplicantRow> rows
    ) {}

    /** Admin-facing applicant count per facility (overview). */
    public record FacilityOverviewDto(
            String key,
            String label,
            Integer capacity,
            BigDecimal reservedPercent,
            int applicants,
            int accepted,
            boolean published
    ) {}
}
