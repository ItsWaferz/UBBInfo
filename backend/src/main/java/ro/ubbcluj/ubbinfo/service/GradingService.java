package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComponentDto;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComputeResult;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComputeRow;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ManualGradeDto;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.SchemeDto;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.SheetColumnsDto;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.GradingComponent;
import ro.ubbcluj.ubbinfo.entity.GradingScheme;
import ro.ubbcluj.ubbinfo.entity.ManualGrade;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.GradingComponentRepository;
import ro.ubbcluj.ubbinfo.repository.GradingSchemeRepository;
import ro.ubbcluj.ubbinfo.repository.ManualGradeRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Grading schemes: a professor defines a per-course formula (weighted components,
 * pass rules), pulls component values from a Google Sheet and/or manual entry,
 * then computes + stores each student's final grade and breakdown.
 */
@Service
public class GradingService {

    private final GradingSchemeRepository schemeRepository;
    private final GradingComponentRepository componentRepository;
    private final ManualGradeRepository manualGradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GoogleSheetReader sheetReader;
    private final CurrentUserService currentUser;

    public GradingService(GradingSchemeRepository schemeRepository,
                          GradingComponentRepository componentRepository,
                          ManualGradeRepository manualGradeRepository,
                          EnrollmentRepository enrollmentRepository,
                          GoogleSheetReader sheetReader,
                          CurrentUserService currentUser) {
        this.schemeRepository = schemeRepository;
        this.componentRepository = componentRepository;
        this.manualGradeRepository = manualGradeRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sheetReader = sheetReader;
        this.currentUser = currentUser;
    }

    private void requireCanGrade(UUID courseId) {
        if (!currentUser.isAdmin() && !currentUser.teachesCourse(courseId)) {
            throw new AccessDeniedException("Nu predai această disciplină.");
        }
    }

    // ---------- scheme CRUD ----------

    @Transactional(readOnly = true)
    public SchemeDto getScheme(UUID courseId) {
        requireCanGrade(courseId);
        UUID me = currentUser.requireUserId();
        GradingScheme s = schemeRepository.findByCourseIdAndProfessorId(courseId, me).orElse(null);
        if (s == null) {
            return null;
        }
        List<ComponentDto> comps = componentRepository.findBySchemeIdOrderBySortOrderAsc(s.getId())
                .stream().map(this::toDto).toList();
        return new SchemeDto(courseId, s.getPassMode(), s.getPassThreshold(), s.getRoundUp(),
                s.getSheetUrl(), s.getMatchField(), s.getMatchColumn(), comps);
    }

    @Transactional
    public SchemeDto saveScheme(UUID courseId, SchemeDto dto) {
        requireCanGrade(courseId);
        UUID me = currentUser.requireUserId();
        GradingScheme s = schemeRepository.findByCourseIdAndProfessorId(courseId, me)
                .orElseGet(GradingScheme::new);
        s.setCourseId(courseId);
        s.setProfessorId(me);
        s.setPassMode(dto.passMode() == null ? "overall" : dto.passMode());
        s.setPassThreshold(dto.passThreshold() == null ? 5.0 : dto.passThreshold());
        s.setRoundUp(dto.roundUp() == null ? Boolean.TRUE : dto.roundUp());
        s.setSheetUrl(blank(dto.sheetUrl()));
        s.setMatchField(dto.matchField() == null ? "student_id" : dto.matchField());
        s.setMatchColumn(blank(dto.matchColumn()));
        GradingScheme saved = schemeRepository.save(s);

        // replace components
        componentRepository.deleteBySchemeId(saved.getId());
        List<ComponentDto> in = dto.components() == null ? List.of() : dto.components();
        int order = 0;
        for (ComponentDto c : in) {
            GradingComponent gc = new GradingComponent();
            gc.setSchemeId(saved.getId());
            gc.setName(c.name());
            gc.setWeight(c.weight() == null ? 0.0 : c.weight());
            gc.setIsBonus(Boolean.TRUE.equals(c.isBonus()));
            gc.setMinThreshold(c.minThreshold());
            gc.setSource(c.source() == null ? "document" : c.source());
            gc.setSheetColumns(c.sheetColumns() == null ? List.of() : c.sheetColumns());
            gc.setSortOrder(order++);
            componentRepository.save(gc);
        }
        return getScheme(courseId);
    }

    // ---------- sheet columns ----------

    @Transactional(readOnly = true)
    public SheetColumnsDto sheetColumns(UUID courseId) {
        requireCanGrade(courseId);
        UUID me = currentUser.requireUserId();
        GradingScheme s = schemeRepository.findByCourseIdAndProfessorId(courseId, me)
                .orElseThrow(() -> new EntityNotFoundException("Definește întâi schema (cu link sheet)."));
        GoogleSheetReader.SheetData data = sheetReader.read(s.getSheetUrl());
        List<Map<String, String>> sample = data.rows().stream().limit(5).toList();
        return new SheetColumnsDto(data.headers(), sample);
    }

