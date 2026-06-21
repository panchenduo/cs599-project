# Source Layout

本仓库是 Maven 多模块项目，课程要求中的 `src/` 目录作为源码入口说明使用。实际源码位于以下模块：

- `admin/src/main/java`: 管理端、用户、分组和远程服务封装。
- `project/src/main/java`: 短链核心业务、跳转恢复、统计持久化和 Kafka 消费。
- `aggregation/src/main/java`: Agent 聚合层、模型调度、工具编排、会话记忆和 settings API。
- `gateway/src/main/java`: 网关路由与统一入口。
- `console-vue/src`: Vue 管理台和智能助手前端。

这样保留原多模块构建能力，同时满足课程仓库结构中“src 作为源代码入口”的要求。
