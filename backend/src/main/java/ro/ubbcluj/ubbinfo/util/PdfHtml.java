package ro.ubbcluj.ubbinfo.util;

/**
 * Shared fragments for the XHTML→PDF templates (documents + facility lists).
 * One escaping rule and one UBB letterhead — previously copy-pasted per
 * template family and already drifting.
 */
public final class PdfHtml {

    private PdfHtml() {}

    /** The UBB letterhead block; templates style .antet/.u/.motto themselves. */
    public static final String ANTET = """
        <div class="antet">
          <div class="u">UNIVERSITATEA BABEŞ-BOLYAI<br/>BABEŞ-BOLYAI TUDOMÁNYEGYETEM<br/>BABEŞ-BOLYAI UNIVERSITÄT<br/>BABEŞ-BOLYAI UNIVERSITY</div>
          <div class="motto">TRADITIO ET EXCELLENTIA</div>
        </div>
        """;

    /** Escape text for XHTML (incl. quotes; newlines become &lt;br/&gt;). */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br/>");
    }
}
