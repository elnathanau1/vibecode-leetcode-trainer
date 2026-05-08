package com.leetcodetrainer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAttemptRequest {

    @NotNull(message = "problemId is required")
    private Long problemId;

    private LocalDateTime solvedAt;

    private Integer timeTakenMinutes;

    @NotBlank(message = "status is required")
    @Pattern(regexp = "SOLVED|REVIEW|FAILED", message = "status must be SOLVED, REVIEW, or FAILED")
    private String status;

    private String notes;
}
