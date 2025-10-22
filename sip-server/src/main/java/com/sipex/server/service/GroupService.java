package com.sipex.server.service;

import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import com.sipex.server.mapper.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    @Autowired
    private GroupMapper groupMapper;

    public Group createGroup(Group group) {
        groupMapper.insert(group);
        // 自动将创建者加入群组
        groupMapper.addMember(group.getId(), group.getCreatorId());
        return group;
    }

    public Group getGroupById(Long id) {
        return groupMapper.findById(id);
    }

    public List<Group> getUserGroups(Long userId) {
        return groupMapper.findByUserId(userId);
    }

    public void addMember(Long groupId, Long userId) {
        groupMapper.addMember(groupId, userId);
    }

    public void removeMember(Long groupId, Long userId) {
        groupMapper.removeMember(groupId, userId);
    }

    public List<User> getGroupMembers(Long groupId) {
        return groupMapper.findMembers(groupId);
    }

    public List<Group> getAllGroups() {
        return groupMapper.findAll();
    }
}

