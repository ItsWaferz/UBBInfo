package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.SemesterConfig;
import ro.ubbcluj.ubbinfo.entity.Vacation;
import ro.ubbcluj.ubbinfo.repository.SemesterConfigRepository;
import ro.ubbcluj.ubbinfo.repository.VacationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Academic calendar. Reads are world-readable for any authenticated user;
 * writes are admin-only (semcfg_admin_write / vac_admin_write).
 */
@Service
public class CalendarService {

    private final SemesterConfigRepository semesterRepository;
    private final VacationRepository vacationRepository;
    private final CurrentUserService currentUser;

    public CalendarService(SemesterConfigRepository semesterRepository,
                           VacationRepository vacationRepository,
                           CurrentUserService currentUser) {
        this.semesterRepository = semesterRepository;
        this.vacationRepository = vacationRepository;
        this.currentUser = currentUser;
    }

    // ---------- semester_config ----------

    @Transactional(readOnly = true)
    public List<SemesterConfig> semesters() {
        return semesterRepository.findAllByOrderByAcademicYearAscSemesterAsc();
    }

    @Transactional
    public SemesterConfig createSemester(SemesterConfig s) {
        currentUser.requireAdmin();
        s.setId(null);
        return semesterRepository.save(s);
    }

    @Transactional
    public SemesterConfig updateSemester(UUID id, SemesterConfig changes) {
        currentUser.requireAdmin();
        SemesterConfig s = semesterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Semester config not found: " + id));
        s.setAcademicYear(changes.getAcademicYear());
        s.setSemester(changes.getSemester());
        s.setStartDate(changes.getStartDate());
        s.setEndDate(changes.getEndDate());
        return semesterRepository.save(s);
    }

    @Transactional
    public void deleteSemester(UUID id) {
        currentUser.requireAdmin();
        if (!semesterRepository.existsById(id)) {
            throw new EntityNotFoundException("Semester config not found: " + id);
        }
        semesterRepository.deleteById(id);
    }

    // ---------- vacations ----------

    @Transactional(readOnly = true)
    public List<Vacation> vacations() {
        return vacationRepository.findAllByOrderByStartDateAsc();
    }

    @Transactional
    public Vacation createVacation(Vacation v) {
        currentUser.requireAdmin();
        v.setId(null);
        return vacationRepository.save(v);
    }

    @Transactional
    public Vacation updateVacation(UUID id, Vacation changes) {
        currentUser.requireAdmin();
        Vacation v = vacationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vacation not found: " + id));
        v.setName(changes.getName());
        v.setStartDate(changes.getStartDate());
        v.setEndDate(changes.getEndDate());
        return vacationRepository.save(v);
    }

    @Transactional
    public void deleteVacation(UUID id) {
        currentUser.requireAdmin();
        if (!vacationRepository.existsById(id)) {
            throw new EntityNotFoundException("Vacation not found: " + id);
        }
        vacationRepository.deleteById(id);
    }
}
