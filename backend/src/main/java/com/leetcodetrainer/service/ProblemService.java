package com.leetcodetrainer.service;

import com.leetcodetrainer.dto.CreateProblemRequest;
import com.leetcodetrainer.dto.ProblemDTO;
import com.leetcodetrainer.model.Attempt;
import com.leetcodetrainer.model.Problem;
import com.leetcodetrainer.repository.AttemptRepository;
import com.leetcodetrainer.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final AttemptRepository attemptRepository;

    public List<ProblemDTO> getAll(String difficulty, String pattern, String company) {
        List<Problem> problems;
        if (pattern != null && !pattern.isBlank()) {
            problems = problemRepository.findByPatternContaining(pattern);
        } else if (company != null && !company.isBlank()) {
            problems = problemRepository.findByCompanyContaining(company);
        } else if (difficulty != null && !difficulty.isBlank()) {
            problems = problemRepository.findByDifficultyOrderByTitle(difficulty.toUpperCase());
        } else {
            problems = problemRepository.findAll(Sort.by("difficulty", "title"));
        }
        return problems.stream().map(p -> enrichWithStatus(ProblemDTO.from(p), p.getId())).collect(Collectors.toList());
    }

    public ProblemDTO getById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found: " + id));
        return enrichWithStatus(ProblemDTO.from(problem), id);
    }

    public ProblemDTO create(CreateProblemRequest req) {
        if (problemRepository.existsBySlug(req.getSlug())) {
            throw new ResponseStatusException(CONFLICT, "Problem with slug already exists: " + req.getSlug());
        }
        Problem problem = new Problem();
        problem.setTitle(req.getTitle());
        problem.setSlug(req.getSlug());
        problem.setDifficulty(req.getDifficulty().toUpperCase());
        problem.setPatterns(req.getPatterns());
        problem.setCompanies(req.getCompanies());
        problem.setUrl(req.getUrl());
        problem.setIsPremium(req.getIsPremium());
        return ProblemDTO.from(problemRepository.save(problem));
    }

    public List<String> getAllPatterns() {
        return problemRepository.findAll().stream()
                .flatMap(p -> p.getPatterns().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getAllCompanies() {
        return problemRepository.findAll().stream()
                .flatMap(p -> p.getCompanies().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private ProblemDTO enrichWithStatus(ProblemDTO dto, Long problemId) {
        Optional<Attempt> latestAttempt = attemptRepository.findLatestByProblemId(problemId);
        latestAttempt.ifPresent(a -> dto.setLatestStatus(a.getStatus()));

        Double avgTime = attemptRepository.findAvgSolveTimeByProblemId(problemId);
        if (avgTime != null) {
            dto.setEstimatedMinutes((int) Math.round(avgTime));
        }
        return dto;
    }
}
