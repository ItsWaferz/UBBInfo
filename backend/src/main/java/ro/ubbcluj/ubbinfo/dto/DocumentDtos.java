package ro.ubbcluj.ubbinfo.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs for student document generation (feature #1). */
public final class DocumentDtos {

    private DocumentDtos() {}

    /** A single editable field on a document form. */
    public record DocField(
            String key,
            String label,
            String type,           // text | textarea | date | select
            String value,          // pre-filled value
            List<String> options,  // for type=select
            String section,        // optional grouping header
            boolean full,          // render full-width
            String hint
    ) {}

    /** A document type the student can generate. */
    public record DocTypeInfo(String key, String title, String description, String icon) {}

    /** The pre-filled form for one document type. */
    public record PrefillResult(String type, String title, List<DocField> fields) {}

    /** An already-issued document (audit / history). */
    public record IssuedDocDto(
            UUID id,
            String type,
            String title,
            String regNumber,
            String academicYear,
            Integer semester,
            OffsetDateTime createdAt
    ) {}
}
