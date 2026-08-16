package com.wallet.user.service;

import com.wallet.user.dto.UpdateUserRequest;
import com.wallet.user.model.UserProfile;
import com.wallet.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile getProfile(Long id) {
        return userProfileRepository.findById(id)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(id, "User " + id, "user" + id + "@wallet.com")));
    }

    @Transactional
    public UserProfile updateProfile(Long id, UpdateUserRequest request) {
        UserProfile profile = getProfile(id);
        if (request.getName() != null && !request.getName().isBlank()) {
            profile.setName(request.getName());
        }
        return userProfileRepository.save(profile);
    }
}
