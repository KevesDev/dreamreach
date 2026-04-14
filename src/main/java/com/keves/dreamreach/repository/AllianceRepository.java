package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.Alliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The DAO (Data Access Object) for the alliance entity.
 * Spring automatically implements the standard CRUD operations at runtime.
 */
@Repository
public interface AllianceRepository extends JpaRepository<Alliance, UUID> {

    // Spring data JPA translates this method name directly into an SQL query:
    // SELECT * FROM alliance WHERE name = ?
    Optional<Alliance> findByName(String name);
}
