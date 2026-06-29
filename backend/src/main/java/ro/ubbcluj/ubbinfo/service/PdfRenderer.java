package ro.ubbcluj.ubbinfo.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Renders an XHTML+CSS string to a PDF byte array via OpenHTMLtoPDF (PDFBox).
 * Bundles Liberation Serif (Times-metric-compatible, OFL) so Romanian
 * diacritics (ș, ț, ă, â, î) render correctly — the PDF base-14 fonts cannot.
 */
@Component
public class PdfRenderer {

    private static final String FAMILY = "Liberation Serif";

    private final byte[] regular = load("fonts/LiberationSerif-Regular.ttf");
    private final byte[] bold = load("fonts/LiberationSerif-Bold.ttf");
    private final byte[] italic = load("fonts/LiberationSerif-Italic.ttf");
    private final byte[] boldItalic = load("fonts/LiberationSerif-BoldItalic.ttf");

    /** Render well-formed XHTML to PDF bytes. */
    public byte[] render(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            builder.useFont(() -> new ByteArrayInputStream(regular), FAMILY, 400,
                    BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> new ByteArrayInputStream(bold), FAMILY, 700,
                    BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> new ByteArrayInputStream(italic), FAMILY, 400,
                    BaseRendererBuilder.FontStyle.ITALIC, true);
            builder.useFont(() -> new ByteArrayInputStream(boldItalic), FAMILY, 700,
                    BaseRendererBuilder.FontStyle.ITALIC, true);

            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF: " + e.getMessage(), e);
        }
    }

    private static byte[] load(String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Missing bundled resource: " + classpath, e);
        }
    }
}
