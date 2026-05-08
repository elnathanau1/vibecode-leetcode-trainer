package com.leetcodetrainer.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecommendationRequest {
    private LocalDate date;
    private Integer targetMinutes = 90;
    private Boolean forceRegenerate = false;
    private String difficulty;
    private List<String> patterns;
    private String company;
    /** Which categories to include: NEW, REVIEW, SPACED_REPETITION. Empty/null means all. */
    private List<String> categories;
}
