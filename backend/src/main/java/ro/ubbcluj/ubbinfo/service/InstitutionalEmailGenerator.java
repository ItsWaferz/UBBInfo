package ro.ubbcluj.ubbinfo.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Builds @stud.ubbcluj.ro login emails with the faculty's collision rules:
 * <ol>
 *   <li>{@code firstGiven.family}</li>
 *   <li>on collision, add the second given name: {@code g1.g2.family}</li>
 *   <li>then rotate the given names: {@code g2.family}, {@code g2.g1.family}, …</li>
 *   <li>then move the family-name position: {@code family.g1}, …</li>
 *   <li>only then append numbers: {@code g1.family2}, {@code g1.family3}, …</li>
 * </ol>
 */
public final class InstitutionalEmailGenerator {

    public static final String DOMAIN = "stud.ubbcluj.ro";

    private InstitutionalEmailGenerator() {
    }

    /** Lowercase, strip Romanian diacritics, keep only a–z0–9. */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    /** Ordered local-part candidates (escalating), before the @domain. */
    public static List<String> candidates(String family, List<String> givenRaw) {
        String fam = normalize(family);
        List<String> givens = new ArrayList<>();
        for (String g : givenRaw) {
            String n = normalize(g);
            if (!n.isEmpty()) {
                givens.add(n);
            }
        }
        if (givens.isEmpty()) {
            givens.add(fam.isEmpty() ? "user" : fam);
        }

        // Given-name arrangements, in the faculty's escalation order.
        LinkedHashSet<List<String>> arrangements = new LinkedHashSet<>();
        arrangements.add(List.of(givens.get(0)));                       // g1
        if (givens.size() >= 2) {
            arrangements.add(List.of(givens.get(0), givens.get(1)));    // g1.g2
        }
        for (String g : givens) {                                       // rotate: each single given
            arrangements.add(List.of(g));
        }
        permutations(givens, new ArrayList<>(), arrangements);          // full permutations

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (boolean familyFirst : new boolean[] {false, true}) {
            for (List<String> arr : arrangements) {
                List<String> parts = new ArrayList<>();
                if (familyFirst) {
                    parts.add(fam);
                    parts.addAll(arr);
                } else {
                    parts.addAll(arr);
                    parts.add(fam);
                }
                String local = String.join(".", parts);
                if (!local.isBlank()) {
                    out.add(local);
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** First free email per the rules; appends numbers to the primary base if all taken. */
    public static String generate(String family, List<String> given, Predicate<String> taken) {
        List<String> cands = candidates(family, given);
        for (String c : cands) {
            String email = c + "@" + DOMAIN;
            if (!taken.test(email)) {
                return email;
            }
        }
        String base = cands.isEmpty() ? "user" : cands.get(0);
        for (int n = 2; ; n++) {
            String email = base + n + "@" + DOMAIN;
            if (!taken.test(email)) {
                return email;
            }
        }
    }

    private static void permutations(List<String> items, List<String> acc, LinkedHashSet<List<String>> out) {
        if (items.size() > 4) {
            return; // guard against factorial blow-up for unusual long name lists
        }
        if (items.isEmpty()) {
            if (!acc.isEmpty()) {
                out.add(List.copyOf(acc));
            }
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            List<String> rest = new ArrayList<>(items);
            String picked = rest.remove(i);
            acc.add(picked);
            permutations(rest, acc, out);
            acc.remove(acc.size() - 1);
        }
    }
}
