package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.AcceptedMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcceptedMissionRepository extends JpaRepository<AcceptedMission, UUID> {
    List<AcceptedMission> findByProfileId(UUID profileId);
    Optional<AcceptedMission> findByProfileIdAndQuestTemplateId(UUID profileId, UUID questTemplateId);
}