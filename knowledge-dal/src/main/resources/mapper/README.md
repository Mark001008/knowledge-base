# Mapper XML 规范

- Mapper 方法名以 `insert`、`delete`、`update`、`select` 开头。
- DAL 只做单表操作，不写联表 SQL。
- 每个 mapper XML 方法必须添加注释。
- SQL 中的表名必须起别名。
- 尽量避免大量动态条件。
- 无意义 ID 在 insert 操作中直接写入 SQL，减少额外数据库交互。
