package com.copyright.repository;

import com.copyright.model.entity.Authorization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {

    List<Authorization> findByCopyrightId(Long copyrightId);

    List<Authorization> findByLicensorId(Long licensorId);

    List<Authorization> findByLicenseeId(Long licenseeId);

    List<Authorization> findByStatus(String status);

    Page<Authorization> findByLicensorId(Long licensorId, Pageable pageable);

    Page<Authorization> findByLicenseeId(Long licenseeId, Pageable pageable);

    @Query("SELECT a FROM Authorization a WHERE a.status = 'ACTIVE' AND a.endDate IS NOT NULL AND a.endDate < CURRENT_DATE")
    List<Authorization> findExpiredActive();
}
