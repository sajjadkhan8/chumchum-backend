package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.AuthLoginRequest;
import com.chamcham.backend.dto.auth.AuthRegisterRequest;
import com.chamcham.backend.dto.brand.BrandCreateRequest;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
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
    private final CreatorService creatorService;
    private final BrandService brandService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            CreatorService creatorService,
            BrandService brandService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.creatorService = creatorService;
        this.brandService = brandService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(AuthRegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a unique username!");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already exists!");
        }

        validateRolePayload(request);

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .image(request.image())
                .city(request.city())
                .phone(request.phone())
                .role(request.role())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        createRoleProfile(savedUser, request);
        log.info("Created user {} with role {}", savedUser.getId(), savedUser.getRole());
        return userMapper.toResponse(savedUser);
    }

    public AuthSession login(AuthLoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.username(), request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Check username or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Check username or password!");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        String token = jwtService.generateToken(user.getId(), user.getRole());
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthSession(token, userResponse);
    }

    private void validateRolePayload(AuthRegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot be created from public signup");
        }

        if (request.role() == UserRole.CREATOR) {
            // Creator signup requires at least one creator field to be present
            if (request.bio() == null && request.category() == null && request.tiktokUrl() == null
                    && request.instagramUrl() == null && request.youtubeUrl() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Creator signup requires at least one profile field");
            }
        }

        if (request.role() == UserRole.BRAND) {
            // Brand signup requires company name
            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Brand signup requires company name");
            }
        }
    }

    private void createRoleProfile(User user, AuthRegisterRequest request) {
        switch (request.role()) {
            case CREATOR -> creatorService.create(new CreatorCreateRequest(
                    user.getId(),
                    request.bio(),
                    request.category(),
                    request.tiktokUrl(),
                    request.instagramUrl(),
                    request.youtubeUrl()
            ));
            case BRAND -> brandService.create(new BrandCreateRequest(
                    user.getId(),
                    request.companyName(),
                    request.website(),
                    request.industry(),
                    request.description()
            ));
            case ADMIN -> {
                // Guarded by validateRolePayload
            }
        }
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found!"));
        return userMapper.toResponse(user);
    }
}
