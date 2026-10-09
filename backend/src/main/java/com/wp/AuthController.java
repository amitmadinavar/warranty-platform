package com.wp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepo users;
    private final CustomerRepo customers;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(UserRepo users, CustomerRepo customers, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.customers = customers;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request == null ? null : request.get("email");
        String password = request == null ? null : request.get("password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !encoder.matches(password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        }

        String token = jwtService.generate(user.email, user.role, user.id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("id", user.id);
        response.put("name", user.name);
        response.put("email", user.email);
        response.put("role", user.role);
        response.put("customerId", user.customerId);
        return ResponseEntity.ok(response);
    }


}
