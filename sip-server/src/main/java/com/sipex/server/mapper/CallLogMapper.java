package com.sipex.server.mapper;

import com.sipex.common.entity.CallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CallLogMapper {

    @Insert("INSERT INTO call_logs (caller, callee, call_type, status, duration, is_group_call, group_id) " +
            "VALUES (#{caller}, #{callee}, #{callType}, #{status}, #{duration}, #{isGroupCall}, #{groupId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CallLog callLog);

    @Select("SELECT * FROM call_logs WHERE caller = #{username} OR callee = #{username} ORDER BY created_at DESC LIMIT #{limit}")
    List<CallLog> findByUsername(@Param("username") String username, @Param("limit") int limit);

    @Select("SELECT * FROM call_logs WHERE is_group_call = true AND group_id = #{groupId} ORDER BY created_at DESC")
    List<CallLog> findByGroupId(Long groupId);

    @Select("SELECT SUM(duration) FROM call_logs WHERE DATE(created_at) = CURDATE()")
    Long getTodayTotalDuration();

    @Select("SELECT * FROM call_logs ORDER BY created_at DESC")
    List<CallLog> findAll();
}

