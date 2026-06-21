# ShortLink Agent

## 项目简介

ShortLink Agent 是对企业级短链 SaaS 系统的 Agent 化改造版本。系统在保留短链创建、分组管理、访问统计、回收站、Redis 缓存、Kafka 异步统计和网关鉴权等原有能力的基础上，新增智能短链运营助手，使运营人员可以通过自然语言完成短链创建、分组查询、短链列表查询、访问分析和运营建议生成。

## 方向

方向二：企业级应用软件的 Agent 改造。

## 技术栈

- AI IDE：Trae CN / Codex 辅助开发
- LLM：OpenAI-compatible API，采用真实 LLM Function Calling
- Agent 能力：工具调用、用户问题改写、意图识别、会话记忆压缩、模型候选调度、三态熔断、safe-fallback、工具链路追踪
- 记忆存储：数据库持久化长期摘要，Redis 缓存最近窗口
- 后端：Java 17、Spring Boot 3、Spring Cloud Alibaba、MyBatis-Plus、ShardingSphere、Redis、Kafka
- 前端：Vue 3、Vite、Element Plus、ECharts
- 基础设施：Maven、Node.js、MySQL、Redis、Nacos、Kafka

## 目录结构

- `admin/`：用户、分组、管理端接口与远程调用封装。
- `project/`：短链核心业务，包括创建、跳转、访问统计、回收站、URL 标题获取和 Kafka 统计消费。
- `aggregation/`：聚合服务与智能短链运营 Agent，包括 Agent API、模型调度、工具编排、会话记忆和熔断降级。
- `gateway/`：Spring Cloud Gateway 网关与登录态校验。
- `console-vue/`：管理端前端，包括短链管理、统计图表、回收站和智能助手页面。
- `docs/`：课程报告、架构说明、系统截图和最终 PDF/Word 文档。
- `resources/database/`：数据库初始化 SQL。
- `resources/finalDemand/`：课程期末大作业要求原始 PDF。
- `src/`：课程要求的源代码入口说明，映射到本 Maven 多模块工程的实际源码目录。

## 环境搭建

1. 安装 JDK 17、Maven、Node.js、MySQL、Redis、Nacos、Kafka。
2. 初始化数据库，参考 `resources/database/link.sql`。
3. 配置运行环境。注意不要在代码中硬编码 API Key，建议通过环境变量或本地配置文件注入：
   - `AGENT_MODEL_BASE_URL`
   - `AGENT_MODEL_API_KEY`
   - `AGENT_CHAT_MODEL`
   - `LOCAL_MODEL_BASE_URL`
   - `LOCAL_MODEL_API_KEY`
   - `LOCAL_CHAT_MODEL`
4. 编译后端：

   ```bash
   mvn -pl aggregation -am -DskipTests compile
   ```

5. 启动前端：

   ```bash
   cd console-vue
   npm install
   npm run dev
   ```

6. 访问前端页面，默认地址为 `http://127.0.0.1:5180/`。如端口占用，可按 Vite 提示使用临时端口。

## 核心功能

- 自然语言创建短链：自动识别 URL、有效期、分组和描述。
- 分组与短链查询：通过智能助手查看当前分组、分组短链列表和基础访问数据。
- 访问统计分析：复用原有统计接口，生成 PV/UV/UIP 指标分析和运营建议。
- 真实 LLM Function Calling：由模型根据工具 schema 完成意图识别、工具选择和参数抽取。
- 数据库 + Redis 记忆：数据库持久化长期摘要，Redis 缓存最近窗口，实现多轮对话上下文延续。
- 模型调度与三态熔断：支持 CLOSED、OPEN、HALF_OPEN 状态转换，并通过 safe-fallback 兜底。
- 可观测面板：前端展示工具数量、成功率、平均耗时、链路追踪、模型状态、会话记忆和建议动作。

## 项目文档

- 最终报告 Word 版：`docs/CS599_大作业报告.docx`
- 最终报告 PDF 版：`docs/CS599_大作业报告.pdf`
- Specs 规格文档：`docs/specs.md`
- 架构说明：`docs/architecture.md`
- Agent 改造说明：`docs/agent-modernization.md`
- 系统截图：`docs/assets/`

## 项目状态

- [x] Proposal
- [x] MVP
- [x] Final
