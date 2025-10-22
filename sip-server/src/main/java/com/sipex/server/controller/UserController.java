package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.User;
import com.sipex.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{username}")
    public ApiResponse<User> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{userId}/friends")
    public ApiResponse<List<User>> getFriends(@PathVariable Long userId) {
        try {
            List<User> friends = userService.getFriends(userId);
            return ApiResponse.success(friends);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{username}/status")
    public ApiResponse<Void> updateStatus(@PathVariable String username, @RequestParam String status) {
        try {
            userService.updateStatus(username, status);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

