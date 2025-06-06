package com.example.timecapsule.service;


import com.example.timecapsule.model.User;
import com.example.timecapsule.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.userRepository = repo;
        this.passwordEncoder = encoder;
    }


    public User registerUser(String username, String email, String password) throws Exception {
        if (userRepository.existsByUsername(username)) {
            throw new Exception("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email is already in use");
        }

        String hashedPassword = passwordEncoder.encode(password);

        // 🔍 Debug logs
        System.out.println("Registering user:");
        System.out.println("Username: " + username);
        System.out.println("Raw password: " + password);
        System.out.println("Encoded password: " + hashedPassword);
        System.out.println("Matches check: " + passwordEncoder.matches(password, hashedPassword));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);
        user.setRoles(new HashSet<>() {{
            add("USER");
        }});

        return userRepository.save(user);
    }


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
