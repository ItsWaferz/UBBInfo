package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.ImportResult;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.PreviewResult;
import ro.ubbcluj.ubbinfo.service.AdmisiImportService;

/** Admitted-students account import (admin, feature #4). */
@RestController
@RequestMapping("/api/admisi")
public class AdmisiController {

    private final AdmisiImportService importService;

    public AdmisiController(AdmisiImportService importService) {
        this.importService = importService;
    }

    /** POST /api/admisi/preview — validate + propose emails, without creating anything. */
    @PostMapping("/preview")
    public PreviewResult preview(@RequestParam("file") MultipartFile file) {
        return importService.preview(file);
    }

    /** POST /api/admisi/import — create the academic accounts; returns a per-row report. */
    @PostMapping("/import")
    public ImportResult importFile(@RequestParam("file") MultipartFile file) {
        return importService.importFile(file);
    }
}
