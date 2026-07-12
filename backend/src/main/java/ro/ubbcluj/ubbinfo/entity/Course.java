package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * public.courses — a discipline. {@code category} is one of
 * obligatoriu | optional | facultativ; only <b>facultativ</b> courses are
 * excluded from the academic average. {@code profile} is the specialization
 * (e.g. "Ingineria Informației (Limba Engleză)").
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "profile")
    private String profile;

    @Column(name = "category")
    private String category;

    @Column(name = "teaching_language")
    private String teachingLanguage;

    /** Academic year the discipline is taught in (1..4), within its specialization. */
    @Column(name = "study_year")
    private Integer studyYear;

    /** Semester within the year: 1 or 2. */
    @Column(name = "semester")
    private Integer semester;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTeachingLanguage() { return teachingLanguage; }
    public void setTeachingLanguage(String teachingLanguage) { this.teachingLanguage = teachingLanguage; }

    public Integer getStudyYear() { return studyYear; }
    public void setStudyYear(Integer studyYear) { this.studyYear = studyYear; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    /** Facultative courses are graded but never count toward the media. */
    public boolean isFacultativ() {
        return "facultativ".equalsIgnoreCase(category);
    }
}
