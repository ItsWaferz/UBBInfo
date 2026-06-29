package ro.ubbcluj.ubbinfo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a Google Sheet (shared "anyone with the link can view") via its CSV
 * export endpoint. Blank header cells are named "Coloana N" so they can still
 * be referenced; fully blank rows are skipped.
 */
@Component
public class GoogleSheetReader {

    private static final Pattern ID = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID = Pattern.compile("[?#&]gid=([0-9]+)");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record SheetData(List<String> headers, List<Map<String, String>> rows) {
    }

    public SheetData read(String sheetUrl) {
        if (sheetUrl == null || sheetUrl.isBlank()) {
            throw new IllegalArgumentException("Link-ul către Google Sheet lipsește.");
        }
        Matcher idm = ID.matcher(sheetUrl);
        if (!idm.find()) {
            throw new IllegalArgumentException("Link Google Sheets invalid.");
        }
        String id = idm.group(1);
        Matcher gidm = GID.matcher(sheetUrl);
        String gid = gidm.find() ? gidm.group(1) : "0";
        String exportUrl = "https://docs.google.com/spreadsheets/d/" + id
                + "/export?format=csv&gid=" + gid;

        String csv;
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(exportUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalArgumentException(
                        "Nu pot citi sheet-ul (HTTP " + resp.statusCode()
                                + "). Verifică partajarea: „Oricine cu link-ul poate vedea\".");
            }
            csv = resp.body();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Eroare la citirea sheet-ului: " + e.getMessage());
        }

        return parse(csv);
    }

    private SheetData parse(String csv) {
        List<CSVRecord> records;
        try (CSVParser parser = CSVParser.parse(csv, CSVFormat.DEFAULT)) {
            records = parser.getRecords();
        } catch (Exception e) {
            throw new IllegalArgumentException("CSV invalid: " + e.getMessage());
        }
        if (records.isEmpty()) {
            return new SheetData(List.of(), List.of());
        }

        CSVRecord headerRec = records.get(0);
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRec.size(); i++) {
            String h = headerRec.get(i).trim();
            if (h.isEmpty()) {
                h = "Coloana " + (i + 1);
            }
            // de-duplicate header names
            String base = h;
            int dup = 2;
            while (headers.contains(h)) {
                h = base + " (" + dup++ + ")";
            }
            headers.add(h);
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int r = 1; r < records.size(); r++) {
            CSVRecord rec = records.get(r);
            Map<String, String> row = new LinkedHashMap<>();
            boolean allBlank = true;
            for (int i = 0; i < headers.size(); i++) {
                String v = i < rec.size() ? rec.get(i).trim() : "";
                row.put(headers.get(i), v);
                if (!v.isEmpty()) {
                    allBlank = false;
                }
            }
            if (!allBlank) {
                rows.add(row);
            }
        }
        return new SheetData(headers, rows);
    }
}
