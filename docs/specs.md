# Specs 规格文档

## Product Spec

### 目标用户

短链运营人员、活动运营人员和系统管理人员。

### 核心目标

将传统短链管理台升级为智能客服式业务系统，使用户能够通过自然语言完成短链创建、查询、统计分析和运营建议生成。

### 用户故事

- 作为运营人员，我希望输入“帮我给 https://www.zhihu.com 创建一个 7 天有效的短链”，系统自动创建短链并返回结果。
- 作为运营人员，我希望输入“查看当前分组短链列表”，系统返回当前分组下的短链标题、短链地址、原始 URL 和 PV 数据。
- 作为运营人员，我希望输入“分析默认分组最近 7 天访问情况”，系统自动读取统计数据并生成运营建议。
- 作为系统管理员，我希望看到模型状态、工具调用成功率、平均耗时和会话记忆，便于排查 Agent 行为。

## Architecture Spec

### 模块划分

| 模块 | 职责 |
| --- | --- |
| `console-vue` | 管理台前端、智能助手页面、统计图表和可观测面板 |
| `gateway` | 统一入口、路由和鉴权 |
| `aggregation` | Agent API、Function Calling、工具编排、记忆、模型调度 |
| `admin` | 用户、分组和管理端服务 |
| `project` | 短链创建、跳转、访问统计和消息消费 |
| MySQL / Redis / Kafka | 持久化、缓存、异步统计和会话记忆基础设施 |

### 关键设计

- Agent 层通过真实 LLM Function Calling 选择工具，不直接写数据库。
- 工具层复用原有业务服务，避免绕过权限、白名单和缓存逻辑。
- 会话记忆采用数据库 + Redis：数据库持久化长期摘要，Redis 缓存最近窗口。
- 模型调度采用候选 provider、优先级、三态熔断和 safe-fallback。
- 前端展示工具 trace、模型状态、意图分布、成功率和平均耗时。

## API Spec

| API | 方法 | 说明 |
| --- | --- | --- |
| `/api/short-link/admin/v1/agent/chat` | POST | Agent 对话、意图识别、工具调用和回复生成 |
| `/api/short-link/admin/v1/agent/settings` | GET | 查询模型 provider、候选模型、熔断状态和记忆配置 |
| `/api/short-link/v1/create` | POST | 复用原短链创建能力 |
| `/api/short-link/v1/page` | GET | 复用原短链分页查询能力 |
| `/api/short-link/v1/stats/group` | GET | 复用分组访问统计能力 |

## Tool Spec

| 工具 | 输入 | 输出 |
| --- | --- | --- |
| `listGroups` | 用户上下文 | 分组名称、gid、短链数量 |
| `createShortLink` | URL、gid、有效期、描述 | 新短链、原始链接、所属分组 |
| `pageShortLink` | gid、分页参数 | 短链列表、PV 概览 |
| `groupShortLinkStats` | gid、起止时间 | PV/UV/UIP、趋势和建议 |
| `getTitleByUrl` | 原始 URL | 网页标题与描述 |

## Evaluation Spec

- 功能正确性：自然语言创建、查询、统计分析是否完成。
- Agent 行为：工具选择是否正确，参数抽取是否完整。
- 可观测性：是否记录工具耗时、成功率、错误信息和模型状态。
- 稳定性：模型失败时是否进入熔断并切换 fallback。
- 记忆效果：多轮追问中是否能恢复上下文对象。
