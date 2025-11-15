-- ========================================
-- 后台记录功能增强 - 数据库迁移脚本
-- ========================================

USE sipex;

-- ========================================
-- 第一阶段：会议室记录核心功能
-- ========================================

-- 1. 会议室记录表
CREATE TABLE IF NOT EXISTS conference_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(50) NOT NULL COMMENT '会议室ID',
    creator VARCHAR(50) NOT NULL COMMENT '创建者用户名',
    room_name VARCHAR(100) COMMENT '会议室名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ended_at TIMESTAMP NULL COMMENT '结束时间',
    duration INT DEFAULT 0 COMMENT '会议时长（秒）',
    max_participants INT DEFAULT 0 COMMENT '最大参与人数',
    total_messages INT DEFAULT 0 COMMENT '总消息数',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, ENDED, EXPIRED',
    INDEX idx_room_id (room_id),
    INDEX idx_creator (creator),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室记录表';

-- 2. 会议室参与者记录表
CREATE TABLE IF NOT EXISTS conference_participants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(50) NOT NULL COMMENT '会议室ID',
    username VARCHAR(50) NOT NULL COMMENT '参与者用户名',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    left_at TIMESTAMP NULL COMMENT '离开时间',
    duration INT DEFAULT 0 COMMENT '参与时长（秒）',
    is_video_enabled BOOLEAN DEFAULT TRUE COMMENT '是否开启视频',
    is_audio_enabled BOOLEAN DEFAULT TRUE COMMENT '是否开启音频',
    messages_sent INT DEFAULT 0 COMMENT '发送消息数',
    INDEX idx_room_id (room_id),
    INDEX idx_username (username),
    INDEX idx_joined_at (joined_at),
    UNIQUE KEY uk_room_username (room_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室参与者记录表';

-- 3. 会议室消息持久化表
CREATE TABLE IF NOT EXISTS conference_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(50) NOT NULL COMMENT '会议室ID',
    from_user VARCHAR(50) NOT NULL COMMENT '发送者用户名',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text, system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_room_id (room_id),
    INDEX idx_from_user (from_user),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室消息表';

-- ========================================
-- 第二阶段：统计功能增强
-- ========================================

-- 4. 每日统计表
CREATE TABLE IF NOT EXISTS daily_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL UNIQUE COMMENT '统计日期',
    total_users INT DEFAULT 0 COMMENT '总用户数',
    active_users INT DEFAULT 0 COMMENT '活跃用户数',
    total_calls INT DEFAULT 0 COMMENT '通话次数',
    total_call_duration INT DEFAULT 0 COMMENT '通话时长（秒）',
    total_messages INT DEFAULT 0 COMMENT '消息数',
    total_conferences INT DEFAULT 0 COMMENT '会议室数',
    total_conference_duration INT DEFAULT 0 COMMENT '会议总时长（秒）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- ========================================
-- 第三阶段：用户行为日志
-- ========================================

-- 5. 用户活动日志表
CREATE TABLE IF NOT EXISTS user_activity_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    activity_type VARCHAR(50) NOT NULL COMMENT '活动类型: LOGIN, LOGOUT, CALL, MESSAGE, JOIN_CONFERENCE, etc.',
    description TEXT COMMENT '活动描述',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    metadata JSON COMMENT '额外元数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    INDEX idx_username (username),
    INDEX idx_activity_type (activity_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户活动日志表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入今日统计记录（如果不存在）
INSERT INTO daily_statistics (stat_date)
VALUES (CURDATE())
ON DUPLICATE KEY UPDATE stat_date = stat_date;
