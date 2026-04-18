package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.TavernListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TavernListingRepository extends JpaRepository<TavernListing, UUID> {
    Optional<TavernListing> findByProfileId(UUID profileId);
}