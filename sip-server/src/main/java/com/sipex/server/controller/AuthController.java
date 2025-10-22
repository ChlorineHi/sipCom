package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.LoginRequest;
import com.sipex.common.dto.RegisterRequest;
import com.sipex.common.entity.User;
import com.sipex.server.service.UserService;
import com.sipex.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword());
            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("token", token);

            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            String token = jwtUtil.generateToken(user.getUsername());

            // 更新用户状态为在线
            userService.updateStatus(user.getUsername(), "ONLINE");

            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("token", token);

            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam String username) {
        try {
            userService.updateStatus(username, "OFFLINE");
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

