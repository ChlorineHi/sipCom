package com.sipex.server.mapper;

import com.sipex.common.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO messages (from_user, to_user, content, type, file_url, is_group, is_read) " +
            "VALUES (#{fromUser}, #{toUser}, #{content}, #{type}, #{fileUrl}, #{isGroup}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Select("SELECT * FROM messages WHERE (from_user = #{user1} AND to_user = #{user2}) OR (from_user = #{user2} AND to_user = #{user1}) " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<Message> findChatHistory(@Param("user1") String user1, @Param("user2") String user2, @Param("limit") int limit);

    @Select("SELECT * FROM messages WHERE to_user = #{username} AND is_group = false AND is_read = false")
    List<Message> findUnreadMessages(String username);

    @Update("UPDATE messages SET is_read = true WHERE id = #{id}")
    int markAsRead(Long id);

    @Select("SELECT * FROM messages WHERE to_user = #{toUser} AND is_group = #{isGroup} ORDER BY created_at DESC")
    List<Message> findByToUser(@Param("toUser") String toUser, @Param("isGroup") boolean isGroup);

    @Select("SELECT COUNT(*) FROM messages WHERE DATE(created_at) = CURDATE()")
    int countTodayMessages();

    @Select("SELECT COUNT(*) FROM messages")
    int countAll();
}

