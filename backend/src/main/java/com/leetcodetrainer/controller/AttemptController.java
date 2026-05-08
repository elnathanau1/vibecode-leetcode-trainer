package com.leetcodetrainer.controller;

import com.leetcodetrainer.dto.AttemptDTO;
import com.leetcodetrainer.dto.CreateAttemptRequest;
import com.leetcodetrainer.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
@Tag(name = "Attempts", description = "Track problem-solving attempts")
public class AttemptController {

    private final AttemptService attemptService;

    @GetMapping
    @Operation(summary = "List all attempts", description = "Returns all attempts ordered by most recent first")
    public List<AttemptDTO> list() {
        return attemptService.getAll();
    }

    @GetMapping("/problem/{problemId}")
    @Operation(summary = "Get all attempts for a specific problem")
    public List<AttemptDTO> getByProblem(@PathVariable Long problemId) {
        return attemptService.getByProblemId(problemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Record a new attempt",
        description = "Log an attempt for a problem. status must be SOLVED, REVIEW, or FAILED. " +
                      "timeTakenMinutes is optional. solvedAt defaults to now if not provided."
    )
    public AttemptDTO create(@Valid @RequestBody CreateAttemptRequest req) {
        return attemptService.create(req);
    }
}
