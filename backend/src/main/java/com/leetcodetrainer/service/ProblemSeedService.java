package com.leetcodetrainer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetcodetrainer.model.Problem;
import com.leetcodetrainer.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemSeedService implements CommandLineRunner {

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.url}")
    private String seedUrl;

    @Override
    public void run(String... args) {
        if (!seedEnabled) return;

        long count = problemRepository.count();
        if (count > 0) {
            log.info("Problems already seeded ({} problems). Skipping.", count);
            return;
        }

        log.info("Seeding problems from {}", seedUrl);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(seedUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // New format: { "updated": "...", "data": [...] }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode questions = root.has("data") ? root.get("data") : root;

            int saved = 0;
            for (JsonNode q : questions) {
                String title = q.path("title").asText(null);
                String slug  = q.path("slug").asText(null);

                if (title == null || slug == null || slug.isBlank()) continue;
                if (problemRepository.existsBySlug(slug)) continue;

                String difficulty = normalizeToUpperCase(q.path("difficulty").asText("Medium"));
                boolean isPremium = q.path("premium").asBoolean(false);
                String url = "https://leetcode.com/problems/" + slug + "/";

                List<String> patterns = toStringList(q.get("pattern"));

                // companies is now [{name, slug, frequency}] — extract names
                List<String> companies = new ArrayList<>();
                JsonNode companiesNode = q.get("companies");
                if (companiesNode != null && companiesNode.isArray()) {
                    for (JsonNode c : companiesNode) {
                        String name = c.path("name").asText(null);
                        if (name != null && !name.isBlank()) companies.add(name);
                    }
                }

                Problem problem = new Problem();
                problem.setTitle(title);
                problem.setSlug(slug);
                problem.setDifficulty(difficulty);
                problem.setPatterns(patterns);
                problem.setCompanies(companies);
                problem.setUrl(url);
                problem.setIsPremium(isPremium);

                problemRepository.save(problem);
                saved++;
            }
            log.info("Seeded {} problems successfully.", saved);
        } catch (Exception e) {
            log.error("Failed to seed problems: {}", e.getMessage(), e);
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) result.add(item.asText());
            }
        }
        return result;
    }

    private String normalizeToUpperCase(String difficulty) {
        if (difficulty == null) return "MEDIUM";
        return switch (difficulty.toLowerCase()) {
            case "easy" -> "EASY";
            case "hard" -> "HARD";
            default -> "MEDIUM";
        };
    }
}
