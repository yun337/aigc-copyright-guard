package com.copyright.repository;

import com.copyright.model.entity.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

    List<Work> findByUserId(Long userId);

    Page<Work> findByUserId(Long userId, Pageable pageable);

    Optional<Work> findByFileHash(String fileHash);

    Optional<Work> findByIpfsCid(String ipfsCid);

    List<Work> findByCopyrightStatus(String copyrightStatus);

    Page<Work> findByWorkType(String workType, Pageable pageable);

    @Query("SELECT w FROM Work w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Work> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(w) FROM Work w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Work w WHERE w.featureVector IS NOT NULL")
    List<Work> findWorksWithFeatureVectors();

    @Query("SELECT w FROM Work w WHERE w.copyrightStatus = 'REGISTERED'")
    Page<Work> findRegisteredWorks(Pageable pageable);
}
