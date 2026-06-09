package com.copyright.repository;

import com.copyright.model.entity.Copyright;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyrightRepository extends JpaRepository<Copyright, Long> {

    Optional<Copyright> findByWorkId(Long workId);

    Optional<Copyright> findByCertificateId(String certificateId);

    Optional<Copyright> findByTxHash(String txHash);

    List<Copyright> findByUserId(Long userId);

    Page<Copyright> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT c FROM Copyright c ORDER BY c.registeredAt DESC")
    Page<Copyright> findAllOrderByRegisteredAt(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Copyright c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
