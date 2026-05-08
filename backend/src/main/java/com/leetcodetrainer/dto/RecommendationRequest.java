package com.leetcodetrainer.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RecommendationRequest {
    private LocalDate date;
    private Integer targetMinutes = 90;
    private Boolean forceRegenerate = false;
    private String difficulty;
    private String pattern;
    private String company;
}
