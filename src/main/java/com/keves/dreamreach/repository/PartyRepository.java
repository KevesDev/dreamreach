package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {
    List<Party> findByOwnerId(UUID ownerId);
}