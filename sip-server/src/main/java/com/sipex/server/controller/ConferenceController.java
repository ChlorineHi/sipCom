package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.ConferenceRequest;
import com.sipex.common.dto.ConferenceResponse;
import com.sipex.common.dto.ConferenceRoom;
import com.sipex.common.dto.ConferenceMessageDTO;
import com.sipex.server.service.ConferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室控制器
 * 提供会议室管理API
 */
@RestController
@RequestMapping("/api/conference")
public class ConferenceController {
    
    @Autowired
    private ConferenceService conferenceService;
    
    /**
     * 创建会议室
     */
    @PostMapping("/create")
    public ApiResponse<ConferenceResponse> createRoom(@RequestBody ConferenceRequest request) {
        try {
            ConferenceRoom room = conferenceService.createRoom(request.getUsername());
            ConferenceResponse response = ConferenceResponse.success(
                room.getRoomId(), 
                room.getParticipants()
            );
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 加入会议室
     */
    @PostMapping("/join")
    public ApiResponse<ConferenceResponse> joinRoom(@RequestBody ConferenceRequest request) {
        try {
            ConferenceRoom room = conferenceService.joinRoom(
                request.getRoomId(), 
                request.getUsername()
            );
            ConferenceResponse response = ConferenceResponse.success(
                room.getRoomId(), 
                room.getParticipants()
            );
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 离开会议室
     */
    @PostMapping("/leave")
    public ApiResponse<ConferenceResponse> leaveRoom(@RequestBody ConferenceRequest request) {
        try {
            ConferenceRoom room = conferenceService.leaveRoom(
                request.getRoomId(), 
                request.getUsername()
            );
            ConferenceResponse response = ConferenceResponse.success(
                room.getRoomId(), 
                room.getParticipants()
            );
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取会议室参与者列表
     */
    @GetMapping("/{roomId}")
    public ApiResponse<ConferenceResponse> getRoomInfo(@PathVariable String roomId) {
        try {
            ConferenceRoom room = conferenceService.getRoom(roomId);
            if (room == null) {
                return ApiResponse.error("会议室不存在");
            }
            ConferenceResponse response = ConferenceResponse.success(
                room.getRoomId(), 
                room.getParticipants()
            );
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 检查会议室是否存在
     */
    @GetMapping("/{roomId}/exists")
    public ApiResponse<Boolean> roomExists(@PathVariable String roomId) {
        boolean exists = conferenceService.roomExists(roomId);
        return ApiResponse.success(exists);
    }
    
    /**
     * 获取活跃会议室数量
     */
    @GetMapping("/active-count")
    public ApiResponse<Integer> getActiveRoomCount() {
        int count = conferenceService.getActiveRoomCount();
        return ApiResponse.success(count);
    }

    /**
     * 发送会议室消息
     */
    @PostMapping("/{roomId}/message")
    public ApiResponse<ConferenceMessageDTO> sendMessage(
            @PathVariable String roomId,
            @RequestBody ConferenceMessageDTO message) {
        try {
            ConferenceMessageDTO savedMessage = conferenceService.addMessage(roomId, message);
            return ApiResponse.success(savedMessage);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取会议室消息列表
     */
    @GetMapping("/{roomId}/messages")
    public ApiResponse<List<ConferenceMessageDTO>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<ConferenceMessageDTO> messages = conferenceService.getMessages(roomId, limit);
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

