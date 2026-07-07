-- ============================================================
-- 原始文档下载权限补丁
-- 适用于已有环境升级：补齐 document:download 权限和内置角色授权。
-- ============================================================

INSERT IGNORE INTO sys_permission (permission_code, permission_name, module, description)
VALUES ('document:download', '下载原文', '文档', '下载原始文档用于核验');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code IN ('SYSTEM_ADMIN', 'KB_ADMIN', 'USER')
  AND p.permission_code = 'document:download';
