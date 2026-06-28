package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * public.profiles — one row per auth.users id.
 * The PK equals the Supabase auth user id (the JWT "sub" claim); it is never generated here.
 */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "initials")
    private String initials;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "email")
    private String email;

    @Column(name = "faculty")
    private String faculty;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "transport_id")
    private String transportId;

    @Column(name = "financing")
    private String financing;

    @Column(name = "academic_rank")
    private String academicRank;

    @Column(name = "honorifics")
    private String honorifics;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "study_year")
    private String studyYear;

    // --- Sensitive identity fields (Identity page) ---
    @Column(name = "phone")
    private String phone;

    @Column(name = "personal_email")
    private String personalEmail;

    @Column(name = "iban")
    private String iban;

    @Column(name = "cnp")
    private String cnp;

    @Column(name = "id_series")
    private String idSeries;

    @Column(name = "address")
    private String address;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getTransportId() { return transportId; }
    public void setTransportId(String transportId) { this.transportId = transportId; }

    public String getFinancing() { return financing; }
    public void setFinancing(String financing) { this.financing = financing; }

    public String getAcademicRank() { return academicRank; }
    public void setAcademicRank(String academicRank) { this.academicRank = academicRank; }

    public String getHonorifics() { return honorifics; }
    public void setHonorifics(String honorifics) { this.honorifics = honorifics; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getStudyYear() { return studyYear; }
    public void setStudyYear(String studyYear) { this.studyYear = studyYear; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getCnp() { return cnp; }
    public void setCnp(String cnp) { this.cnp = cnp; }

    public String getIdSeries() { return idSeries; }
    public void setIdSeries(String idSeries) { this.idSeries = idSeries; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
