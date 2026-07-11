package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Specialization;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.SpecializationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reference data for the student group cascade: the specializations table and
 * the groups derived from existing {@code group_name} codes. The code encodes
 * {@code <specCode><year><group>[/<semigroup>]}, so the spec code is the pre-slash
 * part minus its last two digits.
 */
@Service
public class AcademicMetaService {

    private final SpecializationRepository specializationRepository;
    private final ProfileRepository profileRepository;

    public AcademicMetaService(SpecializationRepository specializationRepository,
                               ProfileRepository profileRepository) {
        this.specializationRepository = specializationRepository;
        this.profileRepository = profileRepository;
    }

    /** One selectable group: its full pre-slash code, the derived spec code + year, and its semigroups. */
    public record GroupDto(String code, String specCode, Integer year, List<String> semigroups) {}

    @Transactional(readOnly = true)
    public List<Specialization> specializations() {
        return specializationRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<GroupDto> groups() {
        Map<String, TreeSet<String>> semisByGroup = new TreeMap<>();
        Map<String, String[]> parsed = new HashMap<>(); // pre -> [specCode, year]

        for (String g : profileRepository.findDistinctGroupNames()) {
            String[] parts = g.split("/", 2);
            String pre = parts[0].trim();
            if (pre.length() < 3) {
                continue; // need at least specCode(1) + year(1) + group(1)
            }
            String specCode = pre.substring(0, pre.length() - 2);
            String year = String.valueOf(pre.charAt(pre.length() - 2));
            semisByGroup.computeIfAbsent(pre, k -> new TreeSet<>());
            if (parts.length > 1 && !parts[1].isBlank()) {
                semisByGroup.get(pre).add(parts[1].trim());
            }
            parsed.put(pre, new String[] {specCode, year});
        }

        List<GroupDto> out = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : semisByGroup.entrySet()) {
            String[] pv = parsed.get(e.getKey());
            Integer year = null;
            try {
                year = Integer.parseInt(pv[1]);
            } catch (NumberFormatException ignored) {
                // leave year null if the digit isn't numeric
            }
            out.add(new GroupDto(e.getKey(), pv[0], year, new ArrayList<>(e.getValue())));
        }
        return out;
    }
}
