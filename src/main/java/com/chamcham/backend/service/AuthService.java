package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.AuthLoginRequest;
import com.chamcham.backend.dto.auth.AuthRegisterRequest;
import com.chamcham.backend.dto.user.BrandProfilePayload;
import com.chamcham.backend.dto.user.CreatorProfilePayload;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
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
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            CreatorRepository creatorRepository,
            BrandRepository brandRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(AuthRegisterRequest request) {
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
    }

    public AuthSession login(AuthLoginRequest request) {
        User user = userRepository.findByUsername(request.username())
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

        if (request.role() == UserRole.CREATOR && request.brand() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Brand profile is not allowed for creator signup");
        }

        if (request.role() == UserRole.BRAND && request.creator() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Creator profile is not allowed for brand signup");
        }
    }

    private void createRoleProfile(User user, AuthRegisterRequest request) {
        if (request.role() == UserRole.CREATOR && request.creator() != null) {
            creatorRepository.save(toCreator(user, request.creator()));
            return;
        }
        if (request.role() == UserRole.BRAND && request.brand() != null) {
            brandRepository.save(toBrand(user, request.brand()));
        }
    }

    private Creator toCreator(User user, CreatorProfilePayload payload) {
        return Creator.builder()
                .id(UUID.randomUUID())
                .user(user)
                .bio(payload.bio())
                .category(payload.category())
                .tiktokUrl(payload.tiktokUrl())
                .instagramUrl(payload.instagramUrl())
                .youtubeUrl(payload.youtubeUrl())
                .followers(payload.followers() == null ? 0 : payload.followers())
                .avgViews(payload.avgViews() == null ? 0 : payload.avgViews())
                .engagementRate(payload.engagementRate())
                .rating(payload.rating() == null ? java.math.BigDecimal.ZERO : payload.rating())
                .totalReviews(payload.totalReviews() == null ? 0 : payload.totalReviews())
                .build();
    }

    private Brand toBrand(User user, BrandProfilePayload payload) {
        if (payload.companyName() == null || payload.companyName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Brand companyName is required");
        }

        return Brand.builder()
                .id(UUID.randomUUID())
                .user(user)
                .companyName(payload.companyName())
                .website(payload.website())
                .industry(payload.industry())
                .description(payload.description())
                .build();
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found!"));
        return userMapper.toResponse(user);
    }
}

