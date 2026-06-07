package com.chamcham.backend.service;

import com.chamcham.backend.dto.auth.AuthGoogleRequest;
import com.chamcham.backend.dto.auth.AuthTokenResponse;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceGoogleIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Test
    void googleSignup_createsUserWithVerifiedEmail() {
        when(googleTokenVerifierService.verifyIdToken("valid-google-token")).thenReturn(
                new GoogleTokenVerifierService.VerifiedGoogleProfile(
                        "google-sub-123",
                        "google.new.user@test.pk",
                        "Google New User",
                        "https://avatar.example/new-user.png"
                )
        );

        AuthTokenResponse response = authService.authenticateWithGoogle(
                new AuthGoogleRequest("valid-google-token", UserRole.PLATFORM_ADMIN, null)
        );

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotNull(response.user());
        assertTrue(response.user().emailVerified());
        assertEquals("google.new.user@test.pk", response.user().email());
        assertEquals("platform_admin", response.user().role());

        User storedUser = userRepository.findByGoogleSubject("google-sub-123").orElseThrow();
        assertTrue(storedUser.isEmailVerified());
        assertEquals(UserRole.PLATFORM_ADMIN, storedUser.getRole());
    }

    @Test
    void googleLogin_linksExistingEmailAndMarksVerified() {
        User existing = userRepository.save(User.builder()
                .username("emailonlyuser")
                .email("existing.link@test.pk")
                .name("Existing User")
                .role(UserRole.BRAND)
                .active(true)
                .emailVerified(false)
                .build());

        when(googleTokenVerifierService.verifyIdToken("existing-email-token")).thenReturn(
                new GoogleTokenVerifierService.VerifiedGoogleProfile(
                        "google-sub-existing",
                        "existing.link@test.pk",
                        "Existing User",
                        "https://avatar.example/existing-user.png"
                )
        );

        AuthTokenResponse response = authService.authenticateWithGoogle(
                new AuthGoogleRequest("existing-email-token", UserRole.BRAND, null)
        );

        assertNotNull(response.accessToken());
        User linkedUser = userRepository.findById(existing.getId()).orElseThrow();
        assertEquals("google-sub-existing", linkedUser.getGoogleSubject());
        assertTrue(linkedUser.isEmailVerified());
        assertEquals("brand", response.user().role());
    }

    @Test
    void googleAuth_rejectsMissingRole() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.authenticateWithGoogle(new AuthGoogleRequest("any-token", null, null))
        );

        assertEquals(400, ex.getStatus().value());
        assertEquals("Role is required for Google authentication", ex.getMessage());
    }
}

