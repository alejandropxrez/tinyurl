package distributed.tinyurl.authservice.service;

import distributed.tinyurl.authservice.config.RefreshTokenProperties;
import distributed.tinyurl.authservice.dto.LoginRequest;
import distributed.tinyurl.authservice.dto.LoginResponse;
import distributed.tinyurl.authservice.dto.RefreshTokenRequest;
import distributed.tinyurl.authservice.dto.RegisterRequest;
import distributed.tinyurl.authservice.dto.RegisterResponse;
import distributed.tinyurl.authservice.exception.EmailAlreadyRegisteredException;
import distributed.tinyurl.authservice.exception.InvalidCredentialsException;
import distributed.tinyurl.authservice.exception.InvalidRefreshTokenException;
import distributed.tinyurl.authservice.model.RefreshToken;
import distributed.tinyurl.authservice.model.User;
import distributed.tinyurl.authservice.observability.AuthMetricOperation;
import distributed.tinyurl.authservice.repository.RefreshTokenRepository;
import distributed.tinyurl.authservice.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import static distributed.tinyurl.authservice.observability.MetricName.AUTH_LOGINS;
import static distributed.tinyurl.authservice.observability.MetricName.AUTH_LOGOUTS;
import static distributed.tinyurl.authservice.observability.MetricName.AUTH_REFRESHES;
import static distributed.tinyurl.authservice.observability.MetricName.AUTH_REGISTRATIONS;
import static distributed.tinyurl.authservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.authservice.observability.MetricTagValue.FAILURE;
import static distributed.tinyurl.authservice.observability.MetricTagValue.SUCCESS;

@Service
@Transactional
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenProperties refreshTokenProperties,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);
        meterRegistry.counter(AUTH_REGISTRATIONS.key(), OUTCOME.key(), SUCCESS.key()).increment();

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    meterRegistry.counter(AUTH_LOGINS.key(), OUTCOME.key(), FAILURE.key()).increment();
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            meterRegistry.counter(AUTH_LOGINS.key(), OUTCOME.key(), FAILURE.key()).increment();
            throw new InvalidCredentialsException();
        }

        meterRegistry.counter(AUTH_LOGINS.key(), OUTCOME.key(), SUCCESS.key()).increment();
        return createTokenPair(user);
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = findValidRefreshToken(request.refreshToken(), AuthMetricOperation.REFRESH);
        refreshToken.setRevokedAt(Instant.now(clock));

        meterRegistry.counter(AUTH_REFRESHES.key(), OUTCOME.key(), SUCCESS.key()).increment();
        return createTokenPair(refreshToken.getUser());
    }

    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = findValidRefreshToken(request.refreshToken(), AuthMetricOperation.LOGOUT);
        refreshToken.setRevokedAt(Instant.now(clock));
        meterRegistry.counter(AUTH_LOGOUTS.key(), OUTCOME.key(), SUCCESS.key()).increment();
    }

    private LoginResponse createTokenPair(User user) {
        String refreshToken = generateRefreshToken();
        Instant now = Instant.now(clock);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashRefreshToken(refreshToken))
                .expiresAt(now.plus(refreshTokenProperties.expiration()))
                .build());

        return new LoginResponse(
                jwtService.createAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.expiresInSeconds(),
                refreshTokenProperties.expiration().toSeconds()
        );
    }

    private RefreshToken findValidRefreshToken(String rawRefreshToken, AuthMetricOperation operation) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> {
                    recordInvalidRefreshToken(operation);
                    return new InvalidRefreshTokenException();
                });

        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(Instant.now(clock))) {
            recordInvalidRefreshToken(operation);
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    private void recordInvalidRefreshToken(AuthMetricOperation operation) {
        meterRegistry.counter(operation.metricName().key(), OUTCOME.key(), FAILURE.key()).increment();
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash refresh token", ex);
        }
    }
}
