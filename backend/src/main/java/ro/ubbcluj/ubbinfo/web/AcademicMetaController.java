package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.entity.Specialization;
import ro.ubbcluj.ubbinfo.service.AcademicMetaService;

import java.util.List;

/** Reference data for the student group cascade (specializations + groups). */
@RestController
@RequestMapping("/api")
public class AcademicMetaController {

    private final AcademicMetaService service;

    public AcademicMetaController(AcademicMetaService service) {
        this.service = service;
    }

    /** GET /api/specializations — code + name + language + faculty. */
    @GetMapping("/specializations")
    public List<Specialization> specializations() {
        return service.specializations();
    }

    /** GET /api/groups — selectable groups derived from existing group_name codes. */
    @GetMapping("/groups")
    public List<AcademicMetaService.GroupDto> groups() {
        return service.groups();
    }
}
