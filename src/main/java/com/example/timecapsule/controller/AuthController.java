package com.example.timecapsule.controller;

import com.example.timecapsule.model.User;
import com.example.timecapsule.security.JwtUtils;
import com.example.timecapsule.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.timecapsule.payload.RegisterRequest;
import com.example.timecapsule.payload.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class); // ADD THIS

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtils jwtUtils,
                          AuthenticationManager authenticationManager,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage(), e); // Enhanced logging
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Inside your AuthController.java

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            User user = userService.findByUsername(request.username())
                    .orElseThrow(() -> {
                        logger.error("User not found after successful authentication for username: {}", request.username());
                        return new RuntimeException("User not found");
                    });

            String jwt = jwtUtils.generateJwtToken(user.getUsername());

            Cookie cookie = new Cookie("token", jwt);
            cookie.setHttpOnly(true); // Prevents client-side JavaScript access to the cookie
            cookie.setSecure(false);  // <<< CRITICAL: Set to false for HTTP (localhost) connections
            cookie.setPath("/");      // Cookie is available for all paths under the domain
            cookie.setMaxAge(24 * 60 * 60); // Cookie valid for 1 day (in seconds)

            // >>>>>>>>>>>>>>> DEFINITIVE DOMAIN SETTING FOR LOCALHOST <<<<<<<<<<<<<<<<
            // This is the most common and reliable setting when your frontend is at http://localhost:3000
            // and your backend is at http://localhost:8080.
            cookie.setDomain("localhost");
            // If you were consistently accessing your frontend/backend via http://127.0.0.1, you would use "127.0.0.1" here.
            // But stick with "localhost" if that's what you type in the browser.
            // >>>>>>>>>>>>>>> END DEFINITIVE DOMAIN SETTING <<<<<<<<<<<<<<<<

            response.addCookie(cookie);

            logger.info("User {} logged in successfully. JWT issued and cookie set.", user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", request.username(), e.getMessage(), e);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
}