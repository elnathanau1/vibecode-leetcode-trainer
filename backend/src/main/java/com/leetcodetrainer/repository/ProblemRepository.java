package com.leetcodetrainer.repository;

import com.leetcodetrainer.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Problem> findByDifficulty(String difficulty);

    List<Problem> findByDifficultyOrderByTitle(String difficulty);

    @Query("SELECT p FROM Problem p WHERE p.id NOT IN :excludeIds")
    List<Problem> findByIdNotIn(@Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT p FROM Problem p WHERE p.id NOT IN :excludeIds AND p.difficulty = :difficulty")
    List<Problem> findByIdNotInAndDifficulty(@Param("excludeIds") List<Long> excludeIds,
                                              @Param("difficulty") String difficulty);

    @Query(value = "SELECT * FROM problems WHERE patterns ILIKE %:pattern% ORDER BY difficulty, title", nativeQuery = true)
    List<Problem> findByPatternContaining(@Param("pattern") String pattern);

    @Query(value = "SELECT * FROM problems WHERE companies ILIKE %:company% ORDER BY difficulty, title", nativeQuery = true)
    List<Problem> findByCompanyContaining(@Param("company") String company);

    Optional<Problem> findByTitleIgnoreCase(String title);

    List<Problem> findByTitleContainingIgnoreCase(String title);
}
