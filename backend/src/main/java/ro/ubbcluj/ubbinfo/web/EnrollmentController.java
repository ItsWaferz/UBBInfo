package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.EnrollmentDto;
import ro.ubbcluj.ubbinfo.service.EnrollmentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /** GET /api/enrollments/me — the logged-in student's grades/history. */
    @GetMapping("/me")
    public List<EnrollmentDto> mine() {
        return enrollmentService.myEnrollments();
    }

    /** GET /api/enrollments?courseId=... — professor/admin view for one course. */
    @GetMapping
    public List<EnrollmentDto> byCourse(@RequestParam("courseId") UUID courseId) {
        return enrollmentService.enrollmentsForCourse(courseId);
    }

    /** PATCH /api/enrollments/{id} — set a grade (professor of the course, or admin). */
    @PatchMapping("/{id}")
    public EnrollmentDto grade(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Integer grade = body.get("grade") == null ? null : ((Number) body.get("grade")).intValue();
        Boolean isRestanta = body.get("isRestanta") == null ? null : (Boolean) body.get("isRestanta");
        return enrollmentService.setGrade(id, grade, isRestanta);
    }
}
