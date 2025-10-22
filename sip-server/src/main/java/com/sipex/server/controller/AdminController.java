package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.CallLog;
import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import com.sipex.server.service.CallLogService;
import com.sipex.server.service.GroupService;
import com.sipex.server.service.MessageService;
import com.sipex.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CallLogService callLogService;

    @Autowired
    private GroupService groupService;

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.countAllUsers());
            stats.put("totalMessages", messageService.countAllMessages());
            stats.put("todayMessages", messageService.countTodayMessages());
            stats.put("todayCallDuration", callLogService.getTodayTotalDuration());

            return ApiResponse.success(stats);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/groups")
    public ApiResponse<List<Group>> getAllGroups() {
        try {
            List<Group> groups = groupService.getAllGroups();
            return ApiResponse.success(groups);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/calls")
    public ApiResponse<List<CallLog>> getAllCallLogs() {
        try {
            List<CallLog> callLogs = callLogService.getAllCallLogs();
            return ApiResponse.success(callLogs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

