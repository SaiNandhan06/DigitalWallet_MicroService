package com.wallet.auth.service;

import com.wallet.auth.client.WalletClient;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.SignupRequest;
import com.wallet.auth.dto.UserDto;
import com.wallet.auth.model.Role;
import com.wallet.auth.model.User;
import com.wallet.auth.repository.UserRepository;
import com.wallet.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WalletClient walletClient;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       WalletClient walletClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.walletClient = walletClient;
    }

    @Transactional
    public UserDto signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(request.getName(), request.getEmail(), encodedPassword, role);
        User savedUser = userRepository.save(user);

        // Call Wallet Service to initialize wallet balance to 0
        try {
            walletClient.initWallet(savedUser.getId());
            log.info("Initialized wallet for userId: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to initialize wallet for userId {}: {}", savedUser.getId(), e.getMessage());
        }

        return new UserDto(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole());
    }
}
