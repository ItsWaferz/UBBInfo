package ro.ubbcluj.ubbinfo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ApplicantRow;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ApplyRequest;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.FacilityOverviewDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.GenerateResult;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Dorm;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.FacilityApplication;
import ro.ubbcluj.ubbinfo.entity.FacilitySetting;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.repository.DormRepository;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.FacilityApplicationRepository;
import ro.ubbcluj.ubbinfo.repository.FacilityPublicationRepository;
import ro.ubbcluj.ubbinfo.repository.FacilitySettingRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature #5 — FacilityService allocation engine + apply/withdraw/publish freeze.
 *
 * The allocation logic is the interesting part: rank by media, a reserved-quota
 * FLOOR for flagged (social/special) students, dorm-preference assignment with
 * fallback, and publish freezing. Media is computed by the shared static
 * {@link AcademicAverageService#media}: a single non-optional course with N
 * credits and finalGrade=G yields media=G, which we use to place applicants at
 * exact medias.
 */
@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock DormRepository dormRepository;
    @Mock FacilitySettingRepository settingRepository;
    @Mock FacilityApplicationRepository appRepository;
    @Mock FacilityPublicationRepository publicationRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock CurrentUserService currentUser;
    @Mock PdfRenderer pdfRenderer;

    @InjectMocks FacilityService service;

    // Applications + their enrollments accumulate here; wiring() feeds the mocks.
    private final List<FacilityApplication> apps = new ArrayList<>();
    private final Map<UUID, List<Enrollment>> enrollments = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(currentUser).requireAdmin();
    }

    // ---------------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------------

    /** Add an applicant with the given media (via one 6-credit course) and flags. */
    private FacilityApplication applicant(String facility, String code, Double media,
                                          boolean social, boolean special, List<String> dormPrefs) {
        UUID sid = UUID.randomUUID();
        Profile p = new Profile();
        p.setStudentId(code);
        p.setFullName("Student " + code);
        p.setIsSocialCase(social);
        p.setIsSpecialCase(special);

        FacilityApplication a = new FacilityApplication();
        a.setId(UUID.randomUUID());
        a.setStudentId(sid);
        a.setFacility(facility);
        a.setStatus("pending");
        a.setStudent(p);
        a.setDormPrefs(dormPrefs);
        a.setCreatedAt(OffsetDateTime.now());
        apps.add(a);

        List<Enrollment> es = new ArrayList<>();
        if (media != null) {
            Course c = new Course();
            c.setId(UUID.randomUUID());
            c.setCredits(6);
            c.setIsOptional(false);
            Enrollment e = new Enrollment();
            e.setStudentId(sid);
            e.setCourse(c);
            e.setFinalGrade(media);
            es.add(e);
        }
        enrollments.put(sid, es);
        return a;
    }

    private FacilityApplication applicant(String facility, String code, Double media) {
        return applicant(facility, code, media, false, false, List.of());
    }

    /** Point the repos at the accumulated applicants/enrollments for `facility`. */
    private void wireAllocation(String facility) {
        List<FacilityApplication> forFac = apps.stream()
                .filter(a -> a.getFacility().equals(facility)).collect(Collectors.toList());
        when(appRepository.findByFacilityWithStudent(facility)).thenReturn(forFac);
        when(enrollmentRepository.findByStudentIdInWithCourse(any(Collection.class)))
                .thenAnswer(inv -> {
                    Collection<UUID> ids = inv.getArgument(0);
                    List<Enrollment> out = new ArrayList<>();
                    for (UUID id : ids) out.addAll(enrollments.getOrDefault(id, List.of()));
                    return out;
                });
    }

    private void setting(String key, Integer capacity, Double reservedPct) {
        FacilitySetting s = new FacilitySetting();
        s.setKey(key);
        s.setLabel(key);
        s.setCapacity(capacity);
        s.setReservedPercent(reservedPct == null ? null : BigDecimal.valueOf(reservedPct));
        when(settingRepository.findById(key)).thenReturn(Optional.of(s));
    }

    private Dorm dorm(String name, int capacity, int sort) {
        Dorm d = new Dorm();
        d.setId(UUID.randomUUID());
        d.setName(name);
        d.setCapacity(capacity);
        d.setSortOrder(sort);
        d.setActive(true);
        return d;
    }

    private void dorms(Dorm... ds) {
        lenient().when(dormRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(ds));
    }

    private static ApplicantRow row(GenerateResult r, String code) {
        return r.rows().stream().filter(x -> code.equals(x.code())).findFirst().orElseThrow();
    }

    private static List<String> acceptedCodes(GenerateResult r) {
        return r.rows().stream().filter(x -> "accepted".equals(x.status()))
                .map(ApplicantRow::code).collect(Collectors.toList());
    }

    // =====================================================================
    // Reserved-quota FLOOR (the fix)
    // =====================================================================
    @Nested
    @DisplayName("Reserved floor for flagged applicants")
    class ReservedFloor {

        @Test
        @DisplayName("flagged with high media enters on merit and does NOT consume the reserved slot")
        void highMediaFlaggedDoesNotWasteQuota() {
            // tabara: flagging comes from is_special_case.
            // cap=5, reserved 40% -> ceil(2). flagged medias 9,4,3; non-flagged 8,7,6,5,4.5
            setting(FacilityService.TABARA, 5, 40.0);
            applicant(FacilityService.TABARA, "F9", 9.0, false, true, List.of());
            applicant(FacilityService.TABARA, "F4", 4.0, false, true, List.of());
            applicant(FacilityService.TABARA, "F3", 3.0, false, true, List.of());
            applicant(FacilityService.TABARA, "N8", 8.0);
            applicant(FacilityService.TABARA, "N7", 7.0);
            applicant(FacilityService.TABARA, "N6", 6.0);
            applicant(FacilityService.TABARA, "N5", 5.0);
            applicant(FacilityService.TABARA, "N45", 4.5);
            wireAllocation(FacilityService.TABARA);

            GenerateResult r = service.generate(FacilityService.TABARA, 0);

            // 2 flagged total guaranteed: F9 (on merit) + F4 (promoted). F3 stays out.
            assertEquals(5, acceptedCodes(r).size());
            assertTrue(acceptedCodes(r).containsAll(List.of("F9", "N8", "N7", "F4")));
            assertFalse(acceptedCodes(r).contains("F3"));
            // F9 entered on merit -> not reserved; F4 was promoted -> reserved.
            assertFalse(row(r, "F9").reserved());
            assertTrue(row(r, "F4").reserved());
            // The displaced victim is the weakest admitted non-flagged (N5), not N6.
            assertFalse(acceptedCodes(r).contains("N5"));
            assertTrue(acceptedCodes(r).contains("N6"));
        }

        @Test
        @DisplayName("no deficit when enough flagged already made the merit cut")
        void noDeficitWhenFlaggedInMerit() {
            setting(FacilityService.TABARA, 3, 50.0); // floor ceil(1.5)=2
            applicant(FacilityService.TABARA, "F9", 9.0, false, true, List.of());
            applicant(FacilityService.TABARA, "F8", 8.0, false, true, List.of());
            applicant(FacilityService.TABARA, "N7", 7.0);
            applicant(FacilityService.TABARA, "N6", 6.0);
            wireAllocation(FacilityService.TABARA);

            GenerateResult r = service.generate(FacilityService.TABARA, 0);

            // F9,F8 (flagged, merit) + N7. Floor of 2 flagged already satisfied.
            assertEquals(List.of("F9", "F8", "N7"), acceptedCodes(r));
            assertFalse(row(r, "F9").reserved()); // nobody promoted
            assertFalse(row(r, "F8").reserved());
        }

        @Test
        @DisplayName("no flagged, no reserved config -> pure merit")
        void pureMeritWhenNoFlags() {
            setting(FacilityService.BURSA_MERIT, 2, 0.0);
            applicant(FacilityService.BURSA_MERIT, "A9", 9.0);
            applicant(FacilityService.BURSA_MERIT, "B7", 7.0);
            applicant(FacilityService.BURSA_MERIT, "C5", 5.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(List.of("A9", "B7"), acceptedCodes(r));
        }

        @Test
        @DisplayName("floor only partially met when there aren't enough flagged")
        void floorPartialWhenNotEnoughFlagged() {
            setting(FacilityService.TABARA, 4, 75.0); // floor ceil(3)=3
            applicant(FacilityService.TABARA, "F2", 2.0, false, true, List.of());
            applicant(FacilityService.TABARA, "N9", 9.0);
            applicant(FacilityService.TABARA, "N8", 8.0);
            applicant(FacilityService.TABARA, "N7", 7.0);
            applicant(FacilityService.TABARA, "N6", 6.0);
            applicant(FacilityService.TABARA, "N5", 5.0);
            wireAllocation(FacilityService.TABARA);

            GenerateResult r = service.generate(FacilityService.TABARA, 0);
            // Only 1 flagged exists -> promoted (displacing weakest merit seat);
            // floor of 3 can't be reached. cap=4.
            assertEquals(4, acceptedCodes(r).size());
            assertTrue(acceptedCodes(r).contains("F2"));
            assertTrue(row(r, "F2").reserved());
            // F2 was NOT in the merit top-4 (N9,N8,N7,N6); it was promoted.
            assertFalse(acceptedCodes(r).contains("N6"));
        }

        @Test
        @DisplayName("reserved count uses ceil rounding of the percentage")
        void reservedCeilRounding() {
            // cap=10, 10% -> ceil(1.0)=1 reserved seat guaranteed
            setting(FacilityService.TABARA, 10, 10.0);
            // 1 low-media flagged + 12 strong non-flagged
            applicant(FacilityService.TABARA, "Flow", 3.0, false, true, List.of());
            for (int i = 0; i < 12; i++) {
                applicant(FacilityService.TABARA, "N" + i, 9.0 - i * 0.1);
            }
            wireAllocation(FacilityService.TABARA);

            GenerateResult r = service.generate(FacilityService.TABARA, 0);
            assertEquals(10, acceptedCodes(r).size());
            // The single flagged is guaranteed despite the lowest media.
            assertTrue(acceptedCodes(r).contains("Flow"));
            assertTrue(row(r, "Flow").reserved());
        }

        @Test
        @DisplayName("camin uses is_social_case; tabara uses is_special_case for flagging")
        void flagSourcePerFacility() {
            // For tabara, a social-only case is NOT flagged.
            setting(FacilityService.TABARA, 1, 100.0);
            applicant(FacilityService.TABARA, "SocOnly", 3.0, true, false, List.of());
            applicant(FacilityService.TABARA, "Merit", 9.0, false, false, List.of());
            wireAllocation(FacilityService.TABARA);

            GenerateResult r = service.generate(FacilityService.TABARA, 0);
            // floor=1 but the only flagged-for-tabara is nobody (SocOnly is social,
            // not special) -> pure merit -> Merit wins.
            assertEquals(List.of("Merit"), acceptedCodes(r));
        }
    }

    // =====================================================================
    // Ranking / media edge cases
    // =====================================================================
    @Nested
    @DisplayName("Ranking and media")
    class Ranking {

        @Test
        @DisplayName("ranks are assigned in descending media order")
        void ranksByMediaDesc() {
            setting(FacilityService.BURSA_MERIT, 3, 0.0);
            applicant(FacilityService.BURSA_MERIT, "Mid", 7.0);
            applicant(FacilityService.BURSA_MERIT, "Top", 9.0);
            applicant(FacilityService.BURSA_MERIT, "Low", 5.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(1, row(r, "Top").rank());
            assertEquals(2, row(r, "Mid").rank());
            assertEquals(3, row(r, "Low").rank());
        }

        @Test
        @DisplayName("applicant with no graded courses ranks last with null media")
        void nullMediaRanksLast() {
            setting(FacilityService.BURSA_MERIT, 2, 0.0);
            applicant(FacilityService.BURSA_MERIT, "Graded", 6.0);
            applicant(FacilityService.BURSA_MERIT, "NoGrades", null);
            applicant(FacilityService.BURSA_MERIT, "Higher", 8.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(List.of("Higher", "Graded"), acceptedCodes(r));
            assertNull(row(r, "NoGrades").media());
            assertEquals("rejected", row(r, "NoGrades").status());
        }

        @Test
        @DisplayName("equal medias break ties by code for a stable order")
        void tieBreakByCode() {
            setting(FacilityService.BURSA_MERIT, 1, 0.0);
            applicant(FacilityService.BURSA_MERIT, "BBB", 8.0);
            applicant(FacilityService.BURSA_MERIT, "AAA", 8.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(List.of("AAA"), acceptedCodes(r)); // AAA < BBB
        }

        @Test
        @DisplayName("media is reported rounded to 2 decimals")
        void mediaRoundedTwoDecimals() {
            setting(FacilityService.BURSA_MERIT, 1, 0.0);
            applicant(FacilityService.BURSA_MERIT, "X", 8.5);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(0, new BigDecimal("8.50").compareTo(row(r, "X").media()));
        }
    }

    // =====================================================================
    // Camin dorm assignment
    // =====================================================================
    @Nested
    @DisplayName("Camin dorm assignment")
    class CaminDorms {

        @Test
        @DisplayName("first preferred dorm with free capacity is assigned")
        void firstPreferenceHonored() {
            Dorm d1 = dorm("Hasdeu", 1, 0);
            Dorm d2 = dorm("Economica", 1, 1);
            dorms(d1, d2);
            setting(FacilityService.CAMIN, 0, 0.0);
            applicant(FacilityService.CAMIN, "Pref2", 9.0, false, false,
                    List.of(d2.getId().toString()));
            wireAllocation(FacilityService.CAMIN);

            GenerateResult r = service.generate(FacilityService.CAMIN, 0);
            assertEquals("Economica", row(r, "Pref2").result());
        }

        @Test
        @DisplayName("when preferred dorm is full, falls back to any free dorm")
        void fallbackWhenPreferenceFull() {
            Dorm d1 = dorm("Hasdeu", 1, 0);
            Dorm d2 = dorm("Economica", 5, 1);
            dorms(d1, d2);
            setting(FacilityService.CAMIN, 0, 0.0);
            // Both prefer Hasdeu; higher media takes it, the other falls back.
            applicant(FacilityService.CAMIN, "High", 9.0, false, false,
                    List.of(d1.getId().toString()));
            applicant(FacilityService.CAMIN, "Low", 6.0, false, false,
                    List.of(d1.getId().toString()));
            wireAllocation(FacilityService.CAMIN);

            GenerateResult r = service.generate(FacilityService.CAMIN, 0);
            assertEquals("Hasdeu", row(r, "High").result());
            assertEquals("Economica", row(r, "Low").result()); // fallback
        }

        @Test
        @DisplayName("camin capacity is capped at total dorm capacity")
        void capacityCappedAtDormTotal() {
            dorms(dorm("A", 1, 0), dorm("B", 1, 1)); // total 2
            setting(FacilityService.CAMIN, 0, 0.0);
            applicant(FacilityService.CAMIN, "S1", 9.0);
            applicant(FacilityService.CAMIN, "S2", 8.0);
            applicant(FacilityService.CAMIN, "S3", 7.0);
            wireAllocation(FacilityService.CAMIN);

            // x=99 requested but only 2 beds exist.
            GenerateResult r = service.generate(FacilityService.CAMIN, 99);
            assertEquals(2, acceptedCodes(r).size());
            assertEquals(List.of("S1", "S2"), acceptedCodes(r));
        }

        @Test
        @DisplayName("invalid UUID in dorm prefs is skipped, falls through to any free dorm")
        void invalidUuidPrefSkipped() {
            Dorm d1 = dorm("Hasdeu", 5, 0);
            dorms(d1);
            setting(FacilityService.CAMIN, 0, 0.0);
            applicant(FacilityService.CAMIN, "Bad", 9.0, false, false,
                    List.of("not-a-uuid"));
            wireAllocation(FacilityService.CAMIN);

            GenerateResult r = service.generate(FacilityService.CAMIN, 0);
            assertEquals("Hasdeu", row(r, "Bad").result());
        }

        @Test
        @DisplayName("social case gets reserved camin seat via floor")
        void socialCaseReservedCamin() {
            dorms(dorm("A", 10, 0));
            setting(FacilityService.CAMIN, 1, 100.0); // cap capped to 1 by x, floor=1
            applicant(FacilityService.CAMIN, "Social", 3.0, true, false,
                    List.of());
            applicant(FacilityService.CAMIN, "Merit", 9.0, false, false,
                    List.of());
            wireAllocation(FacilityService.CAMIN);

            GenerateResult r = service.generate(FacilityService.CAMIN, 1);
            assertEquals(List.of("Social"), acceptedCodes(r));
            assertTrue(row(r, "Social").reserved());
        }
    }

    // =====================================================================
    // apply / withdraw / freeze
    // =====================================================================
    @Nested
    @DisplayName("Apply / withdraw / publish freeze")
    class ApplyWithdraw {

        private final UUID me = UUID.randomUUID();

        @Test
        @DisplayName("apply creates a pending application")
        void applyCreatesPending() {
            when(currentUser.isStudent()).thenReturn(true);
            when(currentUser.requireUserId()).thenReturn(me);
            when(appRepository.findByStudentIdAndFacility(me, FacilityService.BURSA_MERIT))
                    .thenReturn(Optional.empty());

            service.apply(FacilityService.BURSA_MERIT, new ApplyRequest(null));

            verify(appRepository).save(any(FacilityApplication.class));
        }

        @Test
        @DisplayName("non-student cannot apply")
        void nonStudentDenied() {
            when(currentUser.isStudent()).thenReturn(false);
            assertThrows(AccessDeniedException.class,
                    () -> service.apply(FacilityService.BURSA_MERIT, new ApplyRequest(null)));
            verify(appRepository, never()).save(any());
        }

        @Test
        @DisplayName("re-applying after results are published is blocked")
        void reapplyBlockedAfterPublish() {
            FacilityApplication decided = new FacilityApplication();
            decided.setStudentId(me);
            decided.setFacility(FacilityService.BURSA_MERIT);
            decided.setDecidedAt(OffsetDateTime.now());
            when(currentUser.isStudent()).thenReturn(true);
            when(currentUser.requireUserId()).thenReturn(me);
            when(appRepository.findByStudentIdAndFacility(me, FacilityService.BURSA_MERIT))
                    .thenReturn(Optional.of(decided));

            assertThrows(IllegalStateException.class,
                    () -> service.apply(FacilityService.BURSA_MERIT, new ApplyRequest(null)));
            verify(appRepository, never()).save(any());
        }

        @Test
        @DisplayName("withdraw after publish is blocked; the app is not deleted")
        void withdrawBlockedAfterPublish() {
            FacilityApplication decided = new FacilityApplication();
            decided.setStudentId(me);
            decided.setFacility(FacilityService.CAMIN);
            decided.setDecidedAt(OffsetDateTime.now());
            when(currentUser.requireUserId()).thenReturn(me);
            when(appRepository.findByStudentIdAndFacility(me, FacilityService.CAMIN))
                    .thenReturn(Optional.of(decided));

            assertThrows(IllegalStateException.class,
                    () -> service.withdraw(FacilityService.CAMIN));
            verify(appRepository, never()).delete(any());
        }

        @Test
        @DisplayName("withdraw before publish deletes the pending application")
        void withdrawDeletesPending() {
            FacilityApplication pending = new FacilityApplication();
            pending.setStudentId(me);
            pending.setFacility(FacilityService.CAMIN);
            when(currentUser.requireUserId()).thenReturn(me);
            when(appRepository.findByStudentIdAndFacility(me, FacilityService.CAMIN))
                    .thenReturn(Optional.of(pending));

            service.withdraw(FacilityService.CAMIN);
            verify(appRepository).delete(pending);
        }

        @Test
        @DisplayName("dorm prefs are only stored for camin, ignored for bursa")
        void dormPrefsOnlyForCamin() {
            when(currentUser.isStudent()).thenReturn(true);
            when(currentUser.requireUserId()).thenReturn(me);
            when(appRepository.findByStudentIdAndFacility(me, FacilityService.BURSA_MERIT))
                    .thenReturn(Optional.empty());

            service.apply(FacilityService.BURSA_MERIT,
                    new ApplyRequest(List.of(UUID.randomUUID().toString())));

            org.mockito.ArgumentCaptor<FacilityApplication> cap =
                    org.mockito.ArgumentCaptor.forClass(FacilityApplication.class);
            verify(appRepository).save(cap.capture());
            assertTrue(cap.getValue().getDormPrefs().isEmpty());
        }

        @Test
        @DisplayName("unknown facility is rejected")
        void unknownFacilityRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.apply("bursa_fictiva", new ApplyRequest(null)));
        }
    }

    // =====================================================================
    // publish
    // =====================================================================
    @Nested
    @DisplayName("Publish")
    class Publish {

        @Test
        @DisplayName("publish writes statuses, stamps decidedAt, and records a publication")
        void publishWritesStatuses() {
            setting(FacilityService.BURSA_MERIT, 1, 0.0);
            applicant(FacilityService.BURSA_MERIT, "Win", 9.0);
            applicant(FacilityService.BURSA_MERIT, "Lose", 5.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.publish(FacilityService.BURSA_MERIT, 0);

            assertEquals(1, r.accepted());
            FacilityApplication win = apps.stream().filter(a -> "Win".equals(a.getStudent().getStudentId()))
                    .findFirst().orElseThrow();
            FacilityApplication lose = apps.stream().filter(a -> "Lose".equals(a.getStudent().getStudentId()))
                    .findFirst().orElseThrow();
            assertEquals("accepted", win.getStatus());
            assertEquals("rejected", lose.getStatus());
            assertTrue(win.getDecidedAt() != null);
            assertTrue(lose.getDecidedAt() != null);
            assertNull(lose.getResult());
            assertNull(lose.getRank());
            verify(publicationRepository).save(any());
        }
    }

    // =====================================================================
    // overview
    // =====================================================================
    @Nested
    @DisplayName("Overview")
    class Overview {

        /** All 4 settings seeded with non-null capacities, as in production. */
        private List<FacilitySetting> allSettings() {
            List<FacilitySetting> out = new ArrayList<>();
            for (String key : List.of(FacilityService.CAMIN, FacilityService.TABARA,
                    FacilityService.BURSA_SOCIALA, FacilityService.BURSA_MERIT)) {
                FacilitySetting s = new FacilitySetting();
                s.setKey(key);
                s.setLabel(key);
                s.setCapacity(50);
                s.setReservedPercent(BigDecimal.ZERO);
                out.add(s);
            }
            return out;
        }

        @Test
        @DisplayName("overview counts applicants/accepted and flags published per facility")
        void overviewCounts() {
            // Two burse_merit apps, one already accepted+decided (published).
            FacilityApplication a1 = new FacilityApplication();
            a1.setFacility(FacilityService.BURSA_MERIT);
            a1.setStatus("accepted");
            a1.setDecidedAt(OffsetDateTime.now());
            FacilityApplication a2 = new FacilityApplication();
            a2.setFacility(FacilityService.BURSA_MERIT);
            a2.setStatus("rejected");
            a2.setDecidedAt(OffsetDateTime.now());
            when(appRepository.findAll()).thenReturn(List.of(a1, a2));
            when(settingRepository.findAll()).thenReturn(allSettings());
            lenient().when(dormRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());

            List<FacilityOverviewDto> ov = service.overview();
            FacilityOverviewDto m = ov.stream()
                    .filter(o -> o.key().equals(FacilityService.BURSA_MERIT)).findFirst().orElseThrow();
            assertEquals(2, m.applicants());
            assertEquals(1, m.accepted());
            assertTrue(m.published());
        }

        @Test
        @DisplayName("camin overview capacity reflects total dorm capacity")
        void caminCapacityIsDormTotal() {
            when(appRepository.findAll()).thenReturn(List.of());
            when(settingRepository.findAll()).thenReturn(allSettings());
            when(dormRepository.findByActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(dorm("A", 30, 0), dorm("B", 20, 1)));

            List<FacilityOverviewDto> ov = service.overview();
            FacilityOverviewDto camin = ov.stream()
                    .filter(o -> o.key().equals(FacilityService.CAMIN)).findFirst().orElseThrow();
            assertEquals(50, camin.capacity());
        }

        @Test
        @DisplayName("regression: overview does not NPE when a facility setting is missing")
        void overviewToleratesMissingSetting() {
            when(appRepository.findAll()).thenReturn(List.of());
            // No settings at all -> s==null for every facility. The mixed
            // int/Integer ternary used to unbox and NPE here.
            when(settingRepository.findAll()).thenReturn(List.of());
            when(dormRepository.findByActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(dorm("A", 40, 0)));

            List<FacilityOverviewDto> ov = service.overview();

            FacilityOverviewDto camin = ov.stream()
                    .filter(o -> o.key().equals(FacilityService.CAMIN)).findFirst().orElseThrow();
            assertEquals(40, camin.capacity());           // dorm total, still works
            FacilityOverviewDto tabara = ov.stream()
                    .filter(o -> o.key().equals(FacilityService.TABARA)).findFirst().orElseThrow();
            assertNull(tabara.capacity());                // no setting -> null, no NPE
        }
    }

    // =====================================================================
    // capacity resolution
    // =====================================================================
    @Nested
    @DisplayName("Capacity resolution")
    class Capacity {

        @Test
        @DisplayName("non-camin falls back to setting capacity when x<=0")
        void settingCapacityWhenXZero() {
            setting(FacilityService.BURSA_MERIT, 2, 0.0);
            applicant(FacilityService.BURSA_MERIT, "A", 9.0);
            applicant(FacilityService.BURSA_MERIT, "B", 8.0);
            applicant(FacilityService.BURSA_MERIT, "C", 7.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 0);
            assertEquals(2, acceptedCodes(r).size());
        }

        @Test
        @DisplayName("explicit x overrides the configured capacity")
        void explicitXOverridesSetting() {
            setting(FacilityService.BURSA_MERIT, 2, 0.0);
            applicant(FacilityService.BURSA_MERIT, "A", 9.0);
            applicant(FacilityService.BURSA_MERIT, "B", 8.0);
            applicant(FacilityService.BURSA_MERIT, "C", 7.0);
            wireAllocation(FacilityService.BURSA_MERIT);

            GenerateResult r = service.generate(FacilityService.BURSA_MERIT, 1);
            assertEquals(List.of("A"), acceptedCodes(r));
        }
    }
}
