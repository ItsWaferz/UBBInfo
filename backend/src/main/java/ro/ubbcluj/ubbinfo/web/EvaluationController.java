package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.EvaluationTargetsDto;
import ro.ubbcluj.ubbinfo.entity.ProfessorEvaluation;
import ro.ubbcluj.ubbinfo.service.EvaluationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /** GET /api/evaluations/targets — professors to evaluate + existing evaluations. */
    @GetMapping("/targets")
    public EvaluationTargetsDto targets() {
        return evaluationService.targets();
    }

    /** PUT /api/evaluations — create/update an evaluation for a (professor, course). */
    @PutMapping
    @SuppressWarnings("unchecked")
    public ProfessorEvaluation upsert(@RequestBody Map<String, Object> body) {
        UUID professorId = UUID.fromString((String) body.get("professor_id"));
        UUID courseId = UUID.fromString((String) body.get("course_id"));
        Map<String, Object> ratings = (Map<String, Object>) body.get("ratings");
        String comment = (String) body.get("comment");
        return evaluationService.upsert(professorId, courseId, ratings, comment);
    }
}