    // ---------- manual grades ----------

    @Transactional(readOnly = true)
    public List<ManualGradeDto> manualGrades(UUID courseId) {
        requireCanGrade(courseId);
        UUID me = currentUser.requireUserId();
        GradingScheme s = schemeRepository.findByCourseIdAndProfessorId(courseId, me).orElse(null);
        if (s == null) {
            return List.of();
        }
        List<UUID> compIds = componentRepository.findBySchemeIdOrderBySortOrderAsc(s.getId())
                .stream().map(GradingComponent::getId).toList();
        if (compIds.isEmpty()) {
            return List.of();
        }
        return manualGradeRepository.findByComponentIdIn(compIds).stream()
                .map(m -> new ManualGradeDto(m.getComponentId(), m.getStudentId(), m.getValue()))
                .toList();
    }

    @Transactional
    public void setManualGrade(UUID courseId, UUID componentId, UUID studentId, Double value) {
        requireCanGrade(courseId);
        // The component must belong to the CALLER's scheme for THIS course —
        // otherwise a professor could write grades into another professor's
        // scheme by passing a foreign componentId with their own courseId.
        UUID me = currentUser.requireUserId();
        GradingScheme myScheme = schemeRepository.findByCourseIdAndProfessorId(courseId, me)
                .orElseThrow(() -> new EntityNotFoundException("Schema de notare nu e definită."));
        GradingComponent comp = componentRepository.findById(componentId)
                .orElseThrow(() -> new EntityNotFoundException("Componenta nu există."));
        if (!myScheme.getId().equals(comp.getSchemeId())) {
            throw new AccessDeniedException("Componenta nu aparține schemei tale pentru această disciplină.");
        }
        ManualGrade m = manualGradeRepository.findByComponentIdAndStudentId(componentId, studentId)
                .orElseGet(ManualGrade::new);
        m.setComponentId(componentId);
        m.setStudentId(studentId);
        m.setValue(value);
        manualGradeRepository.save(m);
    }

    // ---------- compute ----------

    @Transactional
    public ComputeResult compute(UUID courseId, boolean save) {
        requireCanGrade(courseId);
        UUID me = currentUser.requireUserId();
        GradingScheme scheme = schemeRepository.findByCourseIdAndProfessorId(courseId, me)
                .orElseThrow(() -> new EntityNotFoundException("Schema de notare nu e definită."));
        List<GradingComponent> components = componentRepository.findBySchemeIdOrderBySortOrderAsc(scheme.getId());
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Schema nu are componente.");
        }

        boolean hasDocument = components.stream().anyMatch(c -> "document".equals(c.getSource()));

        // sheet index: normalized identifier -> row
        Map<String, Map<String, String>> sheetIndex = new HashMap<>();
        List<String> allKeys = new ArrayList<>();
        if (hasDocument && scheme.getSheetUrl() != null && !scheme.getSheetUrl().isBlank()
                && scheme.getMatchColumn() != null) {
            GoogleSheetReader.SheetData data = sheetReader.read(scheme.getSheetUrl());
            for (Map<String, String> row : data.rows()) {
                String key = norm(row.get(scheme.getMatchColumn()));
                if (!key.isEmpty()) {
                    sheetIndex.put(key, row);
                    allKeys.add(key);
                }
            }
        }

        // manual grades: componentId|studentId -> value
        List<UUID> compIds = components.stream().map(GradingComponent::getId).toList();
        Map<String, Double> manualMap = new HashMap<>();
        for (ManualGrade m : manualGradeRepository.findByComponentIdIn(compIds)) {
            manualMap.put(m.getComponentId() + "|" + m.getStudentId(), m.getValue());
        }

        // One row per student: grade only their LATEST enrollment for this course.
        // A student who failed in a past year and re-enrolled has multiple rows;
        // writing to all of them would rewrite the historical fail (and silently
        // resolve the carried restanță), so historical rows are never touched.
        Map<UUID, Enrollment> latestByStudent = new LinkedHashMap<>();
        for (Enrollment e : enrollmentRepository.findByCourseIdWithStudent(courseId)) {
            Enrollment prev = latestByStudent.get(e.getStudentId());
            if (prev == null || isLater(e, prev)) {
                latestByStudent.put(e.getStudentId(), e);
            }
        }
        List<ComputeRow> rows = new ArrayList<>();
        java.util.Set<String> matchedKeys = new java.util.HashSet<>();

