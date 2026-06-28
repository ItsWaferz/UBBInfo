package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** public.rooms — a room within a building (code unique per building, plus note/location). */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "code")
    private String code;

    @Column(name = "note")
    private String note;

    @Column(name = "location")
    private String location;

    @Column(name = "capacity")
    private Integer capacity;

    /** 'CURS' | 'SEMINAR' | 'LABORATOR' | 'ORICE' — used to match activity types. */
    @Column(name = "room_type")
    private String roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", insertable = false, updatable = false)
    private Building building;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
}
