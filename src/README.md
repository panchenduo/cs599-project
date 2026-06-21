# Source Layout

本仓库是 Maven 多模块项目。课程要求中的 `src/` 目录作为源代码入口说明使用，实际源代码位于以下模块中：

- `admin/src/main/java`：管理端、用户、分组和远程服务封装。
- `project/src/main/java`：短链核心业务，包括短链创建、跳转、访问统计、回收站和 Kafka 统计消费。
- `aggregation/src/main/java`：智能短链运营 Agent 聚合层，包括 Agent API、工具编排、会话记忆、模型调度和 settings API。
- `gateway/src/main/java`：网关路由、登录态校验与统一入口。
- `console-vue/src`：Vue 管理台和智能助手前端页面。

这样既保留原多模块工程结构，也满足课程仓库结构中“`src/` 作为源代码入口”的要求。
