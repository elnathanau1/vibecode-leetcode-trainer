package com.leetcodetrainer.dto;

import com.leetcodetrainer.model.Attempt;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptDTO {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String problemSlug;
    private String problemDifficulty;
    private LocalDateTime solvedAt;
    private Integer timeTakenMinutes;
    private String status;
    private String notes;

    public static AttemptDTO from(Attempt a) {
        AttemptDTO dto = new AttemptDTO();
        dto.setId(a.getId());
        dto.setProblemId(a.getProblem().getId());
        dto.setProblemTitle(a.getProblem().getTitle());
        dto.setProblemSlug(a.getProblem().getSlug());
        dto.setProblemDifficulty(a.getProblem().getDifficulty());
        dto.setSolvedAt(a.getSolvedAt());
        dto.setTimeTakenMinutes(a.getTimeTakenMinutes());
        dto.setStatus(a.getStatus());
        dto.setNotes(a.getNotes());
        return dto;
    }
}
