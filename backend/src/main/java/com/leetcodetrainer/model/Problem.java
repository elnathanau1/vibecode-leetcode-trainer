package com.leetcodetrainer.model;

import com.leetcodetrainer.model.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String difficulty;

    @Column(name = "patterns", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> patterns = new ArrayList<>();

    @Column(name = "companies", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> companies = new ArrayList<>();

    @Column
    private String url;

    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public int getDefaultEstimatedMinutes() {
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 10;
            case "HARD" -> 30;
            default -> 20;
        };
    }
}
