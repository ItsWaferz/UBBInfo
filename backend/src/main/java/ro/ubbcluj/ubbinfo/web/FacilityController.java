package ro.ubbcluj.ubbinfo.web;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ApplyRequest;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ConfigDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.DormDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.FacilityOverviewDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.GenerateResult;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.MyApplicationDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.SettingDto;
import ro.ubbcluj.ubbinfo.service.FacilityService;
import ro.ubbcluj.ubbinfo.service.FacilityService.GeneratedPdf;

import java.util.List;
import java.util.UUID;

/**
 * Student facilities (feature #5): burse, tabere, camin.
 *
 * <pre>
 *   GET    /api/facilities/config                 -> dorms + settings (apply form)
 *   GET    /api/facilities/me                      -> my applications + status
 *   POST   /api/facilities/{facility}/apply        -> apply (camin sends dorm_prefs)
 *   DELETE /api/facilities/{facility}/apply        -> withdraw
 *   GET    /api/facilities/admin/overview          -> per-facility counts (admin)
 *   GET    /api/facilities/admin/dorms             -> all dorms (admin)
 *   POST   /api/facilities/admin/dorms             -> create/update dorm
 *   DELETE /api/facilities/admin/dorms/{id}        -> delete dorm
 *   PUT    /api/facilities/admin/settings/{key}    -> update capacity/reserved %
 *   POST   /api/facilities/{facility}/generate?x=  -> preview list (admin)
 *   POST   /api/facilities/{facility}/pdf?x=       -> list PDF (admin)
 *   POST   /api/facilities/{facility}/publish?x=   -> publish results (admin)
 * </pre>
 */
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService service;

    public FacilityController(FacilityService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public ConfigDto config() {
        return service.publicConfig();
    }

    @GetMapping("/me")
    public List<MyApplicationDto> myApplications() {
        return service.myApplications();
    }

    @PostMapping("/{facility}/apply")
    public void apply(@PathVariable String facility, @RequestBody(required = false) ApplyRequest req) {
        service.apply(facility, req);
    }

    @DeleteMapping("/{facility}/apply")
    public void withdraw(@PathVariable String facility) {
        service.withdraw(facility);
    }

    // ----- admin -----

    @GetMapping("/admin/overview")
    public List<FacilityOverviewDto> overview() {
        return service.overview();
    }

    @GetMapping("/admin/dorms")
    public List<DormDto> dorms() {
        return service.allDorms();
    }

    @PostMapping("/admin/dorms")
    public DormDto saveDorm(@RequestBody ro.ubbcluj.ubbinfo.dto.FacilityDtos.SaveDormRequest body) {
        return service.saveDorm(body.id(), body.name(), body.capacity(), body.sortOrder(), body.active());
    }

    @DeleteMapping("/admin/dorms/{id}")
    public void deleteDorm(@PathVariable UUID id) {
        service.deleteDorm(id);
    }

    @PutMapping("/admin/settings/{key}")
    public SettingDto saveSetting(@PathVariable String key,
                                  @RequestBody ro.ubbcluj.ubbinfo.dto.FacilityDtos.SaveSettingRequest body) {
        return service.saveSetting(key, body.capacity(), body.reservedPercent());
    }

    @PostMapping("/{facility}/generate")
    public GenerateResult generate(@PathVariable String facility, @RequestParam(defaultValue = "0") int x) {
        return service.generate(facility, x);
    }

    @PostMapping("/{facility}/publish")
    public GenerateResult publish(@PathVariable String facility, @RequestParam(defaultValue = "0") int x) {
        return service.publish(facility, x);
    }

    @PostMapping("/{facility}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable String facility, @RequestParam(defaultValue = "0") int x) {
        GeneratedPdf g = service.pdf(facility, x);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(g.filename()).build().toString())
                .body(new ByteArrayResource(g.pdf()));
    }
}
