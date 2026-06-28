package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Room;

/**
 * Embedded room shape the frontend's formatRoom() expects:
 * {@code { code, note, location, buildings: { name, code } }}.
 */
public record RoomDto(
        String code,
        String note,
        String location,
        BuildingRef buildings
) {
    /** Minimal building reference (named "buildings" in the embedded shape). */
    public record BuildingRef(String name, String code) {
    }

    public static RoomDto from(Room r) {
        if (r == null) {
            return null;
        }
        BuildingRef b = r.getBuilding() == null
                ? null
                : new BuildingRef(r.getBuilding().getName(), r.getBuilding().getCode());
        return new RoomDto(r.getCode(), r.getNote(), r.getLocation(), b);
    }
}
