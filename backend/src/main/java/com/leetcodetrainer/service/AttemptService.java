package com.leetcodetrainer.service;

import com.leetcodetrainer.dto.AttemptDTO;
import com.leetcodetrainer.dto.CreateAttemptRequest;
import com.leetcodetrainer.model.Attempt;
import com.leetcodetrainer.model.Problem;
import com.leetcodetrainer.repository.AttemptRepository;
import com.leetcodetrainer.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final ProblemRepository problemRepository;

    public AttemptDTO create(CreateAttemptRequest req) {
        Problem problem = problemRepository.findById(req.getProblemId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Problem not found: " + req.getProblemId()));

        Attempt attempt = new Attempt();
        attempt.setProblem(problem);
        attempt.setStatus(req.getStatus().toUpperCase());
        attempt.setTimeTakenMinutes(req.getTimeTakenMinutes());
        attempt.setNotes(req.getNotes());
        attempt.setSolvedAt(req.getSolvedAt() != null ? req.getSolvedAt() : LocalDateTime.now());

        return AttemptDTO.from(attemptRepository.save(attempt));
    }

    public List<AttemptDTO> getAll() {
        return attemptRepository.findAllByOrderBySolvedAtDesc().stream()
                .map(AttemptDTO::from)
                .collect(Collectors.toList());
    }

    public List<AttemptDTO> getByProblemId(Long problemId) {
        return attemptRepository.findByProblemIdOrderBySolvedAtDesc(problemId).stream()
                .map(AttemptDTO::from)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getHeatmapData(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = attemptRepository.findDailyCountsSince(from);
        return rows.stream().map(row -> {
            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("date", row[0].toString());
            entry.put("count", ((Number) row[1]).intValue());
            return entry;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getCategoryProgress() {
        List<Attempt> allAttempts = attemptRepository.findAll();
        List<Problem> allProblems = problemRepository.findAll();

        Map<String, Long> totalByPattern = allProblems.stream()
                .flatMap(p -> p.getPatterns().stream())
                .collect(Collectors.groupingBy(pat -> pat, Collectors.counting()));

        Map<Long, String> latestStatusByProblemId = allAttempts.stream()
                .collect(Collectors.toMap(
                        a -> a.getProblem().getId(),
                        Attempt::getStatus,
                        (a, b) -> a
                ));

        Map<String, Long> solvedByPattern = allProblems.stream()
                .filter(p -> "SOLVED".equals(latestStatusByProblemId.get(p.getId())))
                .flatMap(p -> p.getPatterns().stream())
                .collect(Collectors.groupingBy(pat -> pat, Collectors.counting()));

        List<Map<String, Object>> categories = totalByPattern.entrySet().stream()
                .map(entry -> {
                    long total = entry.getValue();
                    long solved = solvedByPattern.getOrDefault(entry.getKey(), 0L);
                    return Map.<String, Object>of(
                            "pattern", entry.getKey(),
                            "total", total,
                            "solved", solved,
                            "percentage", total > 0 ? Math.round((solved * 100.0) / total) : 0
                    );
                })
                .sorted((a, b) -> ((String) a.get("pattern")).compareTo((String) b.get("pattern")))
                .collect(Collectors.toList());

        long totalProblems = allProblems.size();
        long solvedProblems = latestStatusByProblemId.values().stream()
                .filter("SOLVED"::equals).count();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("categories", categories);
        result.put("totalProblems", totalProblems);
        result.put("solvedProblems", solvedProblems);
        return result;
    }
}