        for (Enrollment e : latestByStudent.values()) {
            Profile p = e.getStudent();
            String name = p != null ? p.getFullName() : "—";
            String key = norm(matchValue(scheme.getMatchField(), p));
            Map<String, String> sheetRow = key.isEmpty() ? null : sheetIndex.get(key);
            if (sheetRow != null) {
                matchedKeys.add(key);
            }

            Map<String, Double> values = new LinkedHashMap<>();
            double weightedSum = 0, weightTotal = 0, bonus = 0;
            boolean anyValue = false;
            boolean perCriterionOk = true;

            for (GradingComponent c : components) {
                Double v;
                if ("manual".equals(c.getSource())) {
                    v = manualMap.get(c.getId() + "|" + e.getStudentId());
                } else {
                    v = sheetRow == null ? null : averageColumns(sheetRow, c.getSheetColumns());
                }
                values.put(c.getName(), v == null ? null : round2(v));
                if (v != null) {
                    anyValue = true;
                    double w = c.getWeight() == null ? 0 : c.getWeight();
                    if (Boolean.TRUE.equals(c.getIsBonus())) {
                        bonus += v * w / 100.0;
                    } else {
                        weightedSum += v * w;
                        weightTotal += w;
                    }
                }
                if (c.getMinThreshold() != null && (v == null || v < c.getMinThreshold())) {
                    perCriterionOk = false;
                }
            }

            double base = weightTotal > 0 ? weightedSum / weightTotal : 0;
            double finalRaw = Math.max(0, Math.min(10, base + bonus));
            double finalStored = Boolean.TRUE.equals(scheme.getRoundUp())
                    ? Math.round(finalRaw) : round2(finalRaw);

            // Pass is judged on the STORED grade (post-rounding): a 4.6 that
            // rounds up to a stored 5 must not be reported as failed while every
            // other consumer (grades page, media, restanțe, taxe) treats 5 as passed.
            boolean passed;
            double threshold = scheme.getPassThreshold() == null ? 5.0 : scheme.getPassThreshold();
            if ("per_criterion".equals(scheme.getPassMode())) {
                passed = perCriterionOk && finalStored >= threshold;
            } else {
                passed = finalStored >= threshold;
            }

            String note = !anyValue ? "Fără note"
                    : (hasDocument && sheetRow == null ? "Nepotrivit în sheet" : null);

            rows.add(new ComputeRow(e.getStudentId(), name, sheetRow != null,
                    values, round2(base), round2(bonus), round2(finalRaw), finalStored, passed, note));

            if (save && anyValue) {
                // Managed entity — dirty checking flushes the (batched) update at
                // commit; no per-row save() round-trip.
                e.setFinalGrade(finalStored);
                e.setGradeBreakdown(buildBreakdown(scheme, components, values, base, bonus, finalRaw, finalStored, passed));
            }
        }

        List<String> unmatched = allKeys.stream().filter(k -> !matchedKeys.contains(k)).distinct().toList();
        return new ComputeResult(rows, unmatched, save);
    }

    // ---------- helpers ----------

    /** True when a is a later enrollment than b (academic year, then semester). */
    private static boolean isLater(Enrollment a, Enrollment b) {
        // Years are "YYYY-YYYY" strings, so lexicographic order is chronological.
        int byYear = nullSafe(a.getAcademicYear()).compareTo(nullSafe(b.getAcademicYear()));
        if (byYear != 0) {
            return byYear > 0;
        }
        int sa = a.getSemester() == null ? 0 : a.getSemester();
        int sb = b.getSemester() == null ? 0 : b.getSemester();
        return sa > sb;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private Map<String, Object> buildBreakdown(GradingScheme scheme, List<GradingComponent> components,
                                               Map<String, Double> values, double base, double bonus,
                                               double finalRaw, double finalStored, boolean passed) {
        List<Map<String, Object>> comps = new ArrayList<>();
        for (GradingComponent c : components) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("weight", c.getWeight());
            m.put("is_bonus", Boolean.TRUE.equals(c.getIsBonus()));
            m.put("value", values.get(c.getName()));
            comps.add(m);
        }
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("components", comps);
        b.put("base", round2(base));
        b.put("bonus", round2(bonus));
        b.put("final", round2(finalRaw));
        b.put("stored", finalStored);
        b.put("passed", passed);
        b.put("threshold", scheme.getPassThreshold());
        b.put("pass_mode", scheme.getPassMode());
        b.put("round_up", scheme.getRoundUp());
        return b;
    }

    private static Double averageColumns(Map<String, String> row, List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return null;
        }
        double sum = 0;
        int n = 0;
        for (String col : columns) {
            Double v = parseNum(row.get(col));
            if (v != null) {
                sum += v;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    private static Double parseNum(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String matchValue(String field, Profile p) {
        if (p == null) {
            return "";
        }
        return switch (field == null ? "student_id" : field) {
            case "email" -> p.getEmail();
            case "full_name" -> p.getFullName();
            default -> p.getStudentId();
        };
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ComponentDto toDto(GradingComponent c) {
        return new ComponentDto(c.getId(), c.getName(), c.getWeight(), c.getIsBonus(),
                c.getMinThreshold(), c.getSource(), c.getSheetColumns(), c.getSortOrder());
    }
}
