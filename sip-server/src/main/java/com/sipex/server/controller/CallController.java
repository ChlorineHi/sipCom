package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.CallRequest;
import com.sipex.common.entity.CallLog;
import com.sipex.server.service.CallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    @Autowired
    private CallLogService callLogService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/logs")
    public ApiResponse<CallLog> saveCallLog(@RequestBody CallLog callLog) {
        try {
            CallLog savedLog = callLogService.saveCallLog(callLog);
            return ApiResponse.success(savedLog);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/logs/{username}")
    public ApiResponse<List<CallLog>> getUserCallLogs(
            @PathVariable String username,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<CallLog> logs = callLogService.getUserCallLogs(username, limit);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/logs/group/{groupId}")
    public ApiResponse<List<CallLog>> getGroupCallLogs(@PathVariable Long groupId) {
        try {
            List<CallLog> logs = callLogService.getGroupCallLogs(groupId);
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/signal")
    public ApiResponse<Void> sendCallSignal(@RequestBody CallRequest callRequest) {
        try {
            // 转发呼叫信号到目标用户
            messagingTemplate.convertAndSend("/queue/call/" + callRequest.getCallee(), callRequest);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

