from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "CS599_大作业报告.pdf"
FONT = Path("C:/Windows/Fonts/NotoSansSC-VF.ttf")
FONT_BOLD = Path("C:/Windows/Fonts/simhei.ttf")


class BookmarkDocTemplate(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and getattr(flowable, "bookmark_key", None):
            key = flowable.bookmark_key
            level = flowable.bookmark_level
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(flowable.getPlainText(), key, level=level, closed=False)


def register_fonts():
    pdfmetrics.registerFont(TTFont("NotoSC", str(FONT)))
    pdfmetrics.registerFont(TTFont("NotoSC-Bold", str(FONT_BOLD)))


def p(text, style, bookmark=None, level=0):
    para = Paragraph(text, style)
    if bookmark:
        para.bookmark_key = bookmark
        para.bookmark_level = level
    return para


def table(data, widths=None, font_size=8.5):
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1f4e79")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "NotoSC-Bold"),
        ("FONTNAME", (0, 1), (-1, -1), "NotoSC"),
        ("FONTSIZE", (0, 0), (-1, -1), font_size),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
    ]))
    return t


def page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont("NotoSC", 8)
    canvas.setFillColor(colors.HexColor("#64748b"))
    canvas.drawString(18 * mm, 12 * mm, "ShortLink Agent - CS599 大作业报告")
    canvas.drawRightString(192 * mm, 12 * mm, f"第 {doc.page} 页")
    canvas.restoreState()


def build_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="CoverTitle",
        fontName="NotoSC-Bold",
        fontSize=25,
        leading=34,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=20,
    ))
    styles.add(ParagraphStyle(
        name="CoverSub",
        fontName="NotoSC",
        fontSize=12,
        leading=19,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#334155"),
    ))
    styles.add(ParagraphStyle(
        name="H1C",
        fontName="NotoSC-Bold",
        fontSize=17,
        leading=24,
        textColor=colors.HexColor("#0f172a"),
        spaceBefore=10,
        spaceAfter=8,
    ))
    styles.add(ParagraphStyle(
        name="H2C",
        fontName="NotoSC-Bold",
        fontSize=13,
        leading=19,
        textColor=colors.HexColor("#1e3a8a"),
        spaceBefore=8,
        spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        name="BodyC",
        fontName="NotoSC",
        fontSize=9.5,
        leading=15,
        alignment=TA_LEFT,
        textColor=colors.HexColor("#1f2937"),
        spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        name="SmallC",
        fontName="NotoSC",
        fontSize=8,
        leading=12,
        textColor=colors.HexColor("#475569"),
    ))
    return styles


def section_title(text, styles, key, level=0):
    return p(text, styles["H1C"] if level == 0 else styles["H2C"], key, level)


