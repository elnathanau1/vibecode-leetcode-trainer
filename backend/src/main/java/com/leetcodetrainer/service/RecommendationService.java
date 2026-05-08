package com.leetcodetrainer.service;

import com.leetcodetrainer.dto.RecommendationResponseDTO;
import com.leetcodetrainer.dto.RecommendationResponseDTO.RecommendedProblemDTO;
import com.leetcodetrainer.model.Attempt;
import com.leetcodetrainer.model.DailyRecommendation;
import com.leetcodetrainer.model.Problem;
import com.leetcodetrainer.repository.AttemptRepository;
import com.leetcodetrainer.repository.DailyRecommendationRepository;
import com.leetcodetrainer.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final DailyRecommendationRepository recommendationRepository;
    private final ProblemRepository problemRepository;
    private final AttemptRepository attemptRepository;

    private static final Map<String, Integer> DIFFICULTY_ORDER = Map.of(
            "EASY", 1, "MEDIUM", 2, "HARD", 3
    );

    public RecommendationResponseDTO getOrGenerate(LocalDate date, int targetMinutes, boolean forceRegenerate,
                                                    String difficulty, List<String> patterns, String company,
                                                    List<String> categories) {
        if (!forceRegenerate) {
            Optional<DailyRecommendation> existing = recommendationRepository.findByRecommendationDate(date);
            if (existing.isPresent()) {
                return buildResponse(existing.get());
            }
        } else {
            recommendationRepository.findByRecommendationDate(date)
                    .ifPresent(r -> recommendationRepository.deleteById(r.getId()));
        }

        return generate(date, targetMinutes, difficulty, patterns, company, categories);
    }

    private RecommendationResponseDTO generate(LocalDate date, int targetMinutes,
                                                String filterDifficulty, List<String> filterPatterns, String filterCompany,
                                                List<String> filterCategories) {
        boolean includeReview = filterCategories == null || filterCategories.isEmpty()
                || filterCategories.stream().anyMatch(c -> c.equalsIgnoreCase("REVIEW"));
        boolean includeSpaced = filterCategories == null || filterCategories.isEmpty()
                || filterCategories.stream().anyMatch(c -> c.equalsIgnoreCase("SPACED_REPETITION"));
        boolean includeNew = filterCategories == null || filterCategories.isEmpty()
                || filterCategories.stream().anyMatch(c -> c.equalsIgnoreCase("NEW"));

        List<Long> attemptedIds = attemptRepository.findDistinctProblemIds();

        List<Problem> reviewProblems = includeReview
                ? getReviewProblems(filterDifficulty, filterPatterns, filterCompany)
                : Collections.emptyList();
        List<Problem> spacedRepProblems = includeSpaced
                ? getSpacedRepetitionProblems(filterDifficulty, filterPatterns, filterCompany)
                : Collections.emptyList();
        List<Problem> newProblems = includeNew
                ? getNewProblems(attemptedIds, filterDifficulty, filterPatterns, filterCompany)
                : Collections.emptyList();

        List<ProblemWithCategory> selected = new ArrayList<>();
        int totalMinutes = 0;
        int reviewBudget = (int) (targetMinutes * 0.25);

        // Add up to 2 review problems within 25% budget
        for (Problem p : reviewProblems) {
            if (selected.size() >= 2 || totalMinutes + minutesFor(p) > reviewBudget + totalMinutes) break;
            selected.add(new ProblemWithCategory(p, "REVIEW"));
            totalMinutes += minutesFor(p);
        }

        // Add up to 2 spaced repetition problems within 25% budget
        Set<Long> selectedIds = selected.stream().map(pc -> pc.problem.getId()).collect(Collectors.toSet());
        int spacedAdded = 0;
        for (Problem p : spacedRepProblems) {
            if (spacedAdded >= 2) break;
            if (selectedIds.contains(p.getId())) continue;
            if (totalMinutes + minutesFor(p) > targetMinutes) continue;
            selected.add(new ProblemWithCategory(p, "SPACED_REPETITION"));
            totalMinutes += minutesFor(p);
            selectedIds.add(p.getId());
            spacedAdded++;
        }

        // Fill remaining with new problems
        for (Problem p : newProblems) {
            if (totalMinutes >= targetMinutes) break;
            if (selectedIds.contains(p.getId())) continue;
            int time = minutesFor(p);
            if (totalMinutes + time > targetMinutes + 15) continue; // allow small overflow
            selected.add(new ProblemWithCategory(p, "NEW"));
            totalMinutes += time;
            selectedIds.add(p.getId());
        }

        // Sort by difficulty: EASY → MEDIUM → HARD
        selected.sort(Comparator.comparingInt(pc ->
                DIFFICULTY_ORDER.getOrDefault(pc.problem.getDifficulty().toUpperCase(), 2)));

        DailyRecommendation rec = new DailyRecommendation();
        rec.setRecommendationDate(date);
        rec.setTargetMinutes(targetMinutes);
        rec.setProblemIds(selected.stream()
                .map(pc -> String.valueOf(pc.problem.getId()))
                .collect(Collectors.toList()));
        recommendationRepository.save(rec);

        return buildResponse(rec, selected);
    }

    private List<Problem> getReviewProblems(String difficulty, List<String> patterns, String company) {
        List<Attempt> reviewAttempts = attemptRepository.findLatestAttemptsByStatus("REVIEW");
        List<Problem> problems = reviewAttempts.stream()
                .map(Attempt::getProblem)
                .filter(p -> matchesFilters(p, difficulty, patterns, company))
                .collect(Collectors.toList());
        Collections.shuffle(problems);
        return problems;
    }

    private List<Problem> getSpacedRepetitionProblems(String difficulty, List<String> patterns, String company) {
        List<Attempt> solvedAttempts = attemptRepository.findLatestAttemptsByStatus("SOLVED");
        List<Problem> problems = solvedAttempts.stream()
                .map(Attempt::getProblem)
                .filter(p -> matchesFilters(p, difficulty, patterns, company))
                .collect(Collectors.toList());
        Collections.shuffle(problems);
        return problems;
    }

    private List<Problem> getNewProblems(List<Long> attemptedIds, String difficulty, List<String> patterns, String company) {
        List<Long> excludeIds = attemptedIds.isEmpty() ? List.of(-1L) : attemptedIds;
        List<Problem> problems;
        if (difficulty != null && !difficulty.isBlank()) {
            problems = problemRepository.findByIdNotInAndDifficulty(excludeIds, difficulty.toUpperCase());
        } else {
            problems = problemRepository.findByIdNotIn(excludeIds);
        }
        List<Problem> filtered = problems.stream()
                .filter(p -> matchesFilters(p, null, patterns, company))
                .collect(Collectors.toList());

        List<Problem> easy = filtered.stream().filter(p -> "EASY".equals(p.getDifficulty())).collect(Collectors.toList());
        List<Problem> medium = filtered.stream().filter(p -> "MEDIUM".equals(p.getDifficulty())).collect(Collectors.toList());
        List<Problem> hard = filtered.stream().filter(p -> "HARD".equals(p.getDifficulty())).collect(Collectors.toList());
        Collections.shuffle(easy);
        Collections.shuffle(medium);
        Collections.shuffle(hard);

        // Warmup: up to 2 easy, then interleave medium+hard (2:1 ratio), then remaining easy as fallback
        List<Problem> result = new ArrayList<>();
        result.addAll(easy.subList(0, Math.min(2, easy.size())));

        int mi = 0, hi = 0;
        while (mi < medium.size() || hi < hard.size()) {
            if (mi < medium.size()) result.add(medium.get(mi++));
            if (mi < medium.size()) result.add(medium.get(mi++));
            if (hi < hard.size()) result.add(hard.get(hi++));
        }

        if (easy.size() > 2) result.addAll(easy.subList(2, easy.size()));
        return result;
    }

    private boolean matchesFilters(Problem p, String difficulty, List<String> patterns, String company) {
        if (difficulty != null && !difficulty.isBlank() &&
                !difficulty.equalsIgnoreCase(p.getDifficulty())) return false;
        if (patterns != null && !patterns.isEmpty() &&
                p.getPatterns().stream().noneMatch(pt -> patterns.stream().anyMatch(pt::equalsIgnoreCase))) return false;
        if (company != null && !company.isBlank() &&
                p.getCompanies().stream().noneMatch(c -> c.equalsIgnoreCase(company))) return false;
        return true;
    }

    private int minutesFor(Problem p) {
        Double avg = attemptRepository.findAvgSolveTimeByProblemId(p.getId());
        if (avg != null) return (int) Math.round(avg);
        return p.getDefaultEstimatedMinutes();
    }

    private RecommendationResponseDTO buildResponse(DailyRecommendation rec) {
        List<Long> ids = rec.getProblemIds().stream().map(Long::parseLong).collect(Collectors.toList());
        List<Problem> problems = problemRepository.findAllById(ids);
        Map<Long, Problem> problemMap = problems.stream().collect(Collectors.toMap(Problem::getId, p -> p));

        List<ProblemWithCategory> ordered = ids.stream()
                .filter(problemMap::containsKey)
                .map(id -> new ProblemWithCategory(problemMap.get(id), ""))
                .collect(Collectors.toList());

        return buildResponse(rec, ordered);
    }

    private RecommendationResponseDTO buildResponse(DailyRecommendation rec, List<ProblemWithCategory> problems) {
        RecommendationResponseDTO dto = new RecommendationResponseDTO();
        dto.setId(rec.getId());
        dto.setDate(rec.getRecommendationDate());
        dto.setTargetMinutes(rec.getTargetMinutes());

        List<RecommendedProblemDTO> problemDTOs = new ArrayList<>();
        int totalMinutes = 0;

        for (ProblemWithCategory pc : problems) {
            Problem p = pc.problem;
            RecommendedProblemDTO pd = new RecommendedProblemDTO();
            pd.setId(p.getId());
            pd.setTitle(p.getTitle());
            pd.setSlug(p.getSlug());
            pd.setDifficulty(p.getDifficulty());
            pd.setPatterns(p.getPatterns());
            pd.setCompanies(p.getCompanies());
            pd.setUrl(p.getUrl());
            pd.setCategory(pc.category);
            int mins = minutesFor(p);
            pd.setEstimatedMinutes(mins);
            totalMinutes += mins;

            attemptRepository.findLatestByProblemId(p.getId())
                    .ifPresent(a -> pd.setLatestStatus(a.getStatus()));

            pd.setLoggedToday(attemptRepository.existsByProblemIdAndSolvedDate(p.getId(), java.time.LocalDate.now()));

            problemDTOs.add(pd);
        }

        dto.setProblems(problemDTOs);
        dto.setTotalEstimatedMinutes(totalMinutes);
        return dto;
    }

    private record ProblemWithCategory(Problem problem, String category) {}
}
