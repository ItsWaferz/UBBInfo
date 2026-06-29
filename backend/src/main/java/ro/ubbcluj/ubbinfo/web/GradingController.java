package ro.ubbcluj.ubbinfo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComputeResult;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ManualGradeDto;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.SchemeDto;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.SheetColumnsDto;
import ro.ubbcluj.ubbinfo.service.GradingService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Grading schemes (professor, feature #2). */
@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @GetMapping("/{courseId}/scheme")
    public SchemeDto getScheme(@PathVariable UUID courseId) {
        return gradingService.getScheme(courseId);
    }

    @PutMapping("/{courseId}/scheme")
    public SchemeDto saveScheme(@PathVariable UUID courseId, @RequestBody SchemeDto scheme) {
        return gradingService.saveScheme(courseId, scheme);
    }

    /** Read the column headers (+ sample rows) from the linked Google Sheet. */
    @GetMapping("/{courseId}/sheet-columns")
    public SheetColumnsDto sheetColumns(@PathVariable UUID courseId) {
        return gradingService.sheetColumns(courseId);
    }

    @GetMapping("/{courseId}/manual")
    public List<ManualGradeDto> manualGrades(@PathVariable UUID courseId) {
        return gradingService.manualGrades(courseId);
    }

    @PutMapping("/{courseId}/manual")
    public ResponseEntity<Void> setManualGrade(@PathVariable UUID courseId, @RequestBody Map<String, Object> body) {
        UUID componentId = UUID.fromString(body.get("component_id").toString());
        UUID studentId = UUID.fromString(body.get("student_id").toString());
        Object v = body.get("value");
        Double value = (v == null || v.toString().isBlank()) ? null : Double.valueOf(v.toString());
        gradingService.setManualGrade(courseId, componentId, studentId, value);
        return ResponseEntity.noContent().build();
    }

    /** Compute final grades; ?save=true persists them to enrollments. */
    @PostMapping("/{courseId}/compute")
    public ComputeResult compute(@PathVariable UUID courseId,
                                 @RequestParam(name = "save", defaultValue = "false") boolean save) {
        return gradingService.compute(courseId, save);
    }
}
