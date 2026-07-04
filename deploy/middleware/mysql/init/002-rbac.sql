USE kb_base_db;

-- ============================================================
-- RBAC权限系统表结构
-- ============================================================

-- 1. 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
  id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  permission_code  VARCHAR(128)  NOT NULL UNIQUE COMMENT '权限编码',
  permission_name  VARCHAR(64)   NOT NULL        COMMENT '权限名称',
  module           VARCHAR(64)   NOT NULL        COMMENT '所属模块',
  description      VARCHAR(255)                  COMMENT '权限描述',
  status           VARCHAR(32)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_permission_module (module),
  INDEX idx_permission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 2. 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
  id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
  parent_id        BIGINT        NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  menu_name        VARCHAR(64)   NOT NULL        COMMENT '菜单名称',
  menu_type        VARCHAR(32)   NOT NULL        COMMENT '菜单类型: CATALOG/MENU/BUTTON',
  path             VARCHAR(255)                  COMMENT '路由路径',
  component        VARCHAR(255)                  COMMENT '组件路径',
  icon             VARCHAR(64)                   COMMENT '图标',
  permission_code  VARCHAR(128)                  COMMENT '权限编码',
  sort             INT           NOT NULL DEFAULT 0 COMMENT '排序',
  visible          TINYINT       NOT NULL DEFAULT 1 COMMENT '是否可见: 0-隐藏 1-显示',
  status           VARCHAR(32)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_menu_parent (parent_id),
  INDEX idx_menu_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 3. 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id         BIGINT NOT NULL COMMENT '角色ID',
  permission_id   BIGINT NOT NULL COMMENT '权限ID',
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  INDEX idx_rp_role (role_id),
  INDEX idx_rp_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 4. 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id  BIGINT NOT NULL COMMENT '角色ID',
  menu_id  BIGINT NOT NULL COMMENT '菜单ID',
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  INDEX idx_rm_role (role_id),
  INDEX idx_rm_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ============================================================
-- 扩展sys_role表字段
-- ============================================================
ALTER TABLE sys_role ADD COLUMN description VARCHAR(255) COMMENT '角色描述';
ALTER TABLE sys_role ADD COLUMN builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置: 0-否 1-是';
ALTER TABLE sys_role ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED';
ALTER TABLE sys_role ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- ============================================================
-- 初始化权限数据
-- ============================================================

-- 知识库权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('space:view', '查看知识库', '知识库', '查看知识库列表和详情'),
  ('space:create', '创建知识库', '知识库', '创建新的知识库'),
  ('space:update', '编辑知识库', '知识库', '编辑知识库信息和配置'),
  ('space:delete', '删除知识库', '知识库', '删除知识库');

-- 文档权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('document:view', '查看文档', '文档', '查看文档列表和内容'),
  ('document:upload', '上传文档', '文档', '上传文件文档'),
  ('document:create', '创建文档', '文档', '创建在线文档'),
  ('document:update', '编辑文档', '文档', '编辑文档内容'),
  ('document:delete', '删除文档', '文档', '删除文档'),
  ('document:rebuild', '重建索引', '文档', '重新构建文档索引');

-- 问答权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('qa:view', '查看问答', '问答', '查看问答会话和消息'),
  ('qa:ask', '提问', '问答', '发送问题获取回答');

-- 成员权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('member:view', '查看成员', '成员', '查看空间成员列表'),
  ('member:add', '添加成员', '成员', '添加空间成员'),
  ('member:update', '编辑成员', '成员', '编辑成员角色'),
  ('member:remove', '移除成员', '成员', '移除空间成员');

-- 配置权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('setting:view', '查看配置', '配置', '查看知识库配置'),
  ('setting:update', '编辑配置', '配置', '修改知识库配置');

-- 系统管理权限
INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('user:view', '查看用户', '用户管理', '查看用户列表'),
  ('user:create', '创建用户', '用户管理', '创建新用户'),
  ('user:update', '编辑用户', '用户管理', '编辑用户信息'),
  ('user:disable', '禁用用户', '用户管理', '启用或禁用用户'),
  ('user:reset-password', '重置密码', '用户管理', '重置用户密码'),
  ('user:assign-role', '分配角色', '用户管理', '给用户分配角色');

INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('role:view', '查看角色', '角色管理', '查看角色列表'),
  ('role:create', '创建角色', '角色管理', '创建新角色'),
  ('role:update', '编辑角色', '角色管理', '编辑角色信息'),
  ('role:delete', '删除角色', '角色管理', '删除角色'),
  ('role:assign-permission', '分配权限', '角色管理', '给角色分配权限'),
  ('role:assign-menu', '分配菜单', '角色管理', '给角色分配菜单');

INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('menu:view', '查看菜单', '菜单管理', '查看菜单列表'),
  ('menu:create', '创建菜单', '菜单管理', '创建新菜单'),
  ('menu:update', '编辑菜单', '菜单管理', '编辑菜单信息'),
  ('menu:delete', '删除菜单', '菜单管理', '删除菜单'),
  ('menu:sort', '菜单排序', '菜单管理', '调整菜单排序');

INSERT INTO sys_permission (permission_code, permission_name, module, description) VALUES
  ('permission:view', '查看权限', '权限管理', '查看权限列表'),
  ('permission:create', '创建权限', '权限管理', '创建新权限'),
  ('permission:update', '编辑权限', '权限管理', '编辑权限信息'),
  ('permission:delete', '删除权限', '权限管理', '删除权限');

-- ============================================================
-- 初始化菜单数据
-- ============================================================

-- 一级菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort) VALUES
  (1, 0, '知识库', 'MENU', '/workspace', NULL, 'database', 1),
  (2, 0, '最近问答', 'MENU', '/recent-qa', NULL, 'message', 2),
  (3, 0, '管理', 'MENU', '/system', NULL, 'setting', 3);

-- 管理子菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, permission_code, sort) VALUES
  (4, 3, '用户管理', 'MENU', '/system/users', 'system/users/UserListPage', 'user', 'user:view', 1),
  (5, 3, '角色管理', 'MENU', '/system/roles', 'system/roles/RoleListPage', 'team', 'role:view', 2),
  (6, 3, '菜单管理', 'MENU', '/system/menus', 'system/menus/MenuListPage', 'menu', 'menu:view', 3),
  (7, 3, '权限管理', 'MENU', '/system/permissions', 'system/permissions/PermissionListPage', 'lock', 'permission:view', 4);

-- ============================================================
-- 更新内置角色标记
-- ============================================================
UPDATE sys_role SET builtin = 1, description = '系统管理员，拥有全部权限' WHERE role_code = 'SYSTEM_ADMIN';
UPDATE sys_role SET builtin = 1, description = '普通用户' WHERE role_code = 'USER';

-- ============================================================
-- 系统管理员角色授权所有权限
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN';

-- 系统管理员角色授权所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SYSTEM_ADMIN';
