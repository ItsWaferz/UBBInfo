package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.ProfessorAvailability;
import ro.ubbcluj.ubbinfo.repository.ProfessorAvailabilityRepository;

import java.util.List;
import java.util.UUID;

/**
 * Professor availability windows. A professor manages their own; admins may read
 * anyone's (for context). Saving replaces the whole set for that professor.
 */
@Service
public class AvailabilityService {

    private final ProfessorAvailabilityRepository repository;
    private final CurrentUserService currentUser;

    public AvailabilityService(ProfessorAvailabilityRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ProfessorAvailability> myWindows() {
        return repository.findByProfessorId(currentUser.requireUserId());
    }

    @Transactional(readOnly = true)
    public List<ProfessorAvailability> windowsOf(UUID professorId) {
        currentUser.requireAdmin();
        return repository.findByProfessorId(professorId);
    }

    /** Replace the caller's availability with the given windows. */
    @Transactional
    public List<ProfessorAvailability> replaceMyWindows(List<ProfessorAvailability> windows) {
        UUID me = currentUser.requireUserId();
        // Availability windows are a professor-only concept (the solver reads
        // them for teaching staff); block non-professors from creating rows.
        if (!currentUser.isProfessor()) {
            throw new AccessDeniedException("Doar profesorii pot seta disponibilitatea.");
        }
        repository.deleteByProfessorId(me);
        for (ProfessorAvailability w : windows) {
            w.setId(null);
            w.setProfessorId(me);
            if (w.getPreference() == null || w.getPreference().isBlank()) {
                w.setPreference("available");
            }
        }
        return repository.saveAll(windows);
    }
}
