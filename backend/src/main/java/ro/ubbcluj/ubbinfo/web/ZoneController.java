package ro.ubbcluj.ubbinfo.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.entity.Zone;
import ro.ubbcluj.ubbinfo.repository.ZoneRepository;
import ro.ubbcluj.ubbinfo.service.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proximity zones. Reads are world-readable for any authenticated user;
 * create/delete are admin-only. Zones exist independently of buildings so one
 * can be added before it's assigned (populates the building "zonă" dropdown).
 */
@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneRepository zoneRepository;
    private final CurrentUserService currentUser;

    public ZoneController(ZoneRepository zoneRepository, CurrentUserService currentUser) {
        this.zoneRepository = zoneRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Zone> list() {
        return zoneRepository.findAllByOrderByNameAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Zone create(@RequestBody Map<String, Object> body) {
        currentUser.requireAdmin();
        String name = body.get("name") == null ? "" : body.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Numele zonei este obligatoriu.");
        }
        // Idempotent: reuse an existing zone with the same name (case-insensitive).
        return zoneRepository.findFirstByNameIgnoreCase(name).orElseGet(() -> {
            Zone z = new Zone();
            z.setName(name);
            return zoneRepository.save(z);
        });
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        currentUser.requireAdmin();
        if (!zoneRepository.existsById(id)) {
            throw new EntityNotFoundException("Zone not found: " + id);
        }
        zoneRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
