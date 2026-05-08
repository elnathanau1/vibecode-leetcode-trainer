package com.leetcodetrainer.repository;

import com.leetcodetrainer.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByProblemIdOrderBySolvedAtDesc(Long problemId);

    @Query("SELECT DISTINCT a.problem.id FROM Attempt a")
    List<Long> findDistinctProblemIds();

    @Query("""
        SELECT a FROM Attempt a
        WHERE a.problem.id = :problemId
        ORDER BY a.solvedAt DESC
        LIMIT 1
        """)
    Optional<Attempt> findLatestByProblemId(@Param("problemId") Long problemId);

    @Query(value = """
        SELECT a.* FROM attempts a
        WHERE a.status = :status
        AND a.solved_at = (
            SELECT MAX(a2.solved_at) FROM attempts a2 WHERE a2.problem_id = a.problem_id
        )
        """, nativeQuery = true)
    List<Attempt> findLatestAttemptsByStatus(@Param("status") String status);

    @Query("""
        SELECT a FROM Attempt a
        WHERE a.solvedAt >= :from AND a.solvedAt < :to
        ORDER BY a.solvedAt DESC
        """)
    List<Attempt> findBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT DATE(a.solved_at) as date,
               COUNT(*) as count,
               SUM(CASE WHEN p.difficulty = 'EASY' THEN 1 ELSE 0 END) as easy,
               SUM(CASE WHEN p.difficulty = 'MEDIUM' THEN 1 ELSE 0 END) as medium,
               SUM(CASE WHEN p.difficulty = 'HARD' THEN 1 ELSE 0 END) as hard
        FROM attempts a
        JOIN problems p ON a.problem_id = p.id
        WHERE a.solved_at >= :from
        GROUP BY DATE(a.solved_at)
        ORDER BY date
        """, nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("from") LocalDateTime from);

    @Query("""
        SELECT AVG(a.timeTakenMinutes) FROM Attempt a
        WHERE a.problem.id = :problemId AND a.timeTakenMinutes IS NOT NULL AND a.status = 'SOLVED'
        """)
    Double findAvgSolveTimeByProblemId(@Param("problemId") Long problemId);

    List<Attempt> findAllByOrderBySolvedAtDesc();

    @Query(value = "SELECT COUNT(*) > 0 FROM attempts WHERE problem_id = :problemId AND DATE(solved_at) = :date", nativeQuery = true)
    boolean existsByProblemIdAndSolvedDate(@Param("problemId") Long problemId, @Param("date") java.time.LocalDate date);
}
