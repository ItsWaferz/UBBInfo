package ro.ubbcluj.ubbinfo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.RequirementDto;
import ro.ubbcluj.ubbinfo.entity.SchedulingRequirement;
import ro.ubbcluj.ubbinfo.service.RequirementService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scheduling-requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping
    public List<RequirementDto> list() {
        return requirementService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementDto create(@RequestBody SchedulingRequirement requirement) {
        return requirementService.create(requirement);
    }

    @PutMapping("/{id}")
    public RequirementDto update(@PathVariable UUID id, @RequestBody SchedulingRequirement requirement) {
        return requirementService.update(id, requirement);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        requirementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
