package com.leetcodetrainer.dto;

import com.leetcodetrainer.model.Problem;
import lombok.Data;

import java.util.List;

@Data
public class ProblemDTO {
    private Long id;
    private String title;
    private String slug;
    private String difficulty;
    private List<String> patterns;
    private List<String> companies;
    private String url;
    private Boolean isPremium;
    private Integer estimatedMinutes;
    private String latestStatus;

    public static ProblemDTO from(Problem p) {
        ProblemDTO dto = new ProblemDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setSlug(p.getSlug());
        dto.setDifficulty(p.getDifficulty());
        dto.setPatterns(p.getPatterns());
        dto.setCompanies(p.getCompanies());
        dto.setUrl(p.getUrl());
        dto.setIsPremium(p.getIsPremium());
        dto.setEstimatedMinutes(p.getDefaultEstimatedMinutes());
        return dto;
    }
}
