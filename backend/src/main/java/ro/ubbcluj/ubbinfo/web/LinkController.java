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
import ro.ubbcluj.ubbinfo.entity.UsefulLink;
import ro.ubbcluj.ubbinfo.service.LinkService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    /** GET /api/links  (?active=true for only active links — the dashboard). */
    @GetMapping
    public List<UsefulLink> list(@RequestParam(name = "active", defaultValue = "false") boolean activeOnly) {
        return linkService.list(activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsefulLink create(@RequestBody UsefulLink link) {
        return linkService.create(link);
    }

    @PutMapping("/{id}")
    public UsefulLink update(@PathVariable UUID id, @RequestBody UsefulLink link) {
        return linkService.update(id, link);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        linkService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
