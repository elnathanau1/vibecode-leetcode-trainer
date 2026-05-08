package com.leetcodetrainer.controller;

import com.leetcodetrainer.dto.RecommendationRequest;
import com.leetcodetrainer.dto.RecommendationResponseDTO;
import com.leetcodetrainer.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Daily problem recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/today")
    @Operation(
        summary = "Get today's recommendations",
        description = "Returns today's recommended problems. Generates them if they don't exist yet. " +
                      "Optional filters: targetMinutes (default 90), difficulty, pattern, company"
    )
    public RecommendationResponseDTO getToday(
            @RequestParam(defaultValue = "90") int targetMinutes,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String company) {
        return recommendationService.getOrGenerate(
                LocalDate.now(), targetMinutes, false, difficulty, pattern, company);
    }

    @PostMapping("/generate")
    @Operation(
        summary = "Generate recommendations",
        description = "Generate recommendations for a specific date with custom parameters. " +
                      "Set forceRegenerate=true to replace existing recommendations for that date. " +
                      "Supports filtering by difficulty (EASY/MEDIUM/HARD), pattern, or company. " +
                      "targetMinutes controls total session length (default: 90)."
    )
    public RecommendationResponseDTO generate(@RequestBody RecommendationRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();
        int targetMinutes = req.getTargetMinutes() != null ? req.getTargetMinutes() : 90;
        return recommendationService.getOrGenerate(
                date, targetMinutes,
                Boolean.TRUE.equals(req.getForceRegenerate()),
                req.getDifficulty(), req.getPattern(), req.getCompany());
    }
}
