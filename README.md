# ShortLink Agent

## 项目简介

ShortLink Agent 是对企业级短链 SaaS 系统的 Agent 改造版本，面向运营人员提供自然语言短链创建、分组查询、访问分析、工具链路追踪和模型调度配置能力。

## 方向

方向二：企业级应用软件的 Agent 改造。

## 技术栈

- AI IDE: Trae CN / Codex 辅助开发
- LLM: OpenAI-compatible API / 本地兼容模型预留配置
- Agent 能力: Tool Calling 风格编排、会话记忆、模型候选调度、熔断降级、工具链路追踪
- 后端: Java 17, Spring Boot 3, Spring Cloud Alibaba, MyBatis-Plus, ShardingSphere, Redis, Kafka
- 前端: Vue 3, Vite, Element Plus, ECharts
- 基础设施: Git, Maven, Node.js, MySQL, Redis, Nacos, Kafka

## 目录结构

- `admin/`: 用户、分组、管理端接口与远程调用封装。
- `project/`: 短链核心业务，包括创建、跳转、访问统计、Kafka 统计消费。
- `aggregation/`: 聚合服务与智能短链运营 Agent，包含模型调度配置、工具编排、会话记忆和 Agent API。
- `gateway/`: Spring Cloud Gateway 网关。
- `console-vue/`: 管理端前端，包含智能助手、短链管理和统计图表页面。
- `docs/`: 课程报告、架构说明和 Agent 改造文档。
- `resources/finalDemand/`: 课程大作业要求原始 PDF。
- `src/`: 课程要求的源码入口说明，映射到本 Maven 多模块工程的实际源码目录。

## 环境搭建

1. 安装 JDK 17、Maven、Node.js、MySQL、Redis、Nacos、Kafka。
2. 配置环境变量，禁止硬编码 API Key：
   - `AGENT_MODEL_BASE_URL`
   - `AGENT_MODEL_API_KEY`
   - `AGENT_CHAT_MODEL`
   - `LOCAL_MODEL_BASE_URL`
   - `LOCAL_MODEL_API_KEY`
   - `LOCAL_CHAT_MODEL`
3. 启动基础设施后编译后端：
   ```bash
   mvn -pl aggregation -am -DskipTests compile
   ```
4. 启动前端：
   ```bash
   cd console-vue
   npm install
   npm run dev
   ```
5. 访问 `http://127.0.0.1:5180/`，如端口占用可使用 Vite 临时指定其他端口。

## Agent 改造亮点

- 将原本需要表单操作的短链创建改造为自然语言指令。
- Agent 创建短链时复用标题和 favicon 元数据链路，避免列表出现无标题、无图标的低质结果。
- 引入模型候选配置、熔断状态和 settings API，支持从规则 Agent 逐步升级到真实 LLM Function Calling。
- 前端提供工具耗时、成功率、意图分布、模型调度和链路追踪可视化。
- 通过 Kafka 异步保存访问统计，降低短链跳转链路阻塞风险。

## 项目状态

- [x] Proposal
- [x] MVP
- [ ] Final

## 外部参考

- 本项目借鉴 `D:\project\java_project\ragent` 中的模型配置、settings 展示、链路追踪和调度熔断设计思想，并结合短链业务做了轻量化落地。
