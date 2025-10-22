package com.sipex.server.mapper;

import com.sipex.common.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE sip_uri = #{sipUri}")
    User findBySipUri(String sipUri);

    @Insert("INSERT INTO users (username, password, sip_uri, status) VALUES (#{username}, #{password}, #{sipUri}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET status = #{status}, updated_at = NOW() WHERE username = #{username}")
    int updateStatus(@Param("username") String username, @Param("status") String status);

    @Select("SELECT * FROM users WHERE id IN (SELECT friend_id FROM friendships WHERE user_id = #{userId})")
    List<User> findFriends(Long userId);

    @Select("SELECT * FROM users")
    List<User> findAll();

    @Select("SELECT COUNT(*) FROM users")
    int countAll();
}

