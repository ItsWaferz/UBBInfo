package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.RequirementDto;
import ro.ubbcluj.ubbinfo.entity.SchedulingRequirement;
import ro.ubbcluj.ubbinfo.repository.SchedulingRequirementRepository;

import java.util.List;
import java.util.UUID;

/** Admin CRUD for the timetable scheduling requirements (the generator's demand). */
@Service
public class RequirementService {

    private final SchedulingRequirementRepository repository;
    private final CurrentUserService currentUser;

    public RequirementService(SchedulingRequirementRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<RequirementDto> list() {
        currentUser.requireAdmin();
        return repository.findAllWithCourse().stream().map(RequirementDto::from).toList();
    }

    @Transactional
    public RequirementDto create(SchedulingRequirement r) {
        currentUser.requireAdmin();
        r.setId(null);
        return RequirementDto.from(repository.save(r));
    }

    @Transactional
    public RequirementDto update(UUID id, SchedulingRequirement changes) {
        currentUser.requireAdmin();
        SchedulingRequirement r = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Requirement not found: " + id));
        r.setCourseId(changes.getCourseId());
        r.setActivityType(changes.getActivityType());
        r.setGroupName(changes.getGroupName());
        r.setSessionsPerWeek(changes.getSessionsPerWeek());
        r.setDurationHours(changes.getDurationHours());
        r.setWeekParity(changes.getWeekParity());
        r.setStudentCount(changes.getStudentCount());
        r.setProfessorId(changes.getProfessorId());
        return RequirementDto.from(repository.save(r));
    }

    @Transactional
    public void delete(UUID id) {
        currentUser.requireAdmin();
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Requirement not found: " + id);
        }
        repository.deleteById(id);
    }
}
