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
import ro.ubbcluj.ubbinfo.entity.SemesterConfig;
import ro.ubbcluj.ubbinfo.entity.Vacation;
import ro.ubbcluj.ubbinfo.service.CalendarService;

import java.util.List;
import java.util.UUID;

/**
 * Academic calendar (semester config + vacations). Reads are world-readable for
 * any authenticated user; writes are admin-only (enforced in the service).
 */
@RestController
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendarService;
    private final ro.ubbcluj.ubbinfo.service.AcademicPeriodService periodService;

    public CalendarController(CalendarService calendarService,
                              ro.ubbcluj.ubbinfo.service.AcademicPeriodService periodService) {
        this.calendarService = calendarService;
        this.periodService = periodService;
    }

    // ---------- semester_config ----------

    @GetMapping("/semester-config")
    public List<SemesterConfig> semesterConfig() {
        return calendarService.semesters();
    }

    /** The CURRENT academic period (year + semester), derived from semester_config. */
    @GetMapping("/calendar/current")
    public java.util.Map<String, Object> currentPeriod() {
        var p = periodService.current();
        return java.util.Map.of("academic_year", p.academicYear(), "semester", p.semester());
    }

    @PostMapping("/semester-config")
    @ResponseStatus(HttpStatus.CREATED)
    public SemesterConfig createSemester(@RequestBody SemesterConfig s) {
        return calendarService.createSemester(s);
    }

    @PutMapping("/semester-config/{id}")
    public SemesterConfig updateSemester(@PathVariable UUID id, @RequestBody SemesterConfig s) {
        return calendarService.updateSemester(id, s);
    }

    @DeleteMapping("/semester-config/{id}")
    public ResponseEntity<Void> deleteSemester(@PathVariable UUID id) {
        calendarService.deleteSemester(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- vacations ----------

    @GetMapping("/vacations")
    public List<Vacation> vacations() {
        return calendarService.vacations();
    }

    @PostMapping("/vacations")
    @ResponseStatus(HttpStatus.CREATED)
    public Vacation createVacation(@RequestBody Vacation v) {
        return calendarService.createVacation(v);
    }

    @PutMapping("/vacations/{id}")
    public Vacation updateVacation(@PathVariable UUID id, @RequestBody Vacation v) {
        return calendarService.updateVacation(id, v);
    }

    @DeleteMapping("/vacations/{id}")
    public ResponseEntity<Void> deleteVacation(@PathVariable UUID id) {
        calendarService.deleteVacation(id);
        return ResponseEntity.noContent().build();
    }
}
