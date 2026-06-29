package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * public.manual_grade — a manually-entered value for a (component, student),
 * for components not sourced from the Google Sheet.
 */
@Entity
@Table(name = "manual_grade")
public class ManualGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "component_id")
    private UUID componentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "value")
    private Double value;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getComponentId() { return componentId; }
    public void setComponentId(UUID componentId) { this.componentId = componentId; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}
