package com.leetcodetrainer.controller;

import com.leetcodetrainer.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Progress statistics and heatmap data")
public class StatsController {

    private final AttemptService attemptService;

    @GetMapping("/heatmap")
    @Operation(
        summary = "Get heatmap data",
        description = "Returns daily solve counts for the past N days (default 365). " +
                      "Each entry: {date: 'YYYY-MM-DD', count: N}"
    )
    public List<Map<String, Object>> getHeatmap(@RequestParam(defaultValue = "365") int days) {
        return attemptService.getHeatmapData(days);
    }

    @GetMapping("/progress")
    @Operation(
        summary = "Get category progress",
        description = "Returns solve progress broken down by pattern/category. " +
                      "Includes total problems, solved count, and completion percentage per pattern."
    )
    public Map<String, Object> getCategoryProgress() {
        return attemptService.getCategoryProgress();
    }
}
