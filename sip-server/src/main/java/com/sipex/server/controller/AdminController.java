package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.CallLog;
import com.sipex.common.entity.DailyStatistic;
import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import com.sipex.common.entity.UserActivityLog;
import com.sipex.server.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @Autowired
    private ConferenceLogService conferenceLogService;

    @Autowired
    private ConferenceParticipantService conferenceParticipantService;

    @Autowired
    private DailyStatisticService dailyStatisticService;

    @Autowired
    private UserActivityLogService activityLogService;

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.countAllUsers());
            stats.put("totalMessages", messageService.countAllMessages());
            stats.put("todayMessages", messageService.countTodayMessages());
            stats.put("todayCallDuration", callLogService.getTodayTotalDuration());

            // 新增会议室统计
            stats.put("todayConferences", conferenceLogService.countToday());
            stats.put("totalConferences", conferenceLogService.countAll());
            stats.put("todayConferenceDuration", conferenceLogService.getTodayTotalDuration());
            stats.put("todayActiveUsers", conferenceParticipantService.countTodayActiveUsers());

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

    /**
     * 获取每日统计
     */
    @GetMapping("/daily-statistics")
    public ApiResponse<List<DailyStatistic>> getDailyStatistics(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<DailyStatistic> statistics = dailyStatisticService.getRecent(days);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 生成今日统计
     */
    @PostMapping("/daily-statistics/generate")
    public ApiResponse<DailyStatistic> generateTodayStatistics() {
        try {
            DailyStatistic statistic = dailyStatisticService.generateTodayStatistics();
            return ApiResponse.success(statistic);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 按日期范围查询统计
     */
    @GetMapping("/daily-statistics/range")
    public ApiResponse<List<DailyStatistic>> getStatisticsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DailyStatistic> statistics = dailyStatisticService.getByDateRange(startDate, endDate);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户活动日志
     */
    @GetMapping("/activity-logs")
    public ApiResponse<List<UserActivityLog>> getActivityLogs(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<UserActivityLog> logs = activityLogService.getRecent(limit);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取指定用户的活动日志
     */
    @GetMapping("/activity-logs/{username}")
    public ApiResponse<List<UserActivityLog>> getUserActivityLogs(
            @PathVariable String username,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<UserActivityLog> logs = activityLogService.getByUsername(username, limit);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
