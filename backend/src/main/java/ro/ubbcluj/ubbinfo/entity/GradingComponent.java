package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * public.grading_component — one component of a grading scheme (e.g. "Laborator",
 * "Examen final", a bonus). Its value comes from averaging {@code sheetColumns}
 * (source=document) or from manual entry (source=manual).
 */
@Entity
@Table(name = "grading_component")
public class GradingComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "scheme_id")
    private UUID schemeId;

    @Column(name = "name")
    private String name;

    @Column(name = "weight")
    private Double weight;          // percentage

    @Column(name = "is_bonus")
    private Boolean isBonus;

    @Column(name = "min_threshold")
    private Double minThreshold;

    @Column(name = "source")
    private String source;          // 'document' | 'manual'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sheet_columns", columnDefinition = "jsonb")
    private List<String> sheetColumns;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSchemeId() { return schemeId; }
    public void setSchemeId(UUID schemeId) { this.schemeId = schemeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public Boolean getIsBonus() { return isBonus; }
    public void setIsBonus(Boolean isBonus) { this.isBonus = isBonus; }

    public Double getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Double minThreshold) { this.minThreshold = minThreshold; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public List<String> getSheetColumns() { return sheetColumns; }
    public void setSheetColumns(List<String> sheetColumns) { this.sheetColumns = sheetColumns; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
