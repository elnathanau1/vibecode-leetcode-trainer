package com.leetcodetrainer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateProblemRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "slug is required")
    private String slug;

    @NotBlank(message = "difficulty is required")
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "difficulty must be EASY, MEDIUM, or HARD")
    private String difficulty;

    private List<String> patterns = new ArrayList<>();

    private List<String> companies = new ArrayList<>();

    private String url;

    private Boolean isPremium = false;
}
