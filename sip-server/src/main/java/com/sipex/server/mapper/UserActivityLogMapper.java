package com.sipex.server.mapper;

import com.sipex.common.entity.UserActivityLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserActivityLogMapper {

    @Insert("INSERT INTO user_activity_logs (username, activity_type, description, ip_address, user_agent, metadata) " +
            "VALUES (#{username}, #{activityType}, #{description}, #{ipAddress}, #{userAgent}, #{metadata})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserActivityLog log);

    @Select("SELECT * FROM user_activity_logs WHERE username = #{username} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserActivityLog> findByUsername(@Param("username") String username, @Param("limit") int limit);

    @Select("SELECT * FROM user_activity_logs WHERE activity_type = #{activityType} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserActivityLog> findByActivityType(@Param("activityType") String activityType, @Param("limit") int limit);

    @Select("SELECT * FROM user_activity_logs WHERE created_at BETWEEN #{startDate} AND #{endDate} ORDER BY created_at DESC")
    List<UserActivityLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Select("SELECT * FROM user_activity_logs ORDER BY created_at DESC LIMIT #{limit}")
    List<UserActivityLog> findRecent(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_activity_logs WHERE DATE(created_at) = CURDATE()")
    int countToday();
}
