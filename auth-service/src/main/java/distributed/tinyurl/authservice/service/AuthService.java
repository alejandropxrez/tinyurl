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
import distributed.tinyurl.authservice.repository.RefreshTokenRepository;
import distributed.tinyurl.authservice.repository.UserRepository;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenProperties refreshTokenProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.clock = clock;
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

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return createTokenPair(user);
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = findValidRefreshToken(request.refreshToken());
        refreshToken.setRevokedAt(Instant.now(clock));

        return createTokenPair(refreshToken.getUser());
    }

    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = findValidRefreshToken(request.refreshToken());
        refreshToken.setRevokedAt(Instant.now(clock));
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

    private RefreshToken findValidRefreshToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashRefreshToken(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
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
