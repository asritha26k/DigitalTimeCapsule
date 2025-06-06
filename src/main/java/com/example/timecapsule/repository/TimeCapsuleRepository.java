package com.example.timecapsule.repository;

import com.example.timecapsule.model.TimeCapsule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeCapsuleRepository extends MongoRepository<TimeCapsule, String> {

    List<TimeCapsule> findByStatus(TimeCapsule.CapsuleStatus status);
    List<TimeCapsule> findByUnlockDateBeforeAndStatus(Instant now, TimeCapsule.CapsuleStatus status);
    List<TimeCapsule> findByOwnerUsername(String username);
    List<TimeCapsule> findByStatusAndUnlockDateBefore(TimeCapsule.CapsuleStatus status, Instant now);
    Optional<TimeCapsule> findByPublicAccessToken(String publicAccessToken); // New method to find by token

}
