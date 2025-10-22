-- 创建数据库
CREATE DATABASE IF NOT EXISTS sipex DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sipex;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    sip_uri VARCHAR(255) NOT NULL UNIQUE COMMENT 'SIP URI',
    status VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '在线状态: ONLINE, OFFLINE, BUSY, AWAY',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_sip_uri (sip_uri),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user VARCHAR(50) NOT NULL COMMENT '发送者用户名',
    to_user VARCHAR(50) NOT NULL COMMENT '接收者用户名',
    content TEXT NOT NULL COMMENT '消息内容',
    type VARCHAR(20) NOT NULL COMMENT '消息类型: TEXT, IMAGE, AUDIO, VIDEO',
    file_url VARCHAR(500) COMMENT '文件URL（非文本消息）',
    is_group BOOLEAN DEFAULT FALSE COMMENT '是否是群消息',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_from_user (from_user),
    INDEX idx_to_user (to_user),
    INDEX idx_created_at (created_at),
    INDEX idx_is_group (is_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 群组表
CREATE TABLE IF NOT EXISTS `groups` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL COMMENT '群组名称',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    avatar VARCHAR(500) COMMENT '群头像',
    description TEXT COMMENT '群描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_creator_id (creator_id),
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群成员表
CREATE TABLE IF NOT EXISTS `group_members` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

-- 通话记录表
CREATE TABLE IF NOT EXISTS call_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    caller VARCHAR(50) NOT NULL COMMENT '主叫用户名',
    callee VARCHAR(50) NOT NULL COMMENT '被叫用户名',
    call_type VARCHAR(20) NOT NULL COMMENT '通话类型: AUDIO, VIDEO',
    status VARCHAR(20) NOT NULL COMMENT '通话状态: MISSED, REJECTED, COMPLETED',
    duration INT DEFAULT 0 COMMENT '通话时长（秒）',
    is_group_call BOOLEAN DEFAULT FALSE COMMENT '是否群组通话',
    group_id BIGINT COMMENT '群组ID（如果是群组通话）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '通话时间',
    INDEX idx_caller (caller),
    INDEX idx_callee (callee),
    INDEX idx_created_at (created_at),
    INDEX idx_is_group_call (is_group_call)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话记录表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS friendships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

