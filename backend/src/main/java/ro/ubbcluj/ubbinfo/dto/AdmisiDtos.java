package ro.ubbcluj.ubbinfo.dto;

import java.util.List;

/** DTOs for the admitted-students import (feature #4). */
public final class AdmisiDtos {

    private AdmisiDtos() {
    }

    /** A parsed + validated row, with the proposed email — preview (no creation yet). */
    public record PreviewRow(
            int row,
            String fullName,
            String cnp,
            String groupName,
            String specialization,
            String proposedEmail,
            String status,   // OK | DUPLICATE | INVALID
            String message
    ) {
    }

    public record PreviewResult(
            int total,
            int ok,
            int duplicate,
            int invalid,
            boolean provisioningReady,   // is the service-role key configured?
            List<PreviewRow> rows
    ) {
    }

    /** Result of actually creating an account for a row. */
    public record ImportRow(
            int row,
            String fullName,
            String email,
            String password,   // shown once so the admin can hand it out
            String status,     // CREATED | SKIPPED | ERROR
            String message
    ) {
    }

    public record ImportResult(
            int total,
            int created,
            int skipped,
            int errors,
            List<ImportRow> rows
    ) {
    }
}
