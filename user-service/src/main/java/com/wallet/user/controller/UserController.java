package com.wallet.user.controller;

import com.wallet.user.dto.UpdateUserRequest;
import com.wallet.user.model.UserProfile;
import com.wallet.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable Long id) {
        UserProfile profile = userService.getProfile(id);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        UserProfile updated = userService.updateProfile(id, request);
        return ResponseEntity.ok(updated);
    }
}
