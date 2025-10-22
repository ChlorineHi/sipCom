package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.entity.Group;
import com.sipex.common.entity.User;
import com.sipex.server.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ApiResponse<Group> createGroup(@RequestBody Group group) {
        try {
            Group createdGroup = groupService.createGroup(group);
            return ApiResponse.success(createdGroup);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{groupId}")
    public ApiResponse<Group> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.getGroupById(groupId);
            return ApiResponse.success(group);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Group>> getUserGroups(@PathVariable Long userId) {
        try {
            List<Group> groups = groupService.getUserGroups(userId);
            return ApiResponse.success(groups);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<Void> addMember(@PathVariable Long groupId, @RequestParam Long userId) {
        try {
            groupService.addMember(groupId, userId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        try {
            groupService.removeMember(groupId, userId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<User>> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<User> members = groupService.getGroupMembers(groupId);
            return ApiResponse.success(members);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<Group>> getAllGroups() {
        try {
            List<Group> groups = groupService.getAllGroups();
            return ApiResponse.success(groups);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

