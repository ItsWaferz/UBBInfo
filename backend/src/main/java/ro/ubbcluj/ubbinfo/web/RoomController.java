package ro.ubbcluj.ubbinfo.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.RoomListDto;
import ro.ubbcluj.ubbinfo.entity.Building;
import ro.ubbcluj.ubbinfo.entity.Room;
import ro.ubbcluj.ubbinfo.repository.BuildingRepository;
import ro.ubbcluj.ubbinfo.repository.RoomRepository;
import ro.ubbcluj.ubbinfo.service.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buildings & rooms — world-readable for any authenticated user
 * (buildings_select_all / rooms_select_all). Used by the RoomPicker.
 * Admins can set room capacity + type (for timetable generation).
 */
@RestController
@RequestMapping("/api")
public class RoomController {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUser;

    public RoomController(BuildingRepository buildingRepository, RoomRepository roomRepository,
                          CurrentUserService currentUser) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/buildings")
    public List<Building> buildings() {
        return buildingRepository.findAllByOrderBySortOrderAsc();
    }

    @GetMapping("/rooms")
    public List<RoomListDto> rooms() {
        return roomRepository.findAllByOrderByCodeAsc().stream().map(RoomListDto::from).toList();
    }

    /** PUT /api/rooms/{id} — admin sets capacity + room_type. */
    @PutMapping("/rooms/{id}")
    @Transactional
    public RoomListDto updateRoom(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
        if (body.containsKey("capacity")) {
            Object cap = body.get("capacity");
            room.setCapacity(cap == null ? null : ((Number) cap).intValue());
        }
        if (body.containsKey("room_type")) {
            Object t = body.get("room_type");
            room.setRoomType(t == null ? null : t.toString());
        }
        return RoomListDto.from(roomRepository.save(room));
    }
}
