package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByProfileIdOrderByTimestampDesc(UUID profileId);
    void deleteByProfileId(UUID profileId);
}