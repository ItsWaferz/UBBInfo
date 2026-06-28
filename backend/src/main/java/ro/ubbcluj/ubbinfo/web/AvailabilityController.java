package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.entity.ProfessorAvailability;
import ro.ubbcluj.ubbinfo.service.AvailabilityService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /** GET /api/availability/me — the caller's availability windows. */
    @GetMapping("/me")
    public List<ProfessorAvailability> mine() {
        return availabilityService.myWindows();
    }

    /** PUT /api/availability/me — replace the caller's availability windows. */
    @PutMapping("/me")
    public List<ProfessorAvailability> replaceMine(@RequestBody List<ProfessorAvailability> windows) {
        return availabilityService.replaceMyWindows(windows);
    }

    /** GET /api/availability/{professorId} — admin reads anyone's windows. */
    @GetMapping("/{professorId}")
    public List<ProfessorAvailability> of(@PathVariable UUID professorId) {
        return availabilityService.windowsOf(professorId);
    }
}
