package com.chamcham.backend.service;

import com.chamcham.backend.config.security.JwtService;
import com.chamcham.backend.dto.auth.AuthLoginRequest;
import com.chamcham.backend.dto.auth.AuthRegisterRequest;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private AuthRegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthRegisterRequest(
                "john",
                "john@example.com",
                "password123",
                null,
                "India",
                "9999999999",
                "Senior seller",
                true
        );
    }

    @Test
    void register_throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unique username");
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .username("john")
                .passwordHash("hashed")
                .seller(true)
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(id, true)).thenReturn("jwt-token");

        AuthSession session = authService.login(new AuthLoginRequest("john", "password123"));

        assertThat(session.token()).isEqualTo("jwt-token");
    }

    @Test
    void register_persistsHashedPassword() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        authService.register(registerRequest);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
    }
}

