package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.AuthLoginRequest;
import com.chamcham.backend.dto.auth.AuthRegisterRequest;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(AuthRegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a unique username!");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists!");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .image(request.image())
                .country(request.country())
                .phone(request.phone())
                .description(request.description())
                .seller(request.isSeller())
                .build();

        userRepository.save(user);
        log.info("Created user {}", user.getId());
    }

    public AuthSession login(AuthLoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Check username or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Check username or password!");
        }

        String token = jwtService.generateToken(user.getId(), user.isSeller());
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthSession(token, userResponse);
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found!"));
        return userMapper.toResponse(user);
    }
}