def make_story(styles):
    body = styles["BodyC"]
    small = styles["SmallC"]
    story = []

    story += [
        Spacer(1, 35 * mm),
        p("企业级应用软件设计与开发<br/>期末大作业报告", styles["CoverTitle"]),
        p("项目名称：ShortLink Agent - 企业级短链系统智能化改造", styles["CoverSub"]),
        Spacer(1, 12 * mm),
        table([
            ["字段", "内容"],
            ["课程名称", "企业级应用软件设计与开发"],
            ["方向", "方向二：企业级应用软件的 Agent 改造"],
            ["学号", "待补充"],
            ["姓名", "待补充"],
            ["专业", "计算机技术 / 软件工程"],
            ["指导教师", "戚欣"],
            ["提交日期", "2026 年 6 月 22 日"],
        ], widths=[42 * mm, 100 * mm], font_size=10),
        Spacer(1, 16 * mm),
        p("摘要：本项目选取一个已有的企业级短链 SaaS 系统作为原始系统，围绕短链创建、分组运营、访问统计分析和系统可观测性进行 Agent 改造。改造后系统支持自然语言创建短链、自动补齐标题和图标、分组统计分析、工具链路追踪、模型候选配置和熔断降级，目标是解决真实运营场景中的效率、分析和可维护性问题。", body),
        PageBreak(),
    ]

    story += [
        section_title("目录", styles, "toc"),
        p("1. 选题背景与设计思想", body),
        p("2. Specs 规格文档", body),
        p("3. 系统架构与设计", body),
        p("4. 关键实现与代码展示", body),
        p("5. 测试与评估", body),
        p("6. 系统升级与扩展", body),
        p("7. 课程总结", body),
        PageBreak(),
    ]

    story += [
        section_title("一、选题背景与设计思想", styles, "sec1"),
        p("本项目选择方向二：企业级应用软件的 Agent 改造。原始系统是一个企业级短链 SaaS 平台，已具备用户、分组、短链创建、批量创建、回收站、访问统计、Redis 缓存、网关和消息队列等能力。系统具备真实业务基础，但在运营使用体验、自动分析和智能编排方面存在明显提升空间。", body),
        p("原始系统痛点", styles["H2C"]),
        table([
            ["痛点", "表现", "Agent 改造目标"],
            ["创建门槛高", "运营人员需要理解分组、有效期、描述等表单字段", "自然语言解析 URL、有效期、分组并自动创建"],
            ["数据分析弱", "统计页只有 PV/UV/UIP 等指标，缺少业务解释", "自动生成分组访问表现、风险和运营建议"],
            ["流程割裂", "创建、查询、统计分散在不同页面", "通过 Agent 编排多个业务 API"],
            ["智能层像 demo", "早期助手主要依赖规则和固定回复", "增加模型配置、候选调度、熔断、追踪和可视化"],
        ], widths=[30 * mm, 66 * mm, 70 * mm]),
        p("改造前 vs 改造后", styles["H2C"]),
        table([
            ["维度", "改造前", "改造后"],
            ["短链创建", "表单输入，标题由前端单独查询", "Agent 创建也复用标题与 favicon 元数据链路"],
            ["运营分析", "人工查看图表后自行判断", "Agent 自动调用统计工具并生成可读结论"],
            ["模型调度", "无模型配置或候选管理", "支持 provider、候选模型、优先级、熔断参数和 settings API"],
            ["可观测性", "只能看最终回答", "展示工具耗时、成功率、意图分布和模型健康状态"],
        ], widths=[28 * mm, 68 * mm, 70 * mm]),
    ]

    story += [
        section_title("二、Specs 规格文档", styles, "sec2"),
        p("Product Spec：系统面向短链运营人员，提供自然语言操作入口。用户可以说“帮我给 https://www.zhihu.com 创建一个 7 天有效的短链”，系统自动完成 URL 识别、分组选择、有效期解析、标题抓取、短链创建和结果展示。", body),
        p("Architecture Spec：新增 aggregation 层 Agent 模块，位于业务服务与前端之间。Agent 不直接操作数据库，而是通过工具调用复用 GroupService、ShortLinkService、ShortLinkStatsService 和 UrlTitleService。模型配置来自 short-link.agent.model，支持 OpenAI-compatible 和本地兼容模型预留。", body),
        p("API Spec", styles["H2C"]),
        table([
            ["接口", "方法", "用途"],
            ["/api/short-link/admin/v1/agent/chat", "POST", "Agent 对话、意图识别和工具编排"],
            ["/api/short-link/admin/v1/agent/settings", "GET", "返回模型 provider、候选模型、熔断和记忆配置"],
            ["/api/short-link/v1/create", "POST", "复用原短链创建工具"],
            ["/api/short-link/v1/stats/group", "GET", "复用分组统计工具"],
        ], widths=[68 * mm, 22 * mm, 76 * mm]),
    ]

    story += [
        section_title("三、系统架构与设计", styles, "sec3"),
        p("核心链路：用户自然语言 -> console-vue 智能助手 -> ShortLinkAgentController -> ShortLinkAgentService -> AgentRoutingExecutor -> 业务工具 -> 原短链系统服务 -> 数据库/缓存/消息队列 -> 工具追踪与自然语言回答。", body),
        table([
            ["层次", "组件", "职责"],
            ["前端", "AgentIndex.vue", "聊天、建议动作、模型调度、链路追踪和图表展示"],
            ["聚合层", "ShortLinkAgentServiceImpl", "意图识别、参数解析、工具编排、记忆追加"],
            ["调度层", "AgentRoutingExecutor", "候选模型选择、失败切换、熔断状态展示"],
            ["配置层", "AgentModelProperties", "provider、模型候选、优先级、stream 和 selection 参数"],
            ["业务层", "ShortLinkService / StatsService", "短链创建、分页查询、分组统计和跳转统计"],
        ], widths=[24 * mm, 50 * mm, 92 * mm]),
        p("数据流设计强调不绕过原系统校验：Agent 创建短链仍走白名单、分组、有效期、缓存预热等既有逻辑；访问统计走 Kafka 异步保存，避免跳转链路阻塞。", body),
    ]

    story += [
        section_title("四、关键实现与代码展示", styles, "sec4"),
        p("1. 元数据一致性修复：Agent 创建短链时新增 resolveDescribe，优先使用用户显式描述，其次调用 UrlTitleService 获取网页标题，最后才使用本地 fallback。ShortLinkService 继续负责 favicon 抓取，并已加入超时与失败降级，避免外部站点慢响应导致创建失败。", body),
        p("2. 模型配置迁移：借鉴 ragent 的 AIModelProperties 和 settings 控制器模式，新增 AgentModelProperties 与 AgentSettingsRespDTO。配置支持 openai-compatible、本地兼容模型、候选模型优先级、deepThinkingModel、熔断阈值和 stream chunk size。", body),
        p("3. 可观测前端：Agent 页面新增工具成功率、平均耗时、会话轮次、调用耗时折线、工具状态饼图、意图分布和模型候选健康状态。", body),
        p("4. 工具定义：listGroups、getTitleByUrl、createShortLink、batchCreateShortLink、pageShortLink、groupShortLinkStats 均记录 request、response 摘要、success、durationMs 和 message。", body),
    ]

    story += [
        section_title("五、测试与评估", styles, "sec5"),
        table([
            ["测试项", "方法", "结果"],
            ["后端编译", "mvn -pl aggregation -am -DskipTests compile", "通过"],
            ["前端构建", "console-vue 下执行 npm run build", "通过"],
            ["Agent 创建短链", "自然语言创建知乎 7 天短链", "已修复标题和 favicon 元数据链路"],
            ["分组统计分析", "分析分组最近 7 天访问情况", "空聚合返回 0，不再 NPE"],
            ["模型配置展示", "GET /agent/settings", "可返回候选模型和脱敏 provider"],
        ], widths=[32 * mm, 72 * mm, 62 * mm]),
        p("行为评估指标包括：工具调用成功率、平均工具耗时、意图识别是否正确、短链元数据是否完整、统计为空时是否稳定返回、模型候选是否能展示熔断健康状态。", body),
    ]

    story += [
        section_title("六、系统升级与扩展", styles, "sec6"),
        p("下一阶段计划：1）接入真实 OpenAI-compatible Function Calling，将当前规则解析替换为模型工具调用；2）引入持久化 Agent Trace 表，沉淀 LLMOps 数据；3）引入定时运营报告和异常预警；4）准备 Ollama 本地模型作为 Demo Day API 失败保底；5）补充 Docker Compose 一键启动 MySQL、Redis、Nacos、Kafka 和服务。", body),
        p("可扩展架构上，现有模型配置已为 provider 多样化、候选模型优先级、深度思考模型、本地模型兜底留下接口；现有 Tool Trace 可以自然扩展为评估数据集。", body),
    ]

    story += [
        section_title("七、课程总结", styles, "sec7"),
        p("本项目的核心收获是：Agent 改造不是简单增加一个聊天框，而是要从原系统痛点出发，把已有业务 API、模型调度、工具可观测、错误降级和用户体验组织成完整工程闭环。相比传统 CRUD 开发，Agentic AI 更强调规格设计、工具边界、状态管理和评估闭环。", body),
        p("后续若补充真实 LLM Function Calling、持久化 Trace、Docker 部署和完整演示视频，本项目可以从课程 MVP 进一步提升为更接近生产级的智能短链运营平台。", body),
        Spacer(1, 10 * mm),
        p("注：封面中的学号、姓名为初版占位，提交前需替换为真实信息。", small),
    ]
    return story


def main():
    register_fonts()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    frame = Frame(18 * mm, 18 * mm, 174 * mm, 260 * mm, id="normal")
    doc = BookmarkDocTemplate(
        str(OUT),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=18 * mm,
    )
    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=page_number)])
    styles = build_styles()
    doc.build(make_story(styles))
    print(OUT)


if __name__ == "__main__":
    main()
