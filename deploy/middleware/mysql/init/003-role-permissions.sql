USE kb_base_db;

-- ============================================================
-- 权限体系重构：三级角色
-- 超级管理员(SYSTEM_ADMIN) > 知识库管理员(KB_ADMIN) > 普通用户(USER)
-- ============================================================

-- 1. 更新角色信息
UPDATE sys_role SET role_name = '超级管理员', description = '超级管理员，拥有全部权限', builtin = 1 WHERE role_code = 'SYSTEM_ADMIN';
UPDATE sys_role SET description = '普通用户，仅可查看和提问', builtin = 1 WHERE role_code = 'USER';

-- 2. 新增知识库管理员角色
INSERT IGNORE INTO sys_role (role_code, role_name, description, builtin, status)
VALUES ('KB_ADMIN', '知识库管理员', '可管理知识库、文档、成员和配置，不能管理系统设置', 1, 'ENABLED');

-- 3. 清空原有角色权限关联（重新分配）
DELETE FROM sys_role_permission WHERE role_id IN (SELECT id FROM sys_role WHERE role_code IN ('SYSTEM_ADMIN', 'KB_ADMIN', 'USER'));
DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM sys_role WHERE role_code IN ('SYSTEM_ADMIN', 'KB_ADMIN', 'USER'));

-- 4. 超级管理员：全部权限 + 全部菜单
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SYSTEM_ADMIN';

-- 5. 知识库管理员：知识库 + 文档 + 成员 + 配置 + 问答
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'KB_ADMIN'
  AND p.permission_code IN (
    'space:view', 'space:create', 'space:update', 'space:delete',
    'document:view', 'document:upload', 'document:create', 'document:update', 'document:delete', 'document:rebuild',
    'qa:view', 'qa:ask',
    'member:view', 'member:add', 'member:update', 'member:remove',
    'setting:view', 'setting:update'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'KB_ADMIN'
  AND m.path IN ('/workspace', '/recent-qa');

-- 6. 普通用户：只读 + 问答提问
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'USER'
  AND p.permission_code IN (
    'space:view',
    'document:view',
    'qa:view', 'qa:ask',
    'member:view',
    'setting:view'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'USER'
  AND m.path IN ('/workspace', '/recent-qa');
