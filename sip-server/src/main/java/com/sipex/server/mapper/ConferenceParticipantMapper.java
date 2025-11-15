package com.sipex.server.mapper;

import com.sipex.common.entity.ConferenceParticipant;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConferenceParticipantMapper {

    @Insert("INSERT INTO conference_participants (room_id, username, is_video_enabled, is_audio_enabled) " +
            "VALUES (#{roomId}, #{username}, #{isVideoEnabled}, #{isAudioEnabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConferenceParticipant participant);

    @Update("UPDATE conference_participants SET left_at = #{leftAt}, duration = #{duration} " +
            "WHERE room_id = #{roomId} AND username = #{username}")
    int updateLeaveInfo(ConferenceParticipant participant);

    @Update("UPDATE conference_participants SET messages_sent = messages_sent + 1 " +
            "WHERE room_id = #{roomId} AND username = #{username}")
    int incrementMessageCount(@Param("roomId") String roomId, @Param("username") String username);

    @Select("SELECT * FROM conference_participants WHERE room_id = #{roomId} ORDER BY joined_at")
    List<ConferenceParticipant> findByRoomId(String roomId);

    @Select("SELECT * FROM conference_participants WHERE username = #{username} ORDER BY joined_at DESC LIMIT #{limit}")
    List<ConferenceParticipant> findByUsername(@Param("username") String username, @Param("limit") int limit);

    @Select("SELECT * FROM conference_participants WHERE room_id = #{roomId} AND username = #{username}")
    ConferenceParticipant findByRoomIdAndUsername(@Param("roomId") String roomId, @Param("username") String username);

    @Select("SELECT COUNT(DISTINCT username) FROM conference_participants WHERE room_id = #{roomId}")
    int countByRoomId(String roomId);

    @Select("SELECT COUNT(DISTINCT username) FROM conference_participants WHERE DATE(joined_at) = CURDATE()")
    int countTodayActiveUsers();
}
