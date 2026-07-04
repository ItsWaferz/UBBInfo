package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.AdminOverview;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.MyTuition;
import ro.ubbcluj.ubbinfo.service.TuitionService;

import java.util.Map;

/**
 * Tuition & fees (feature #6).
 *
 * <pre>
 *   GET  /api/tuition/me              -> my installments + restanțe + totals
 *   POST /api/tuition/pay {charge_key}-> simulate paying one charge
 *   POST /api/tuition/pay-advance     -> pay all 4 installments (-10%)
 *   GET  /api/tuition/admin/overview  -> per-student statistic (admin)
 * </pre>
 */
@RestController
@RequestMapping("/api/tuition")
public class TuitionController {

    private final TuitionService service;

    public TuitionController(TuitionService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public MyTuition me() {
        return service.myTuition();
    }

    @PostMapping("/pay")
    public MyTuition pay(@RequestBody Map<String, String> body) {
        service.pay(body.get("charge_key"));
        return service.myTuition();
    }

    @PostMapping("/pay-advance")
    public MyTuition payAdvance() {
        service.payAllAdvance();
        return service.myTuition();
    }

    @GetMapping("/admin/overview")
    public AdminOverview overview() {
        return service.adminOverview();
    }
}
