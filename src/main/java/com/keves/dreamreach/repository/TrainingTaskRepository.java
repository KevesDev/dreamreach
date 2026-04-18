package com.keves.dreamreach.repository;

import com.keves.dreamreach.entity.TrainingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingTaskRepository extends JpaRepository<TrainingTask, UUID> {
    // Retrieves tasks in chronological order so the queue is processed sequentially
    List<TrainingTask> findByProfileIdOrderByStartTimeAsc(UUID profileId);
}