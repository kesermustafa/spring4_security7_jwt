package com.example.jwt.services;


import com.example.jwt.domain.dtos.request.LoginRequest;
import com.example.jwt.domain.dtos.request.RegisterRequest;
import com.example.jwt.domain.dtos.response.ApiResponse;
import com.example.jwt.domain.dtos.response.AuthResponse;
import com.example.jwt.domain.enums.Role;
import com.example.jwt.domain.model.User;
import com.example.jwt.exception.BadRequestException;
import com.example.jwt.exception.EmailAlreadyExistsException;
import com.example.jwt.exception.messages.ErrorMessages;
import com.example.jwt.exception.messages.SuccessMessages;
import com.example.jwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public ApiResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    String.format(ErrorMessages.EMAIL_ALREADY_EXIST_MESSAGE, request.email())
            );
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();

        userRepository.save(user);

        return ApiResponse.success(
                String.format(SuccessMessages.USER_CREATED_SUCCESSFUL, request.email())
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException(ErrorMessages.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new BadRequestException(ErrorMessages.USER_DISABLED);
        }

        String accessToken = refreshTokenService.generateAccessToken(user);
        String refreshTokenRaw = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshTokenRaw);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(String.format(ErrorMessages.USER_NOT_FOUND, email)));
    }

}
