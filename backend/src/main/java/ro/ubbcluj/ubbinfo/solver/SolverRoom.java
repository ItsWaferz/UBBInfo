package ro.ubbcluj.ubbinfo.solver;

import java.util.UUID;

/** A room (problem fact): capacity + type, used for matching activities. */
public class SolverRoom {

    private UUID id;
    private String code;
    private Integer capacity;
    private String type;   // 'CURS' | 'SEMINAR' | 'LABORATOR' | 'ORICE' | null

    public SolverRoom() {
    }

    public SolverRoom(UUID id, String code, Integer capacity, String type) {
        this.id = id;
        this.code = code;
        this.capacity = capacity;
        this.type = type;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public Integer getCapacity() { return capacity; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return code;
    }
}
