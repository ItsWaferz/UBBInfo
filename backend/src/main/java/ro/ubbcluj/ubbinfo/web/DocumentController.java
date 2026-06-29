package ro.ubbcluj.ubbinfo.web;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.DocTypeInfo;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.IssuedDocDto;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.PrefillResult;
import ro.ubbcluj.ubbinfo.service.DocumentService;
import ro.ubbcluj.ubbinfo.service.DocumentService.Generated;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Student document generation (feature #1).
 *
 * <pre>
 *   GET  /api/documents/types            -> available document types
 *   GET  /api/documents/{type}/prefill   -> pre-filled editable form
 *   POST /api/documents/{type}/generate  -> rendered PDF (and audit row)
 *   GET  /api/documents/history          -> my issued documents
 *   GET  /api/documents/{id}/download    -> re-download a stored document
 * </pre>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/types")
    public List<DocTypeInfo> types() {
        return documentService.types();
    }

    @GetMapping("/history")
    public List<IssuedDocDto> history() {
        return documentService.history();
    }

    @GetMapping("/{type}/prefill")
    public PrefillResult prefill(@PathVariable String type) {
        return documentService.prefill(type);
    }

    @PostMapping("/{type}/generate")
    public ResponseEntity<Resource> generate(@PathVariable String type,
                                             @RequestBody Map<String, String> fields) {
        Generated g = documentService.generate(type, fields);
        return pdf(g);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Generated g = documentService.reRender(id);
        return pdf(g);
    }

    private static ResponseEntity<Resource> pdf(Generated g) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(g.filename()).build().toString())
                .body(new ByteArrayResource(g.pdf()));
    }
}
