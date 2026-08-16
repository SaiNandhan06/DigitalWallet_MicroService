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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WalletClient walletClient;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Alice", "alice@example.com", "hashed_password", Role.USER);
        testUser.setId(1L);
    }

    @Test
    void signup_successfullyCreatesUser() {
        SignupRequest request = new SignupRequest("Alice", "alice@example.com", "password123", Role.USER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto dto = authService.signup(request);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("alice@example.com", dto.getEmail());
        verify(walletClient, times(1)).initWallet(1L);
    }

    @Test
    void signup_throwsWhenEmailExists() {
        SignupRequest request = new SignupRequest("Alice", "alice@example.com", "password123", Role.USER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.signup(request));
    }

    @Test
    void login_returnsJwtToken() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "alice@example.com", "USER")).thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void login_throwsWhenInvalidPassword() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong_password");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> authService.login(request));
    }
}
