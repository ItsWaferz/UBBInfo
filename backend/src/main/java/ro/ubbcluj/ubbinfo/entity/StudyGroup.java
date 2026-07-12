package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * public.groups — the authoritative catalog of study groups (seeded from the
 * faculty's official group/semigroup list, not derived from enrolled students).
 * {@code code} is the pre-slash group code (e.g. "1322"); {@code semigroups} is
 * how many semigroups it splits into (1 or 2), expanded to "1".."N" downstream.
 */
@Entity
@Table(name = "groups")
public class StudyGroup {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "spec_code")
    private String specCode;

    @Column(name = "study_year")
    private Integer studyYear;

    @Column(name = "semigroups")
    private Integer semigroups;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSpecCode() { return specCode; }
    public void setSpecCode(String specCode) { this.specCode = specCode; }

    public Integer getStudyYear() { return studyYear; }
    public void setStudyYear(Integer studyYear) { this.studyYear = studyYear; }

    public Integer getSemigroups() { return semigroups; }
    public void setSemigroups(Integer semigroups) { this.semigroups = semigroups; }
}
