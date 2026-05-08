package com.leetcodetrainer.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecommendationResponseDTO {
    private Long id;
    private LocalDate date;
    private Integer targetMinutes;
    private Integer totalEstimatedMinutes;
    private List<RecommendedProblemDTO> problems;

    @Data
    public static class RecommendedProblemDTO {
        private Long id;
        private String title;
        private String slug;
        private String difficulty;
        private List<String> patterns;
        private List<String> companies;
        private String url;
        private Integer estimatedMinutes;
        private String category;
        private String latestStatus;
    }
}
