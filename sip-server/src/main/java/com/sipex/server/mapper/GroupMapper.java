package com.sipex.server.mapper;

import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GroupMapper {

    @Insert("INSERT INTO `groups` (group_name, creator_id, description) VALUES (#{groupName}, #{creatorId}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Group group);

    @Select("SELECT * FROM `groups` WHERE id = #{id}")
    Group findById(Long id);

    @Select("SELECT g.* FROM `groups` g INNER JOIN group_members gm ON g.id = gm.group_id WHERE gm.user_id = #{userId}")
    List<Group> findByUserId(Long userId);

    @Insert("INSERT INTO group_members (group_id, user_id) VALUES (#{groupId}, #{userId})")
    int addMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Delete("DELETE FROM group_members WHERE group_id = #{groupId} AND user_id = #{userId}")
    int removeMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Select("SELECT u.* FROM users u INNER JOIN group_members gm ON u.id = gm.user_id WHERE gm.group_id = #{groupId}")
    List<User> findMembers(Long groupId);

    @Select("SELECT * FROM `groups`")
    List<Group> findAll();
}

