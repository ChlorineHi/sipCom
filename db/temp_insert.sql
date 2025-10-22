USE sipex;

INSERT IGNORE INTO `groups` (group_name, creator_id, description) VALUES
('ProjectTeam', 1, 'Project discussion'),
('TechTalk', 2, 'Technical topics');

INSERT IGNORE INTO `group_members` (group_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 2), (2, 4);

