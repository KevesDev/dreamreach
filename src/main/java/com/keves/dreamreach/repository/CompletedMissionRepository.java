package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.CompletedMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompletedMissionRepository extends JpaRepository<CompletedMission, UUID> {
    List<CompletedMission> findByProfileId(UUID profileId);
}