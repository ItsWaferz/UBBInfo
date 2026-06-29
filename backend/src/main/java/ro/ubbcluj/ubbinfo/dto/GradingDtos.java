package ro.ubbcluj.ubbinfo.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTOs for the grading-scheme feature (#2). */
public final class GradingDtos {

    private GradingDtos() {
    }

    public record ComponentDto(
            UUID id,
            String name,
            Double weight,
            Boolean isBonus,
            Double minThreshold,
            String source,            // 'document' | 'manual'
            List<String> sheetColumns,
            Integer sortOrder
    ) {
    }

    /** Full scheme (settings + components) for the professor's editor. */
    public record SchemeDto(
            UUID courseId,
            String passMode,
            Double passThreshold,
            Boolean roundUp,
            String sheetUrl,
            String matchField,
            String matchColumn,
            List<ComponentDto> components
    ) {
    }

    /** Columns (+ a few sample rows) read from the linked Google Sheet. */
    public record SheetColumnsDto(
            List<String> headers,
            List<Map<String, String>> sampleRows
    ) {
    }

    public record ManualGradeDto(UUID componentId, UUID studentId, Double value) {
    }

    /** One student's computed result (preview before save). */
    public record ComputeRow(
            UUID studentId,
            String studentName,
            boolean matched,
            Map<String, Double> components,   // component name -> value
            Double base,
            Double bonus,
            Double finalRaw,
            Double finalStored,
            boolean passed,
            String note
    ) {
    }

    public record ComputeResult(
            List<ComputeRow> rows,
            List<String> unmatchedSheetKeys,
            boolean saved
    ) {
    }
}
