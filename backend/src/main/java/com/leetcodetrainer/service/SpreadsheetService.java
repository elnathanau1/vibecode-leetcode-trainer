package com.leetcodetrainer.service;

import com.leetcodetrainer.dto.ImportResultDTO;
import com.leetcodetrainer.model.Attempt;
import com.leetcodetrainer.model.Problem;
import com.leetcodetrainer.repository.AttemptRepository;
import com.leetcodetrainer.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpreadsheetService {

    private final ProblemRepository problemRepository;
    private final AttemptRepository attemptRepository;

    private static final DateTimeFormatter DATE_FMT_MDY = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter DATE_FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Import ────────────────────────────────────────────────────────────────

    public ImportResultDTO importFromFile(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return parseAndImport(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    public ImportResultDTO importFromGoogleSheet(String sheetUrl) {
        try {
            String exportUrl = toExportUrl(sheetUrl);
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(exportUrl)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch sheet (HTTP " + response.statusCode() + "). Make sure the sheet is publicly accessible.");
            }
            return parseAndImport(new StringReader(response.body()));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch Google Sheet: " + e.getMessage(), e);
        }
    }

    private ImportResultDTO parseAndImport(Reader reader) {
        ImportResultDTO result = new ImportResultDTO();
        result.setNotFound(new ArrayList<>());
        result.setErrors(new ArrayList<>());

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    processRow(record, result);
                } catch (Exception e) {
                    result.setErrors(new ArrayList<>(result.getErrors()));
                    result.getErrors().add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV parse error: " + e.getMessage(), e);
        }
        return result;
    }

    private void processRow(CSVRecord record, ImportResultDTO result) {
        String problemName = safeGet(record, "Problem");
        if (problemName == null || problemName.isBlank()) return;

        String dateStr   = safeGet(record, "Date");
        String timeStr   = safeGet(record, "Time");
        String solutionStr = safeGet(record, "Solution");
        String tagsStr   = safeGet(record, "Tags");

        // Match problem by exact title first, then partial
        Optional<Problem> problemOpt = problemRepository.findByTitleIgnoreCase(problemName);
        if (problemOpt.isEmpty()) {
            List<Problem> candidates = problemRepository.findByTitleContainingIgnoreCase(problemName);
            if (!candidates.isEmpty()) problemOpt = Optional.of(candidates.get(0));
        }

        if (problemOpt.isEmpty()) {
            result.setNotFound(new ArrayList<>(result.getNotFound()));
            result.getNotFound().add(problemName);
            result.setSkipped(result.getSkipped() + 1);
            return;
        }

        Problem problem = problemOpt.get();
        LocalDateTime solvedAt = parseDate(dateStr);
        if (solvedAt == null) solvedAt = LocalDateTime.now();

        // Skip duplicate (same problem, same day)
        if (attemptRepository.existsByProblemIdAndSolvedDate(problem.getId(), solvedAt.toLocalDate())) {
            result.setDuplicate(result.getDuplicate() + 1);
            return;
        }

        Integer timeTaken = parseTimeToMinutes(timeStr);
        String status = determineStatus(timeStr, solutionStr);

        // Build notes from solution + tags
        String notes = buildNotes(solutionStr, tagsStr);

        Attempt attempt = new Attempt();
        attempt.setProblem(problem);
        attempt.setSolvedAt(solvedAt);
        attempt.setTimeTakenMinutes(timeTaken);
        attempt.setStatus(status);
        attempt.setNotes(notes);
        attemptRepository.save(attempt);
        result.setImported(result.getImported() + 1);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    public byte[] exportToCsv() {
        List<Attempt> attempts = attemptRepository.findAllByOrderBySolvedAtDesc();
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT
                .builder()
                .setHeader("Problem", "Date", "Time (minutes)", "Difficulty", "Status", "Notes")
                .build())) {
            for (Attempt a : attempts) {
                printer.printRecord(
                        a.getProblem().getTitle(),
                        a.getSolvedAt().toLocalDate().format(DATE_FMT_ISO),
                        a.getTimeTakenMinutes() != null ? a.getTimeTakenMinutes() : "",
                        a.getProblem().getDifficulty(),
                        a.getStatus(),
                        a.getNotes() != null ? a.getNotes() : ""
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Export failed", e);
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Parsing helpers ───────────────────────────────────────────────────────

    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Multi-line: "3/4/2026\n4/27/2026" — take the first date
        String firstLine = raw.split("\n")[0].trim();
        try {
            return LocalDate.parse(firstLine, DATE_FMT_MDY).atStartOfDay();
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(firstLine, DATE_FMT_ISO).atStartOfDay();
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse date: {}", firstLine);
                return null;
            }
        }
    }

    /**
     * Parses the Google Sheets stopwatch time format (MM:SS:xx or MM:SS) to minutes.
     * Examples: "2:00:00" → 2, "25:28:00" → 25, "01:57" → 2, "brute force: 01:57\ntree: 28:45:00" → 29
     */
    private Integer parseTimeToMinutes(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // DNF variants
        if (raw.toUpperCase().contains("DNF")) return null;

        // Multi-line: take last line that looks like a time
        String[] lines = raw.split("\n");
        String timePart = null;
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.matches(".*\\d+:\\d+.*")) {
                timePart = line;
                break;
            }
        }
        if (timePart == null) return null;

        // Strip prefix like "tree: " or "brute force: "
        if (timePart.contains(": ")) {
            timePart = timePart.substring(timePart.lastIndexOf(": ") + 2).trim();
        }

        // Parse MM:SS or MM:SS:xx
        String[] parts = timePart.split(":");
        try {
            int minutes = Integer.parseInt(parts[0].trim());
            int seconds = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return Math.max(1, minutes + (int) Math.round(seconds / 60.0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String determineStatus(String timeStr, String solutionStr) {
        if (timeStr == null || timeStr.toUpperCase().contains("DNF")) {
            String sol = solutionStr != null ? solutionStr.toLowerCase() : "";
            if (sol.contains("review") || sol.contains("try again") || sol.contains("look into")) {
                return "REVIEW";
            }
            return "FAILED";
        }
        return "SOLVED";
    }

    private String buildNotes(String solution, String tags) {
        StringBuilder sb = new StringBuilder();
        if (solution != null && !solution.isBlank()) sb.append(solution);
        if (tags != null && !tags.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n\nTags: ");
            else sb.append("Tags: ");
            sb.append(tags);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String safeGet(CSVRecord record, String header) {
        try {
            return record.get(header);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toExportUrl(String sheetUrl) {
        Pattern p = Pattern.compile("spreadsheets/d/([a-zA-Z0-9_-]+)");
        Matcher m = p.matcher(sheetUrl);
        if (m.find()) {
            return "https://docs.google.com/spreadsheets/d/" + m.group(1) + "/export?format=csv";
        }
        throw new IllegalArgumentException("Not a valid Google Sheets URL: " + sheetUrl);
    }
}
