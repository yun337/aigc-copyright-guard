package com.copyright.repository;

import com.copyright.model.entity.NFT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NFTRepository extends JpaRepository<NFT, Long> {

    Optional<NFT> findByWorkId(Long workId);

    Optional<NFT> findByTokenIdAndContractAddress(String tokenId, String contractAddress);

    List<NFT> findByOwnerId(Long ownerId);

    @Query("SELECT n FROM NFT n WHERE n.owner.id = :ownerId ORDER BY n.mintedAt DESC")
    List<NFT> findLatestByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(n) FROM NFT n WHERE n.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);
}
