package ro.ubbcluj.ubbinfo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.ExamDto;
import ro.ubbcluj.ubbinfo.entity.Exam;
import ro.ubbcluj.ubbinfo.entity.ExamRegistration;
import ro.ubbcluj.ubbinfo.service.ExamRegistrationService;
import ro.ubbcluj.ubbinfo.service.ExamService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ExamController {

    private final ExamService examService;
    private final ExamRegistrationService registrationService;

    public ExamController(ExamService examService, ExamRegistrationService registrationService) {
        this.examService = examService;
        this.registrationService = registrationService;
    }

    /** GET /api/exams/mine — exams for the student's enrolled courses. */
    @GetMapping("/exams/mine")
    public List<ExamDto> myExams() {
        return examService.myExams();
    }

    /** GET /api/exams/teaching — the professor's own exams. */
    @GetMapping("/exams/teaching")
    public List<ExamDto> teachingExams() {
        return examService.myProfessorExams();
    }

    /** POST /api/exams — schedule an exam (professor of the course, or admin). */
    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamDto create(@RequestBody Exam exam) {
        return examService.create(exam);
    }

    @PutMapping("/exams/{id}")
    public ExamDto update(@PathVariable UUID id, @RequestBody Exam exam) {
        return examService.update(id, exam);
    }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        examService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/exam-registrations/me — the student's chosen slots. */
    @GetMapping("/exam-registrations/me")
    public List<ExamRegistration> myRegistrations() {
        return registrationService.myRegistrations();
    }

    /** PUT /api/exam-registrations — register/move the slot for a course. */
    @PutMapping("/exam-registrations")
    public ExamRegistration register(@RequestBody Map<String, UUID> body) {
        return registrationService.register(body.get("course_id"), body.get("exam_id"));
    }

    /** DELETE /api/exam-registrations?courseId=... — cancel a registration. */
    @DeleteMapping("/exam-registrations")
    public ResponseEntity<Void> cancel(@RequestParam("courseId") UUID courseId) {
        registrationService.cancel(courseId);
        return ResponseEntity.noContent().build();
    }
}
