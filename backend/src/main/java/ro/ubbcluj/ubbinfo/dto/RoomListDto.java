package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Room;

import java.util.UUID;

/** Flat room row for the RoomPicker + admin room admin (capacity/type). */
public record RoomListDto(
        UUID id,
        UUID buildingId,
        String code,
        String note,
        String location,
        Integer capacity,
        String roomType
) {
    public static RoomListDto from(Room r) {
        return new RoomListDto(r.getId(), r.getBuildingId(), r.getCode(), r.getNote(),
                r.getLocation(), r.getCapacity(), r.getRoomType());
    }
}
