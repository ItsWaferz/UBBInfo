package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** public.facility_settings — capacity + reserved percentage per facility. */
@Entity
@Table(name = "facility_settings")
public class FacilitySetting {

    @Id
    @Column(name = "key")
    private String key;       // camin | tabara | bursa_sociala | bursa_merit

    @Column(name = "label")
    private String label;

    @Column(name = "capacity")
    private Integer capacity; // null for camin (sum of dorms)

    @Column(name = "reserved_percent")
    private BigDecimal reservedPercent;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public BigDecimal getReservedPercent() { return reservedPercent; }
    public void setReservedPercent(BigDecimal reservedPercent) { this.reservedPercent = reservedPercent; }
}
