package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.repository.CourseRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;

import java.util.List;
import java.util.UUID;

/**
 * Courses are world-readable for any authenticated user; only admins may
 * create / update / delete (RLS: admin_insert/update/delete_courses).
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ProfileRepository profileRepository;
    private final CurrentUserService currentUser;

    public CourseService(CourseRepository courseRepository,
                         ProfileRepository profileRepository,
                         CurrentUserService currentUser) {
        this.courseRepository = courseRepository;
        this.profileRepository = profileRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<Course> list(boolean facultativeOnly) {
        return facultativeOnly
                ? courseRepository.findByCategoryOrderByNameAsc("facultativ")
                : courseRepository.findAllByOrderByNameAsc();
    }

    /** Distinct student specializations — the source for the course "profil" dropdown. */
    @Transactional(readOnly = true)
    public List<String> specializations() {
        return profileRepository.findDistinctSpecializations();
    }

    @Transactional
    public Course create(Course course) {
        currentUser.requireAdmin();
        course.setId(null); // let the DB/Hibernate assign it
        return courseRepository.save(course);
    }

    @Transactional
    public Course update(UUID id, Course changes) {
        currentUser.requireAdmin();
        Course existing = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + id));
        existing.setName(changes.getName());
        existing.setCredits(changes.getCredits());
        existing.setProfile(changes.getProfile());
        existing.setTeachingLanguage(changes.getTeachingLanguage());
        existing.setStudyYear(changes.getStudyYear());
        existing.setSemester(changes.getSemester());
        if (changes.getCategory() != null) {
            existing.setCategory(changes.getCategory());
        }
        return courseRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        currentUser.requireAdmin();
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course not found: " + id);
        }
        courseRepository.deleteById(id);
    }
}
