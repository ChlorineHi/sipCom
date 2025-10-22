package com.sipex.server.service;

import com.sipex.common.entity.CallLog;
import com.sipex.server.mapper.CallLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CallLogService {

    @Autowired
    private CallLogMapper callLogMapper;

    public CallLog saveCallLog(CallLog callLog) {
        callLogMapper.insert(callLog);
        return callLog;
    }

    public List<CallLog> getUserCallLogs(String username, int limit) {
        return callLogMapper.findByUsername(username, limit);
    }

    public List<CallLog> getGroupCallLogs(Long groupId) {
        return callLogMapper.findByGroupId(groupId);
    }

    public Long getTodayTotalDuration() {
        Long duration = callLogMapper.getTodayTotalDuration();
        return duration != null ? duration : 0L;
    }

    public List<CallLog> getAllCallLogs() {
        return callLogMapper.findAll();
    }
}

