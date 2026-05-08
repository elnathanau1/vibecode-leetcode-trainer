package com.leetcodetrainer.controller;

import com.leetcodetrainer.dto.ImportResultDTO;
import com.leetcodetrainer.service.SpreadsheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/spreadsheet")
@RequiredArgsConstructor
@Tag(name = "Spreadsheet", description = "Import and export attempts as CSV / Google Sheets")
public class SpreadsheetController {

    private final SpreadsheetService spreadsheetService;

    @PostMapping(value = "/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import attempts from a CSV file",
        description = "Upload a CSV with columns: Problem, Date (M/D/YYYY), Time (MM:SS:00 or DNF), Level, Description, Solution, Tags. " +
                      "Duplicate attempts (same problem + same day) are skipped automatically."
    )
    public ImportResultDTO importFile(@RequestParam("file") MultipartFile file) {
        return spreadsheetService.importFromFile(file);
    }

    @PostMapping("/import/url")
    @Operation(
        summary = "Import attempts from a Google Sheet URL",
        description = "Provide a public Google Sheet URL. The sheet must be accessible without login. " +
                      "The sheet must follow the same column format as the file import."
    )
    public ImportResultDTO importFromUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        return spreadsheetService.importFromGoogleSheet(url);
    }

    @GetMapping("/export")
    @Operation(
        summary = "Export all attempts to CSV",
        description = "Downloads a CSV file with columns: Problem, Date, Time (minutes), Difficulty, Status, Notes."
    )
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = spreadsheetService.exportToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leetcode-attempts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
