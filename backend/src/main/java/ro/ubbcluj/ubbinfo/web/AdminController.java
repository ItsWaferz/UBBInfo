package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.AdminEvaluationsDto;
import ro.ubbcluj.ubbinfo.dto.OverviewDto;
import ro.ubbcluj.ubbinfo.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** GET /api/admin/overview — aggregate dashboard stats. */
    @GetMapping("/overview")
    public OverviewDto overview() {
        return adminService.overview();
    }

    /** GET /api/admin/evaluations — anonymized professor evaluation report. */
    @GetMapping("/evaluations")
    public AdminEvaluationsDto evaluations() {
        return adminService.evaluations();
    }
}
