package ro.ubbcluj.ubbinfo.solver;

import java.util.UUID;

/** A room (problem fact): capacity + type + which building/zone it is in. */
public class SolverRoom {

    private UUID id;
    private String code;
    private Integer capacity;
    private String type;        // 'CURS' | 'SEMINAR' | 'LABORATOR' | 'ORICE' | null
    private UUID buildingId;
    private String zone;        // proximity zone of the building (null = only close to itself)

    public SolverRoom() {
    }

    public SolverRoom(UUID id, String code, Integer capacity, String type, UUID buildingId, String zone) {
        this.id = id;
        this.code = code;
        this.capacity = capacity;
        this.type = type;
        this.buildingId = buildingId;
        this.zone = zone;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public Integer getCapacity() { return capacity; }
    public String getType() { return type; }
    public UUID getBuildingId() { return buildingId; }
    public String getZone() { return zone; }

    @Override
    public String toString() {
        return code;
    }
}
