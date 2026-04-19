package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.ActiveMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActiveMissionRepository extends JpaRepository<ActiveMission, UUID> {
    List<ActiveMission> findByPartyOwnerId(UUID ownerId);
}