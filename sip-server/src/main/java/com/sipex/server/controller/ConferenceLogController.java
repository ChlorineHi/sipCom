package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.ConferenceLog;
import com.sipex.common.entity.ConferenceMessage;
import com.sipex.common.entity.ConferenceParticipant;
import com.sipex.server.service.ConferenceLogService;
import com.sipex.server.service.ConferenceMessageService;
import com.sipex.server.service.ConferenceParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会议室记录查询Controller
 */
@RestController
@RequestMapping("/api/conference/logs")
public class ConferenceLogController {

    @Autowired
    private ConferenceLogService conferenceLogService;

    @Autowired
    private ConferenceParticipantService participantService;

    @Autowired
    private ConferenceMessageService conferenceMessageService;

    /**
     * 获取会议室记录详情
     */
    @GetMapping("/{roomId}")
    public ApiResponse<ConferenceLog> getLogByRoomId(@PathVariable String roomId) {
        try {
            ConferenceLog log = conferenceLogService.getByRoomId(roomId);
            if (log == null) {
                return ApiResponse.error("会议室记录不存在");
            }
            return ApiResponse.success(log);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取会议室参与者记录
     */
    @GetMapping("/{roomId}/participants")
    public ApiResponse<List<ConferenceParticipant>> getParticipants(@PathVariable String roomId) {
        try {
            List<ConferenceParticipant> participants = participantService.getByRoomId(roomId);
            return ApiResponse.success(participants);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取会议室消息记录（持久化版本）
     */
    @GetMapping("/{roomId}/messages-history")
    public ApiResponse<List<ConferenceMessage>> getMessagesHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ConferenceMessage> messages = conferenceMessageService.getByRoomId(roomId, limit);
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询用户创建的会议室记录
     */
    @GetMapping("/creator/{username}")
    public ApiResponse<List<ConferenceLog>> getByCreator(
            @PathVariable String username,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<ConferenceLog> logs = conferenceLogService.getByCreator(username, limit);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询用户参与的会议室记录
     */
    @GetMapping("/participant/{username}")
    public ApiResponse<List<ConferenceParticipant>> getUserParticipation(
            @PathVariable String username,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<ConferenceParticipant> participants = participantService.getByUsername(username, limit);
            return ApiResponse.success(participants);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 根据状态查询会议室
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<ConferenceLog>> getByStatus(@PathVariable String status) {
        try {
            List<ConferenceLog> logs = conferenceLogService.getByStatus(status);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 查询所有会议室记录
     */
    @GetMapping("/all")
    public ApiResponse<List<ConferenceLog>> getAll(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ConferenceLog> logs = conferenceLogService.getAll(limit);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 按日期范围查询
     */
    @GetMapping("/range")
    public ApiResponse<List<ConferenceLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<ConferenceLog> logs = conferenceLogService.getByDateRange(startDate, endDate);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 会议室统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("todayConferences", conferenceLogService.countToday());
            stats.put("todayDuration", conferenceLogService.getTodayTotalDuration());
            stats.put("totalConferences", conferenceLogService.countAll());
            stats.put("todayActiveUsers", participantService.countTodayActiveUsers());

            return ApiResponse.success(stats);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
