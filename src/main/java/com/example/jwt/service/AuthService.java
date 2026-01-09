package com.example.jwt.service;

import com.example.jwt.domain.dtos.response.ApiResponse;
import com.example.jwt.domain.enums.Role;
import com.example.jwt.domain.User;
import com.example.jwt.domain.dtos.request.LoginRequest;
import com.example.jwt.domain.dtos.request.RegisterRequest;
import com.example.jwt.exception.BadRequestException;
import com.example.jwt.exception.EmailAlreadyExistsException;
import com.example.jwt.exception.messages.ErrorMessages;
import com.example.jwt.exception.messages.SuccessMessages;
import com.example.jwt.repository.UserRepository;
import com.example.jwt.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public ApiResponse register(RegisterRequest request) {

        validateEmailUniqueness(request.email());

        User user = createUser(request);
        userRepository.save(user);

        return ApiResponse.success(String.format(
                SuccessMessages.USER_CREATED_SUCCESSFUL,
                request.email()
        ));
    }

    public String login(LoginRequest request) {

        User user = findUserByEmail(request.email());

        validatePassword(request.password(), user.getPassword());
        validateUserEnabled(user);

        return jwtService.generateToken(user.getEmail());
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    String.format(ErrorMessages.EMAIL_ALREADY_EXIST_MESSAGE, email)
            );
        }
    }

    private User createUser(RegisterRequest request) {
        return User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();
    }


    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.USER_NOT_FOUND, email)));
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadRequestException(String.format(ErrorMessages.INVALID_CREDENTIALS));
        }
    }

    private void validateUserEnabled(User user) {
        if (!user.isEnabled()) {
            throw new BadRequestException(String.format(ErrorMessages.USER_DISABLED));
        }
    }

}
