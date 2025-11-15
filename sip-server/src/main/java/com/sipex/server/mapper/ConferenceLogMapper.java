package com.sipex.server.mapper;

import com.sipex.common.entity.ConferenceLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConferenceLogMapper {

    @Insert("INSERT INTO conference_logs (room_id, creator, room_name, status) " +
            "VALUES (#{roomId}, #{creator}, #{roomName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConferenceLog log);

    @Update("UPDATE conference_logs SET ended_at = #{endedAt}, duration = #{duration}, " +
            "max_participants = #{maxParticipants}, total_messages = #{totalMessages}, status = #{status} " +
            "WHERE room_id = #{roomId}")
    int updateByRoomId(ConferenceLog log);

    @Update("UPDATE conference_logs SET total_messages = total_messages + 1 WHERE room_id = #{roomId}")
    int incrementMessageCount(String roomId);

    @Update("UPDATE conference_logs SET max_participants = #{maxParticipants} " +
            "WHERE room_id = #{roomId} AND max_participants < #{maxParticipants}")
    int updateMaxParticipants(@Param("roomId") String roomId, @Param("maxParticipants") int maxParticipants);

    @Select("SELECT * FROM conference_logs WHERE room_id = #{roomId}")
    ConferenceLog findByRoomId(String roomId);

    @Select("SELECT * FROM conference_logs WHERE creator = #{creator} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConferenceLog> findByCreator(@Param("creator") String creator, @Param("limit") int limit);

    @Select("SELECT * FROM conference_logs WHERE status = #{status} ORDER BY created_at DESC")
    List<ConferenceLog> findByStatus(String status);

    @Select("SELECT * FROM conference_logs ORDER BY created_at DESC LIMIT #{limit}")
    List<ConferenceLog> findAll(@Param("limit") int limit);

    @Select("SELECT * FROM conference_logs WHERE created_at BETWEEN #{startDate} AND #{endDate} ORDER BY created_at DESC")
    List<ConferenceLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Select("SELECT COUNT(*) FROM conference_logs WHERE DATE(created_at) = CURDATE()")
    int countToday();

    @Select("SELECT COUNT(*) FROM conference_logs")
    int countAll();

    @Select("SELECT SUM(duration) FROM conference_logs WHERE DATE(created_at) = CURDATE() AND status = 'ENDED'")
    Long getTodayTotalDuration();
}
