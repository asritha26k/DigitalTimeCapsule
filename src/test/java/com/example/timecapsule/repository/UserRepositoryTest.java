package com.example.timecapsule.repository;

import com.example.timecapsule.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByUsername() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPasswordHash("hashedpassword123");
        user.setRoles(Set.of("USER"));

        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("john");

        assertTrue(found.isPresent());
        assertEquals("john@example.com", found.get().getEmail());
    }

    @Test
    void testExistsByUsername() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash123");
        user.setRoles(Set.of("USER"));

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("alice"));
        assertFalse(userRepository.existsByUsername("bob"));
    }

    @Test
    void testExistsByEmail() {
        User user = new User();
        user.setUsername("eve");
        user.setEmail("eve@example.com");
        user.setPasswordHash("hash456");
        user.setRoles(Set.of("ADMIN"));

        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("eve@example.com"));
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }
}
