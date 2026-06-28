package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.CatalogRowDto;
import ro.ubbcluj.ubbinfo.dto.ProfessorCourseDto;
import ro.ubbcluj.ubbinfo.service.ProfessorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    /** GET /api/professor-courses/mine — the professor's course assignments. */
    @GetMapping("/professor-courses/mine")
    public List<ProfessorCourseDto> myCourses() {
        return professorService.myCourses();
    }

    /** GET /api/professor/catalog?courseId=... — grade catalog for a course. */
    @GetMapping("/professor/catalog")
    public List<CatalogRowDto> catalog(@RequestParam("courseId") UUID courseId) {
        return professorService.catalog(courseId);
    }
}
