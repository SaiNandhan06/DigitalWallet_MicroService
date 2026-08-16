package com.wallet.user.service;

import com.wallet.user.dto.UpdateUserRequest;
import com.wallet.user.model.UserProfile;
import com.wallet.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserService userService;

    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new UserProfile(1L, "Alice", "alice@wallet.com");
    }

    @Test
    void getProfile_returnsExistingProfile() {
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));

        UserProfile profile = userService.getProfile(1L);

        assertNotNull(profile);
        assertEquals("Alice", profile.getName());
    }

    @Test
    void getProfile_createsFallbackIfNotExists() {
        when(userProfileRepository.findById(2L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = userService.getProfile(2L);

        assertNotNull(profile);
        assertEquals(2L, profile.getId());
        assertEquals("User 2", profile.getName());
    }

    @Test
    void updateProfile_updatesName() {
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest("Alice Updated");
        UserProfile updated = userService.updateProfile(1L, request);

        assertEquals("Alice Updated", updated.getName());
    }
}
