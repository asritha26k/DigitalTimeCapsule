package com.example.timecapsule.repository;

import com.example.timecapsule.model.TimeCapsule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
class TimeCapsuleRepositoryTest {

    @Autowired
    private TimeCapsuleRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll(); // Clean before test
    }

    @Test
    void testSaveAndFindByStatus() {
        TimeCapsule capsule = new TimeCapsule();
        capsule.setOwnerUsername("testUser");
        capsule.setRecipientEmail("recipient@example.com");
        capsule.setUnlockDate(Instant.now().plusSeconds(3600));
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        capsule.setFileMetadataList(List.of());

        repository.save(capsule);

        List<TimeCapsule> lockedCapsules = repository.findByStatus(TimeCapsule.CapsuleStatus.LOCKED);

        assertFalse(lockedCapsules.isEmpty(), "Should return at least one locked capsule");
        assertEquals("testUser", lockedCapsules.get(0).getOwnerUsername());
    }

    @Test
    void testFindByPublicAccessToken() {
        TimeCapsule capsule = new TimeCapsule();
        capsule.setPublicAccessToken("abc123token");
        repository.save(capsule);

        var result = repository.findByPublicAccessToken("abc123token");
        assertTrue(result.isPresent());
    }
}




