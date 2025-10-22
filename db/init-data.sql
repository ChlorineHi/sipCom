USE sipex;

-- 插入测试用户（密码都是: 123456）
-- 密码使用BCrypt加密，这里使用简单的明文，实际应用中会在程序中加密
INSERT INTO users (username, password, sip_uri, status) VALUES
('alice', '$2a$10$EIXUd6aVw6FKqFQBdQa8Q.RZG3bGxnZXFB6xJqvDxKxBKHQXJQY8K', 'sip:alice@172.22.189.160', 'OFFLINE'),
('bob', '$2a$10$EIXUd6aVw6FKqFQBdQa8Q.RZG3bGxnZXFB6xJqvDxKxBKHQXJQY8K', 'sip:bob@172.22.189.160', 'OFFLINE'),
('charlie', '$2a$10$EIXUd6aVw6FKqFQBdQa8Q.RZG3bGxnZXFB6xJqvDxKxBKHQXJQY8K', 'sip:charlie@172.22.189.160', 'OFFLINE'),
('david', '$2a$10$EIXUd6aVw6FKqFQBdQa8Q.RZG3bGxnZXFB6xJqvDxKxBKHQXJQY8K', 'sip:david@172.22.189.160', 'OFFLINE');

-- 建立好友关系
INSERT INTO friendships (user_id, friend_id) VALUES
(1, 2), (2, 1),  -- alice 和 bob 互为好友
(1, 3), (3, 1),  -- alice 和 charlie 互为好友
(2, 3), (3, 2),  -- bob 和 charlie 互为好友
(1, 4), (4, 1);  -- alice 和 david 互为好友

-- 创建测试群组
INSERT INTO `groups` (group_name, creator_id, description) VALUES
('项目讨论组', 1, '项目开发讨论'),
('技术交流', 2, '技术相关话题');

-- 添加群成员
INSERT INTO `group_members` (group_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),  -- alice, bob, charlie 在项目讨论组
(2, 1), (2, 2), (2, 4);  -- alice, bob, david 在技术交流组

