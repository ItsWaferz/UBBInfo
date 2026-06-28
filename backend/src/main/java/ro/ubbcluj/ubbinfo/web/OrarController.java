package ro.ubbcluj.ubbinfo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.OrarDto;
import ro.ubbcluj.ubbinfo.entity.Orar;
import ro.ubbcluj.ubbinfo.service.OrarService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orar")
public class OrarController {

    private final OrarService orarService;

    public OrarController(OrarService orarService) {
        this.orarService = orarService;
    }

    /** GET /api/orar  (optionally ?group=1321/2 to filter a single group). */
    @GetMapping
    public List<OrarDto> list(@RequestParam(name = "group", required = false) String group) {
        return orarService.list(group);
    }

    /** GET /api/orar/groups — distinct group identifiers for pickers. */
    @GetMapping("/groups")
    public List<String> groups() {
        return orarService.groups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrarDto create(@RequestBody Orar entry) {
        return orarService.create(entry);
    }

    @PutMapping("/{id}")
    public OrarDto update(@PathVariable UUID id, @RequestBody Orar entry) {
        return orarService.update(id, entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orarService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
