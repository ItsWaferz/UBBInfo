package ro.ubbcluj.ubbinfo.util;

/** Tiny shared string helpers (previously copy-pasted per service). */
public final class Strings {

    private Strings() {}

    /** Trimmed value, or null when null/blank. */
    public static String blankToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
