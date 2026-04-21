package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.UpgradeTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpgradeTaskRepository extends JpaRepository<UpgradeTask, UUID> {
    Optional<UpgradeTask> findByBuildingInstanceId(UUID buildingInstanceId);
}