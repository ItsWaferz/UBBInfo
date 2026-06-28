package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.repository.CourseRepository;

import java.util.List;
import java.util.UUID;

/**
 * Courses are world-readable for any authenticated user; only admins may
 * create / update / delete (RLS: admin_insert/update/delete_courses).
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CurrentUserService currentUser;

    public CourseService(CourseRepository courseRepository, CurrentUserService currentUser) {
        this.courseRepository = courseRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<Course> list(boolean optionalOnly) {
        return optionalOnly
                ? courseRepository.findByIsOptionalTrueOrderByNameAsc()
                : courseRepository.findAllByOrderByNameAsc();
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
        existing.setLevel(changes.getLevel());
        existing.setProfile(changes.getProfile());
        if (changes.getIsOptional() != null) {
            existing.setIsOptional(changes.getIsOptional());
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
