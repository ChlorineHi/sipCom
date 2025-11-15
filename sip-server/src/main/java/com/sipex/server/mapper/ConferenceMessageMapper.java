package com.sipex.server.mapper;

import com.sipex.common.entity.ConferenceMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConferenceMessageMapper {

    @Insert("INSERT INTO conference_messages (room_id, from_user, content, message_type) " +
            "VALUES (#{roomId}, #{fromUser}, #{content}, #{messageType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConferenceMessage message);

    @Select("SELECT * FROM conference_messages WHERE room_id = #{roomId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConferenceMessage> findByRoomId(@Param("roomId") String roomId, @Param("limit") int limit);

    @Select("SELECT * FROM conference_messages WHERE from_user = #{username} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConferenceMessage> findByUsername(@Param("username") String username, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM conference_messages WHERE room_id = #{roomId}")
    int countByRoomId(String roomId);

    @Select("SELECT COUNT(*) FROM conference_messages WHERE DATE(created_at) = CURDATE()")
    int countToday();
}
