package com.sipex.server.service;

import com.sipex.common.entity.User;
import com.sipex.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Value("${kamailio.host}")
    private String kamailioHost;

    public User register(String username, String password) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户（实际应用中密码应该加密）
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 简单起见，这里不加密
        user.setSipUri("sip:" + username + "@" + kamailioHost);
        user.setStatus("OFFLINE");

        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    public void updateStatus(String username, String status) {
        userMapper.updateStatus(username, status);
    }

    public List<User> getFriends(Long userId) {
        return userMapper.findFriends(userId);
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    public int countAllUsers() {
        return userMapper.countAll();
    }
}

