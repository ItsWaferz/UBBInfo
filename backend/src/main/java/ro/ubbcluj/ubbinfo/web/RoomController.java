package ro.ubbcluj.ubbinfo.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
 * Buildings & rooms. Reads are world-readable for any authenticated user;
 * create/update/delete are admin-only (the dedicated admin "Clădiri & săli" page).
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

    // ---------- buildings ----------

    @GetMapping("/buildings")
    public List<Building> buildings() {
        return buildingRepository.findAllByOrderBySortOrderAsc();
    }

    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Building createBuilding(@RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        Building b = new Building();
        applyBuilding(b, body);
        return buildingRepository.save(b);
    }

    @PutMapping("/buildings/{id}")
    @Transactional
    public Building updateBuilding(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        Building b = buildingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + id));
        applyBuilding(b, body);
        return buildingRepository.save(b);
    }

    @DeleteMapping("/buildings/{id}")
    @Transactional
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID id) {
        currentUser.requireAdmin();
        if (!buildingRepository.existsById(id)) {
            throw new EntityNotFoundException("Building not found: " + id);
        }
        buildingRepository.deleteById(id); // rooms cascade (FK on delete cascade)
        return ResponseEntity.noContent().build();
    }

    private void applyBuilding(Building b, Map<String, Object> body) {
        if (body.containsKey("code")) b.setCode(str(body.get("code")));
        if (body.containsKey("name")) b.setName(str(body.get("name")));
        if (body.containsKey("address")) b.setAddress(str(body.get("address")));
        if (body.containsKey("zone")) b.setZone(str(body.get("zone")));
        if (body.containsKey("sort_order")) {
            b.setSortOrder(toInt(body.get("sort_order")));
        }
    }

    // ---------- rooms ----------

    @GetMapping("/rooms")
    public List<RoomListDto> rooms() {
        return roomRepository.findAllByOrderByCodeAsc().stream().map(RoomListDto::from).toList();
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RoomListDto createRoom(@RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        Room r = new Room();
        applyRoom(r, body);
        return RoomListDto.from(roomRepository.save(r));
    }

    @PutMapping("/rooms/{id}")
    @Transactional
    public RoomListDto updateRoom(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        Room r = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
        applyRoom(r, body);
        return RoomListDto.from(roomRepository.save(r));
    }

    @DeleteMapping("/rooms/{id}")
    @Transactional
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID id) {
        currentUser.requireAdmin();
        if (!roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Room not found: " + id);
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyRoom(Room r, Map<String, Object> body) {
        if (body.containsKey("building_id")) {
            Object v = body.get("building_id");
            r.setBuildingId(v == null ? null : UUID.fromString(v.toString()));
        }
        if (body.containsKey("code")) r.setCode(str(body.get("code")));
        if (body.containsKey("note")) r.setNote(str(body.get("note")));
        if (body.containsKey("location")) r.setLocation(str(body.get("location")));
        if (body.containsKey("room_type")) r.setRoomType(str(body.get("room_type")));
        if (body.containsKey("capacity")) {
            r.setCapacity(toInt(body.get("capacity")));
        }
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Coerce a JSON value to an Integer: accepts numbers and numeric strings,
     * treats blank as null, and rejects anything else with a 400 (via the global
     * IllegalArgumentException handler) instead of a 500 ClassCastException.
     */
    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valoare numerică invalidă: " + s);
        }
    }
}
