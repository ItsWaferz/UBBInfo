package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Orar;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Timetable entry as the frontend reads it: scalar fields + the embedded
 * {@code rooms} relation (Supabase shape: select('*, rooms(...)')).
 */
public record OrarDto(
        UUID id,
        String groupName,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String courseName,
        String type,
        String room,
        String professor,
        String weekParity,
        UUID roomId,
        String semigroup,
        RoomDto rooms
) {
    public static OrarDto from(Orar o) {
        return new OrarDto(
                o.getId(), o.getGroupName(), o.getDayOfWeek(), o.getStartTime(), o.getEndTime(),
                o.getCourseName(), o.getType(), o.getRoom(), o.getProfessor(), o.getWeekParity(),
                o.getRoomId(), o.getSemigroup(), RoomDto.from(o.getRoomRef()));
    }
}
