package com.leetcodetrainer.repository;

import com.leetcodetrainer.model.DailyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, Long> {

    Optional<DailyRecommendation> findByRecommendationDate(LocalDate date);

    boolean existsByRecommendationDate(LocalDate date);
}
