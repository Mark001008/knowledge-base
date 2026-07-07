-- ============================================================
-- 问答会话权限补丁
-- 适用于已有环境升级：将会话创建、编辑、删除从 qa:view 中拆出。
-- ============================================================

INSERT IGNORE INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('qa:create', '创建会话', '问答', '创建问答会话'),
  ('qa:update', '编辑会话', '问答', '重命名问答会话'),
  ('qa:delete', '删除会话', '问答', '删除问答会话');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code IN ('SYSTEM_ADMIN', 'KB_ADMIN', 'USER')
  AND p.permission_code IN ('qa:create', 'qa:update', 'qa:delete');
