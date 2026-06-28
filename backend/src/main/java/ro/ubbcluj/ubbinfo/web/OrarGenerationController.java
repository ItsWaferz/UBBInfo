package ro.ubbcluj.ubbinfo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.entity.TimetableDraft;
import ro.ubbcluj.ubbinfo.entity.TimetableDraftLesson;
import ro.ubbcluj.ubbinfo.service.GenerationService;

import java.util.List;
import java.util.UUID;

/** Timetable generation + draft management (admin). */
@RestController
@RequestMapping("/api/orar")
public class OrarGenerationController {

    private final GenerationService generationService;

    public OrarGenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    /** POST /api/orar/generate?drafts=3 — run the solver and produce drafts. */
    @PostMapping("/generate")
    public List<TimetableDraft> generate(@RequestParam(name = "drafts", defaultValue = "3") int drafts) {
        return generationService.generate(drafts);
    }

    @GetMapping("/drafts")
    public List<TimetableDraft> drafts() {
        return generationService.listDrafts();
    }

    @GetMapping("/drafts/{id}/lessons")
    public List<TimetableDraftLesson> draftLessons(@PathVariable UUID id) {
        return generationService.draftLessons(id);
    }

    /** POST /api/orar/drafts/{id}/publish — replace the live orar with this draft. */
    @PostMapping("/drafts/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable UUID id) {
        generationService.publishDraft(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        generationService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }
}
