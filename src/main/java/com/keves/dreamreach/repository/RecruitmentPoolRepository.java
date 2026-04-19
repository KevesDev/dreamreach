package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.RecruitmentPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecruitmentPoolRepository extends JpaRepository<RecruitmentPool, UUID> {
    Optional<RecruitmentPool> findByCharacterTemplateId(UUID characterTemplateId);
}