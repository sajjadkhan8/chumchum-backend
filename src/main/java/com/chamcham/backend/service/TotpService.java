package com.chamcham.backend.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TotpService {

    private static final int SECRET_LENGTH = 32;
    // Allow ±1 time window (30-second clock drift tolerance)
    private static final int ALLOWED_DISCREPANCY = 1;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_LENGTH);
    private final DefaultCodeVerifier codeVerifier;

    @Value("${app.name:ChamCham}")
    private String issuerName;

    public TotpService() {
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
        this.codeVerifier.setTimePeriod(30);
        this.codeVerifier.setAllowedTimePeriodDiscrepancy(ALLOWED_DISCREPANCY);
    }

    /** Generates a new base32-encoded TOTP secret. */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Returns the otpauth:// URI for seeding an authenticator app.
     * The frontend should render this as a QR code (e.g. with qrcode.react).
     */
    public String buildOtpAuthUri(String secret, String email) {
        String encodedIssuer = URLEncoder.encode(issuerName, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + encodedIssuer + ":" + encodedEmail
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /** Returns true if the given 6-digit code is valid for the secret at the current time. */
    public boolean verify(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) return false;
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception ex) {
            return false;
        }
    }
}
