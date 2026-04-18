package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.BuildingInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Standard JpaRepository for managing physical building records.
 * Required to perform specific updates on individual structure instances.
 */
@Repository
public interface BuildingInstanceRepository extends JpaRepository<BuildingInstance, UUID> {
}