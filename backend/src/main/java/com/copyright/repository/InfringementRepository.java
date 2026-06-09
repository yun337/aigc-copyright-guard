package com.copyright.repository;

import com.copyright.model.entity.Infringement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfringementRepository extends JpaRepository<Infringement, Long> {

    List<Infringement> findByWorkId(Long workId);

    List<Infringement> findByStatus(String status);

    Page<Infringement> findByStatus(String status, Pageable pageable);

    @Query("SELECT i FROM Infringement i WHERE i.similarityScore >= :threshold ORDER BY i.similarityScore DESC")
    Page<Infringement> findHighSimilarity(@Param("threshold") Double threshold, Pageable pageable);

    @Query("SELECT i FROM Infringement i WHERE i.work.id = :workId ORDER BY i.similarityScore DESC")
    List<Infringement> findByWorkIdOrderByScore(@Param("workId") Long workId);
}
