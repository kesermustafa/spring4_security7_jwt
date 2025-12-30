package com.example.jwt.service;

import com.example.jwt.domain.enums.Role;
import com.example.jwt.domain.User;
import com.example.jwt.domain.dtos.request.LoginRequest;
import com.example.jwt.domain.dtos.request.RegisterRequest;
import com.example.jwt.repository.UserRepository;
import com.example.jwt.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("User disabled");
        }

        return jwtService.generateToken(user.getEmail());
    }
}
