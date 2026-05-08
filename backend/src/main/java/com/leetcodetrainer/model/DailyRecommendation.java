package com.leetcodetrainer.model;

import com.leetcodetrainer.model.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "daily_recommendations")
public class DailyRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recommendation_date", unique = true, nullable = false)
    private LocalDate recommendationDate;

    @Column(name = "target_minutes")
    private Integer targetMinutes = 90;

    @Column(name = "problem_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> problemIds = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
