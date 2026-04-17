package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.ConstructionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConstructionTaskRepository extends JpaRepository<ConstructionTask, UUID> {

    // Finds a specific active build (used by ConstructionService to prevent duplicates)
    Optional<ConstructionTask> findByProfileIdAndBuildingType(UUID profileId, String buildingType);

    // Finds ALL active builds (Missing method! Used by PlayerController for the HUD)
    List<ConstructionTask> findByProfileId(UUID profileId);
}