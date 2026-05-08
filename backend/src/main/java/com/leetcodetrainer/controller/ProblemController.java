package com.leetcodetrainer.controller;

import com.leetcodetrainer.dto.CreateProblemRequest;
import com.leetcodetrainer.dto.ProblemDTO;
import com.leetcodetrainer.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "Problems", description = "Manage the problem set")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "List all problems", description = "Returns all problems. Optional filters: difficulty (EASY/MEDIUM/HARD), pattern, company")
    public List<ProblemDTO> list(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String company) {
        return problemService.getAll(difficulty, pattern, company);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get problem by ID")
    public ProblemDTO getById(@PathVariable Long id) {
        return problemService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new problem to the problem set")
    public ProblemDTO create(@Valid @RequestBody CreateProblemRequest req) {
        return problemService.create(req);
    }

    @GetMapping("/patterns")
    @Operation(summary = "Get all distinct patterns", description = "Useful for filtering and building UIs")
    public List<String> getPatterns() {
        return problemService.getAllPatterns();
    }

    @GetMapping("/companies")
    @Operation(summary = "Get all distinct companies", description = "Useful for filtering and building UIs")
    public List<String> getCompanies() {
        return problemService.getAllCompanies();
    }
}
