package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Specialization;
import ro.ubbcluj.ubbinfo.entity.StudyGroup;
import ro.ubbcluj.ubbinfo.repository.SpecializationRepository;
import ro.ubbcluj.ubbinfo.repository.StudyGroupRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference data for the student group cascade: the specializations table and
 * the authoritative groups catalog. The group {@code code} encodes
 * {@code <specCode><year><group>}, and each group splits into 1–2 semigroups
 * (expanded here to "1".."N").
 */
@Service
public class AcademicMetaService {

    private final SpecializationRepository specializationRepository;
    private final StudyGroupRepository studyGroupRepository;

    public AcademicMetaService(SpecializationRepository specializationRepository,
                               StudyGroupRepository studyGroupRepository) {
        this.specializationRepository = specializationRepository;
        this.studyGroupRepository = studyGroupRepository;
    }

    /** One selectable group: its code, its spec code + year, and its semigroups ("1".."N"). */
    public record GroupDto(String code, String specCode, Integer year, List<String> semigroups) {}

    @Transactional(readOnly = true)
    public List<Specialization> specializations() {
        return specializationRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<GroupDto> groups() {
        List<GroupDto> out = new ArrayList<>();
        for (StudyGroup g : studyGroupRepository.findAllByOrderByCodeAsc()) {
            int n = g.getSemigroups() == null ? 1 : Math.max(g.getSemigroups(), 1);
            List<String> semis = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                semis.add(String.valueOf(i));
            }
            out.add(new GroupDto(g.getCode(), g.getSpecCode(), g.getStudyYear(), semis));
        }
        return out;
    }
}
