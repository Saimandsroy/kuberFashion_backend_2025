package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.ReferralRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRelationRepository extends JpaRepository<ReferralRelation, Long> {

    Optional<ReferralRelation> findByUserId(Long userId);

    List<ReferralRelation> findByParentId(Long parentId);

    @Query("SELECT rr FROM ReferralRelation rr WHERE rr.parent.id = :parentId")
    List<ReferralRelation> getDirectReferrals(@Param("parentId") Long parentId);
}
