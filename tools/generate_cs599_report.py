import os
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
    Flowable,
    Frame,
    Image,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "CS599_大作业报告.pdf"
TMP_OUT = ROOT / "docs" / "CS599_大作业报告.new.pdf"
ASSETS = ROOT / "docs" / "assets"
FONT = Path("C:/Windows/Fonts/NotoSansSC-VF.ttf")
FONT_BOLD = Path("C:/Windows/Fonts/simhei.ttf")


class BookmarkDocTemplate(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and getattr(flowable, "bookmark_key", None):
            self.canv.bookmarkPage(flowable.bookmark_key)
            self.canv.addOutlineEntry(
                flowable.getPlainText(),
                flowable.bookmark_key,
                level=flowable.bookmark_level,
                closed=False,
            )


def register_fonts():
    pdfmetrics.registerFont(TTFont("NotoSC", str(FONT)))
    pdfmetrics.registerFont(TTFont("NotoSC-Bold", str(FONT_BOLD)))


def build_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="CoverTitle",
        fontName="NotoSC-Bold",
        fontSize=25,
        leading=34,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=18,
    ))
    styles.add(ParagraphStyle(
        name="CoverSub",
        fontName="NotoSC",
        fontSize=11.5,
        leading=18,
        alignment=TA_CENTER,
        textColor=colors.HexColor("#334155"),
    ))
    styles.add(ParagraphStyle(
        name="H1C",
        fontName="NotoSC-Bold",
        fontSize=16.5,
        leading=23,
        textColor=colors.HexColor("#0f172a"),
        spaceBefore=9,
        spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        name="H2C",
        fontName="NotoSC-Bold",
        fontSize=12.5,
        leading=18,
        textColor=colors.HexColor("#1d4ed8"),
        spaceBefore=7,
        spaceAfter=4,
    ))
    styles.add(ParagraphStyle(
        name="BodyC",
        fontName="NotoSC",
        fontSize=9.2,
        leading=14.3,
        alignment=TA_LEFT,
        textColor=colors.HexColor("#1f2937"),
        spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        name="SmallC",
        fontName="NotoSC",
        fontSize=7.7,
        leading=11.5,
        textColor=colors.HexColor("#475569"),
    ))
    styles.add(ParagraphStyle(
        name="TableC",
        fontName="NotoSC",
        fontSize=7.8,
        leading=11.4,
        textColor=colors.HexColor("#1f2937"),
    ))
    styles.add(ParagraphStyle(
        name="TableHeadC",
        fontName="NotoSC-Bold",
        fontSize=8,
        leading=11,
        textColor=colors.white,
        alignment=TA_CENTER,
    ))
    styles.add(ParagraphStyle(
        name="CodeC",
        fontName="Courier",
        fontSize=7.4,
        leading=10.2,
        textColor=colors.HexColor("#0f172a"),
        backColor=colors.HexColor("#f8fafc"),
        borderColor=colors.HexColor("#dbeafe"),
        borderWidth=0.4,
        borderPadding=5,
        spaceAfter=6,
    ))
    return styles


def para(text, style, bookmark=None, level=0):
    p = Paragraph(text, style)
    if bookmark:
        p.bookmark_key = bookmark
        p.bookmark_level = level
    return p


def cell(text, styles, bold=False):
    return Paragraph(str(text), styles["TableHeadC" if bold else "TableC"])


def table(data, styles, widths=None, font_size=7.8, header=True):
    rows = []
    for r, row in enumerate(data):
        rows.append([cell(x, styles, bold=(header and r == 0)) for x in row])
    t = Table(rows, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [
        ("FONTNAME", (0, 0), (-1, -1), "NotoSC"),
        ("FONTSIZE", (0, 0), (-1, -1), font_size),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
    ]
    if header:
        commands.extend([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1f4e79")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "NotoSC-Bold"),
        ])
    t.setStyle(TableStyle(commands))
    return t


def bullet_list(items, styles):
    story = []
    for item in items:
        story.append(para("• " + item, styles["BodyC"]))
    return story


def section_title(text, styles, key, level=0):
    return para(text, styles["H1C"] if level == 0 else styles["H2C"], key, level)


class ArchitectureDiagram(Flowable):
    def __init__(self, width=174 * mm, height=126 * mm):
        super().__init__()
        self.width = width
        self.height = height

    def wrap(self, avail_width, avail_height):
        return self.width, self.height

    def _box(self, c, x, y, w, h, title, lines, fill, stroke="#94a3b8"):
        c.setStrokeColor(colors.HexColor(stroke))
        c.setFillColor(colors.HexColor(fill))
        c.roundRect(x, y, w, h, 5, stroke=1, fill=1)
        c.setFillColor(colors.HexColor("#0f172a"))
        c.setFont("NotoSC-Bold", 8.3)
        c.drawString(x + 4, y + h - 12, title)
        c.setFont("NotoSC", 6.7)
        c.setFillColor(colors.HexColor("#334155"))
        for i, line in enumerate(lines):
            c.drawString(x + 4, y + h - 23 - i * 9, line)

    def _arrow(self, c, x1, y1, x2, y2, label=None):
        c.setStrokeColor(colors.HexColor("#64748b"))
        c.setLineWidth(0.85)
        c.line(x1, y1, x2, y2)
        dx = 1 if x2 >= x1 else -1
        c.line(x2, y2, x2 - 5 * dx, y2 + 3)
        c.line(x2, y2, x2 - 5 * dx, y2 - 3)
        if label:
            c.setFont("NotoSC", 6.2)
            c.setFillColor(colors.HexColor("#475569"))
            c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + 4, label)

    def draw(self):
        c = self.canv
        c.saveState()
        c.setFillColor(colors.HexColor("#f8fafc"))
        c.roundRect(0, 0, self.width, self.height, 8, stroke=0, fill=1)
        c.setFont("NotoSC-Bold", 10)
        c.setFillColor(colors.HexColor("#0f172a"))
        c.drawString(8, self.height - 14, "核心架构图：原始短链系统 + 智能客服/运营 Agent")

        y_top = self.height - 52
        bw, bh = 44 * mm, 25 * mm
        xs = [8, 8 + 47 * mm, 8 + 94 * mm, 8 + 141 * mm]
        self._box(c, xs[0], y_top, bw, bh, "用户入口", ["短链运营人员", "自然语言指令", "短链管理页面"], "#e0f2fe", "#38bdf8")
        self._box(c, xs[1], y_top, bw, bh, "console-vue", ["智能助手面板", "会话记忆/建议动作", "工具与模型可观测"], "#dbeafe", "#60a5fa")
        self._box(c, xs[2], y_top, bw, bh, "网关与聚合层", ["Gateway 鉴权路由", "Aggregation Agent API", "统一封装业务工具"], "#ede9fe", "#8b5cf6")
        self._box(c, xs[3], y_top, bw, bh, "智能调度核心", ["Query Rewrite / Intent", "Memory Compression", "Model Router + Breaker"], "#fae8ff", "#d946ef")

        y_mid = self.height - 91
        self._box(c, xs[0], y_mid, bw, bh, "原始短链前台能力", ["分组/创建/批量创建", "短链列表/回收站", "访问统计图表"], "#ecfdf5", "#10b981")
        self._box(c, xs[1], y_mid, bw, bh, "业务工具层", ["listGroups", "createShortLink", "pageShortLink / stats"], "#f0fdf4", "#22c55e")
        self._box(c, xs[2], y_mid, bw, bh, "短链业务服务", ["Admin Service", "Project Service", "UrlTitle/Favicon"], "#fff7ed", "#fb923c")
        self._box(c, xs[3], y_mid, bw, bh, "模型与降级", ["OpenAI-compatible", "local-compatible", "safe-fallback"], "#fef2f2", "#f87171")

        y_low = 13
        self._box(c, xs[0], y_low, bw, 22 * mm, "数据基础设施", ["MySQL / ShardingSphere", "Redis 缓存", "Nacos 配置中心"], "#f1f5f9")
        self._box(c, xs[1], y_low, bw, 22 * mm, "异步统计链路", ["Kafka / Redis Stream", "访问日志聚合", "PV/UV/UIP 指标"], "#f1f5f9")
        self._box(c, xs[2], y_low, bw, 22 * mm, "Agent 运行态", ["Tool Trace", "会话窗口 + 摘要", "成功率/耗时/意图分布"], "#f1f5f9")
        self._box(c, xs[3], y_low, bw, 22 * mm, "外部资源", ["目标网站标题抓取", "大模型 API", "本地兼容模型预留"], "#f1f5f9")

        for i in range(3):
            self._arrow(c, xs[i] + bw, y_top + bh / 2, xs[i + 1], y_top + bh / 2)
            self._arrow(c, xs[i] + bw, y_mid + bh / 2, xs[i + 1], y_mid + bh / 2)

        self._arrow(c, xs[3] + bw / 2, y_top, xs[3] + bw / 2, y_mid + bh, "候选策略")
        self._arrow(c, xs[1] + bw / 2, y_top, xs[1] + bw / 2, y_mid + bh, "普通页面复用")
        self._arrow(c, xs[1] + bw / 2, y_mid, xs[1] + bw / 2, y_low + 22 * mm, "trace")
        self._arrow(c, xs[2] + bw / 2, y_mid, xs[2] + bw / 2, y_low + 22 * mm, "持久化/缓存")
        self._arrow(c, xs[3] + bw / 2, y_mid, xs[3] + bw / 2, y_low + 22 * mm, "API 调用")
        self._arrow(c, xs[2] + bw, y_low + 11 * mm, xs[3], y_low + 11 * mm, "模型请求")
        self._arrow(c, xs[2], y_low + 11 * mm, xs[1] + bw, y_low + 11 * mm, "统计事件")
        c.restoreState()


class BreakerDiagram(Flowable):
    def __init__(self, width=174 * mm, height=53 * mm):
        super().__init__()
        self.width = width
        self.height = height

    def wrap(self, avail_width, avail_height):
        return self.width, self.height

    def _node(self, c, x, y, label, sub, fill, stroke):
        c.setFillColor(colors.HexColor(fill))
        c.setStrokeColor(colors.HexColor(stroke))
        c.roundRect(x, y, 42 * mm, 18 * mm, 5, stroke=1, fill=1)
        c.setFillColor(colors.HexColor("#0f172a"))
        c.setFont("NotoSC-Bold", 9)
        c.drawCentredString(x + 21 * mm, y + 11 * mm, label)
        c.setFont("NotoSC", 6.5)
        c.setFillColor(colors.HexColor("#475569"))
        c.drawCentredString(x + 21 * mm, y + 5 * mm, sub)

    def _arrow(self, c, x1, y1, x2, y2, label):
        c.setStrokeColor(colors.HexColor("#64748b"))
        c.line(x1, y1, x2, y2)
        c.line(x2, y2, x2 - 5, y2 + 3)
        c.line(x2, y2, x2 - 5, y2 - 3)
        c.setFont("NotoSC", 6.4)
        c.setFillColor(colors.HexColor("#334155"))
        c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + 6, label)

    def draw(self):
        c = self.canv
        c.saveState()
        c.setFillColor(colors.HexColor("#f8fafc"))
        c.roundRect(0, 0, self.width, self.height, 7, stroke=0, fill=1)
        x1, x2, x3 = 9 * mm, 66 * mm, 123 * mm
        y = 21 * mm
        self._node(c, x1, y, "CLOSED", "正常放行并统计失败", "#dcfce7", "#22c55e")
        self._node(c, x2, y, "OPEN", "拒绝调用，等待冷却", "#fee2e2", "#ef4444")
        self._node(c, x3, y, "HALF_OPEN", "只允许一个探测请求", "#fef3c7", "#f59e0b")
        self._arrow(c, x1 + 42 * mm, y + 9 * mm, x2, y + 9 * mm, "连续失败 >= 阈值")
        self._arrow(c, x2 + 42 * mm, y + 9 * mm, x3, y + 9 * mm, "冷却时间到")
        self._arrow(c, x3, y + 2 * mm, x1 + 42 * mm, y + 2 * mm, "探测成功")
        self._arrow(c, x3, y + 16 * mm, x2 + 42 * mm, y + 16 * mm, "探测失败")
        c.setFont("NotoSC", 7)
        c.setFillColor(colors.HexColor("#475569"))
        c.drawString(7 * mm, 8 * mm, "默认策略：主模型失败后进入 OPEN，safe-fallback 兜底；冷却后 HALF_OPEN 探测，避免单点模型故障拖垮智能助手。")
        c.restoreState()


def page_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("NotoSC", 8)
    canvas.setFillColor(colors.HexColor("#64748b"))
    canvas.drawString(18 * mm, 12 * mm, "ShortLink Agent - CS599 大作业报告")
    canvas.drawRightString(192 * mm, 12 * mm, f"第 {doc.page} 页")
    canvas.restoreState()


def add_image(path, caption, styles, max_width=166 * mm, max_height=86 * mm):
    img = Image(str(path))
    scale = min(max_width / img.imageWidth, max_height / img.imageHeight)
    img.drawWidth = img.imageWidth * scale
    img.drawHeight = img.imageHeight * scale
    return [img, para(caption, styles["SmallC"]), Spacer(1, 4 * mm)]


def code_block(text, styles):
    escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>")
    return para(escaped, styles["CodeC"])


def make_story(styles):
    body = styles["BodyC"]
    story = []

    story += [
        Spacer(1, 26 * mm),
        para("企业级应用软件设计与开发<br/>期末大作业报告", styles["CoverTitle"]),
        para("ShortLink Agent：面向短链运营的智能客服与智能编排系统", styles["CoverSub"]),
        Spacer(1, 11 * mm),
        table([
            ["字段", "内容"],
            ["课程名称", "企业级应用软件设计与开发"],
            ["选题方向", "方向二：企业级应用软件的 Agent 改造"],
            ["项目基础", "企业级短链 SaaS 系统"],
            ["改造主题", "智能短链运营助手：自然语言创建、查询、分析与可观测调度"],
            ["姓名 / 学号", "请提交前替换为真实信息"],
            ["提交日期", "2026 年 6 月"],
        ], styles, widths=[42 * mm, 104 * mm], font_size=9.5),
        Spacer(1, 12 * mm),
        para("摘要", styles["H2C"]),
        para("本项目在已有企业级短链系统基础上进行 Agent 化改造。原系统已经具备用户、分组、短链创建、批量创建、回收站、访问统计、Redis 缓存、Kafka 异步统计、网关鉴权等企业应用能力；本次改造的重点不是另起一个聊天 Demo，而是在原有业务闭环上增加智能客服/运营 Agent，使运营人员能够通过自然语言完成短链创建、分组查询、短链列表查看、访问统计分析和运营建议生成。报告重点说明三类工程机制：会话记忆压缩、模型候选降级、用户问题改写与意图拆分，并通过系统截图与核心架构图展示改造效果。", body),
        para("关键词：短链系统；智能客服；Agent；Tool Calling；记忆压缩；三态熔断器；Query Rewrite；可观测性", styles["SmallC"]),
        PageBreak(),
    ]

    story += [
        section_title("目录", styles, "toc"),
        table([
            ["章节", "内容重点"],
            ["一、选题背景与改造目标", "说明为什么原短链系统适合做 Agent 改造"],
            ["二、原始短链系统能力", "梳理原系统的业务边界、服务划分和数据链路"],
            ["三、智能客服短链系统总体架构", "给出融合原系统与 Agent 的核心架构图"],
            ["四、智能助手业务闭环", "说明自然语言创建、查询、统计分析的运行方式"],
            ["五、记忆压缩设计", "解释多轮对话不失忆与 Token 预算控制"],
            ["六、模型降级与三态熔断", "解释候选模型、失败转移与 safe-fallback"],
            ["七、用户问题改写与意图拆分", "解释用户原话如何变成可执行工具参数"],
            ["八、关键实现与截图展示", "结合系统运行图说明落地效果"],
            ["九、测试、总结与展望", "整理验证结果、课程收获与后续计划"],
        ], styles, widths=[48 * mm, 118 * mm]),
        PageBreak(),
    ]

    story += [
        section_title("一、选题背景与改造目标", styles, "sec1"),
        para("原始项目是一个企业级短链 SaaS 平台，适合从传统 CRUD 系统升级为智能客服式业务系统。短链运营场景天然包含“创建、查询、分组、统计、分析、建议”多个连续动作：用户并不总是想逐个点击页面，而是希望直接表达运营目标，例如“帮我给知乎创建一个 7 天有效的短链”“查看当前分组短链列表”“分析默认分组最近 7 天访问情况”。这些任务本质上需要 Agent 对现有 API 进行理解、选择与编排。", body),
        para("本次大作业的目标是：保留原短链系统稳定的业务能力，在其上方增加智能客服与智能运营层，让自然语言成为新的业务入口，同时补齐生产级 Agent 需要的记忆、降级和可观测能力。", body),
        table([
            ["原始痛点", "具体表现", "改造后的解决方案"],
            ["操作入口割裂", "创建短链、查看分组、统计分析分布在不同页面", "智能助手将多步操作合并为自然语言指令"],
            ["业务参数门槛高", "用户要理解分组、有效期、描述、URL 白名单等字段", "Agent 从自然语言中抽取 URL、有效期、分组和描述"],
            ["统计数据难转化", "PV/UV/UIP 图表需要人工解释", "Agent 调用统计工具并生成运营建议"],
            ["多轮对话易丢上下文", "“它”“这个分组”等指代需要历史对话", "会话窗口 + 摘要压缩，保留最近原文和长期摘要"],
            ["模型调用不稳定", "大模型 API 可能超时、失败或不可用", "候选策略、三态熔断和 safe-fallback 兜底"],
        ], styles, widths=[32 * mm, 64 * mm, 70 * mm]),
    ]

    story += [
        section_title("二、原始短链系统能力", styles, "sec2"),
        para("原系统不是空壳 Demo，而是已经具备较完整的企业应用结构。后端按 admin、project、gateway、aggregation 等模块组织：admin 负责用户、分组与管理接口；project 负责短链创建、跳转、访问统计、回收站和 URL 标题获取；gateway 负责统一路由与鉴权；aggregation 在本次改造中成为智能 Agent 的聚合入口。", body),
        table([
            ["能力域", "主要模块/类", "作用"],
            ["用户与分组", "UserController、GroupController、GroupService", "管理用户登录态、分组创建、排序、查询"],
            ["短链创建", "ShortLinkController、ShortLinkService", "生成短链、校验白名单、保存映射、预热缓存"],
            ["短链跳转", "ShortLinkController / goto", "根据短码查询原始 URL，并记录访问行为"],
            ["访问统计", "ShortLinkStatsService、LinkAccessStatsDO", "统计 PV、UV、UIP、浏览器、设备、地域等指标"],
            ["异步链路", "KafkaShortLinkStatsSaveConsumer、RedisStreamConfiguration", "将跳转链路与统计落库解耦"],
            ["前端管理台", "console-vue", "分组列表、短链列表、创建弹窗、统计图表、回收站"],
        ], styles, widths=[30 * mm, 60 * mm, 76 * mm]),
        para("Agent 改造遵循一个重要原则：智能层不直接绕过原业务服务，不直接写数据库，而是以工具调用方式复用原系统的服务边界。这样可以保留原本的权限校验、白名单校验、缓存预热、统计处理和异常处理。", body),
    ]

    story += [
        section_title("三、智能客服短链系统总体架构", styles, "sec3"),
        para("下图展示当前系统的核心架构。它既覆盖原始短链系统，也突出新增的智能客服/运营 Agent：前端用户通过 console-vue 的智能助手输入自然语言；请求进入网关和 aggregation 聚合层；Agent 先加载记忆、改写问题、识别意图，再通过工具层调用原有短链服务；模型调度层负责候选模型选择、熔断降级和兜底响应；工具调用轨迹与模型状态回传给前端，形成可观测面板。", body),
        ArchitectureDiagram(),
        Spacer(1, 4 * mm),
        table([
            ["架构层", "新增或复用", "说明"],
            ["智能助手 UI", "新增", "聊天区、建议动作、工具状态、意图分布、模型调度、会话记忆"],
            ["Agent Controller", "新增", "提供 /agent/chat 与 /agent/settings 接口"],
            ["Agent Service", "新增", "负责任务编排、意图识别、参数解析、工具调用与回复生成"],
            ["Memory Service", "新增", "维护最近窗口、摘要、会话轮次，避免长对话爆 Token"],
            ["Routing Executor", "新增", "按候选模型与熔断状态选择策略，失败后切换 fallback"],
            ["业务工具", "复用 + 封装", "将 listGroups、createShortLink、pageShortLink、stats 等业务 API 封装为工具"],
            ["原短链服务", "复用", "负责真实创建、查询、统计、跳转、缓存、消息队列和数据落库"],
        ], styles, widths=[30 * mm, 28 * mm, 108 * mm]),
    ]

    story += [
        section_title("四、智能助手业务闭环", styles, "sec4"),
        para("智能客服的核心不是“回答闲聊”，而是把用户目标转成可执行的短链运营动作。当前系统围绕四个高频场景形成闭环。", body),
        table([
            ["用户自然语言", "识别意图", "调用工具", "返回结果"],
            ["查看当前分组列表", "LIST_GROUPS", "GroupService.groupList", "返回分组名称、短链数量，并给出后续建议"],
            ["查看当前分组短链列表", "LIST_SHORT_LINKS", "ShortLinkService.pageShortLink", "返回短链标题、短链地址、原始 URL 和 PV"],
            ["帮我给 https://www.zhihu.com 创建一个 7 天有效的短链", "CREATE_SHORT_LINK", "UrlTitleService + ShortLinkService.createShortLink", "返回新短链、原始链接、所属分组和有效期"],
            ["分析默认分组最近 7 天访问情况", "ANALYZE_GROUP_STATS", "ShortLinkStatsService.groupShortLinkStats", "汇总 PV/UV/UIP，并生成运营建议"],
        ], styles, widths=[52 * mm, 32 * mm, 45 * mm, 37 * mm]),
        para("这个闭环体现了 Agent 改造的关键价值：用户不需要知道系统有哪些 API，也不需要关心应该先查分组还是先填表单；系统通过意图识别、工具选择和结果总结完成端到端操作。", body),
    ]

    story += [
        section_title("五、记忆压缩设计：长对话不失忆", styles, "sec5"),
        para("大模型 API 本身没有跨请求记忆。要让“这个分组”“它的访问情况”这类表达可被理解，系统必须在每次请求前主动组装上下文。但上下文不能无限增长，否则 Token 预算、响应耗时和费用都会失控。因此本项目吸收 Ragent 笔记中的设计，将记忆拆成“最近原文窗口 + 早期摘要”两部分。", body),
        table([
            ["设计点", "报告中的落地解释"],
            ["门面服务", "AgentMemoryService 对外只暴露加载、追加、快照等能力，业务编排层不关心底层存储细节"],
            ["最近窗口", "保留最近若干轮用户与助手原文，保证指代、修正和追问能被准确理解"],
            ["摘要压缩", "更早消息压缩成一段长期摘要，作为系统上下文插入，不把所有历史原文都塞给模型"],
            ["水位线思想", "摘要记录已经覆盖到哪一轮，下一次只压缩新增历史，避免重复摘要造成语义漂移"],
            ["异步压缩", "完整一轮对话结束后再触发压缩，不阻塞当前用户请求"],
            ["降级容错", "摘要加载失败时至少返回最近窗口；历史加载失败时可返回空上下文，保证主流程可用"],
        ], styles, widths=[36 * mm, 130 * mm]),
        code_block("memory = loadSummary(conversationId) + loadRecentMessages(conversationId)\nmessages = [systemPrompt, memorySummary, recentTurns, currentUserQuestion]\nappend(userQuestion)\nanswer = runAgent(messages)\nappend(assistantAnswer)\ncompressIfNeededAsync(conversationId)", styles),
        para("在短链系统里，记忆的内容不只是聊天文本，还包括最近查看的分组、最近创建的短链、用户常用有效期、当前会话意图和工具结果摘要。这样用户追问“把刚才那个也放到分组二”“分析这个分组最近 7 天”时，Agent 可以从上下文中恢复业务对象。", body),
    ]

    story += [
        section_title("六、模型降级与三态熔断", styles, "sec6"),
        para("智能客服系统依赖模型，但模型 API 可能因为网络、额度、服务端异常或本地模型不可用而失败。为了避免助手页面卡死或反复请求同一个坏模型，本项目引入候选模型与三态熔断器。前端截图中可以看到默认模型 rule-planner-v2、local-compatible-v1 和 safe-fallback 等候选项，以及 CLOSED/disabled 等状态。", body),
        BreakerDiagram(),
        Spacer(1, 4 * mm),
        table([
            ["状态", "含义", "调用策略"],
            ["CLOSED", "模型健康，正常放行请求", "成功则清零失败计数，失败则累计连续失败次数"],
            ["OPEN", "模型已熔断，暂时拒绝调用", "直接跳过该候选，选择下一个候选或 safe-fallback"],
            ["HALF_OPEN", "冷却后进入探测", "同一时间只允许一个探测请求；成功恢复 CLOSED，失败回到 OPEN"],
        ], styles, widths=[28 * mm, 58 * mm, 80 * mm]),
        para("这套机制使系统具备生产级韧性：当主模型不可用时，Agent 仍能通过规则规划器或安全兜底返回可解释结果；当冷却时间结束后，再用 HALF_OPEN 探测恢复，避免永久封禁临时失败的模型。", body),
    ]

    story += [
        section_title("七、用户问题改写与意图拆分", styles, "sec7"),
        para("用户说的话不等于系统该执行的工具参数。Ragent 笔记中强调：记忆让模型知道“它”是什么，但检索或工具调用仍需要明确、独立、结构化的问题。因此本项目在 Agent 编排中加入轻量 Query Rewrite 思路：先做术语归一化和上下文补全，再识别意图，必要时拆分复合问题。", body),
        table([
            ["阶段", "作用", "短链系统示例"],
            ["术语归一化", "把口语化表达映射到系统字段或业务对象", "“七天有效” -> validDateType + validDate；“当前分组” -> gid"],
            ["指代消解", "结合记忆补全省略对象", "“它的访问情况” -> 上一轮创建的知乎短链访问情况"],
            ["问题改写", "把自然语言变成完整可执行描述", "“给知乎来一个” -> “为 https://www.zhihu.com 创建短链”"],
            ["意图识别", "选择工具类型", "CREATE_SHORT_LINK / LIST_GROUPS / LIST_SHORT_LINKS / ANALYZE_GROUP_STATS"],
            ["复合拆分", "一句话包含多个独立任务时拆成子任务", "“创建知乎短链并查看分组二列表” -> create + page 两个工具"],
        ], styles, widths=[30 * mm, 54 * mm, 82 * mm]),
        para("在当前课程项目中，为了可控演示，意图识别以规则规划器为主，并预留 OpenAI-compatible Function Calling 和本地模型规划器。这样的设计既能保证课堂演示稳定，也给后续替换真实 LLM 工具调用留下接口。", body),
    ]

    story += [
        section_title("八、关键实现与运行截图", styles, "sec8"),
        para("以下截图来自系统实际运行，展示了原始短链列表、智能助手对话区、工具可观测面板、模型调度状态和会话记忆。", body),
    ]
    story += add_image(ASSETS / "shortlink-list.png", "图 1：原始短链系统页面。左侧为短链分组，主表格展示短链标题、短链地址、原始 URL、访问次数、访问人数、IP 数和操作入口。", styles)
    story += add_image(ASSETS / "agent-dashboard.png", "图 2：智能短链运营助手。用户通过自然语言查看分组短链列表并创建知乎短链，右侧展示意图、工具数量、成功率、平均耗时、链路追踪和意图分布。", styles)
    story += add_image(ASSETS / "agent-model-routing.png", "图 3：模型调度、调度状态、会话记忆与建议动作。可看到默认模型、候选 provider、熔断状态、会话历史摘要和推荐下一步操作。", styles, max_height=112 * mm)

    story += [
        para("从截图可以看出，新增智能助手不是覆盖原页面，而是与原短链管理台并存：运营人员仍可使用传统表格和图表，也可以用自然语言触发同一批业务能力。右侧可观测面板则把 Agent 的黑盒行为拆成可检查的指标，包括意图、工具数、成功率、平均耗时、会话轮次、模型候选状态和会话记忆。", body),
        table([
            ["实现点", "对应文件/模块", "说明"],
            ["Agent 对话入口", "ShortLinkAgentController", "接收用户问题、conversationId 和上下文参数"],
            ["业务编排", "ShortLinkAgentServiceImpl", "识别意图、解析 URL/有效期/分组、调用业务工具"],
            ["模型配置", "AgentModelProperties", "配置 provider、候选模型、优先级、熔断阈值和兜底策略"],
            ["三态熔断", "AgentCircuitBreakerStore", "记录模型健康状态并控制 CLOSED/OPEN/HALF_OPEN 转换"],
            ["会话记忆", "InMemoryAgentMemoryService", "维护会话窗口、助手回复和摘要快照"],
            ["前端面板", "console-vue/src/views/agent/AgentIndex.vue", "展示聊天、建议动作、图表、模型状态和工具 trace"],
        ], styles, widths=[34 * mm, 55 * mm, 77 * mm]),
    ]

    story += [
        section_title("九、测试、总结与展望", styles, "sec9"),
        para("本项目的验证重点包括后端编译、前端构建、自然语言短链创建、分组列表查询、短链列表查询、统计分析、模型设置展示、会话记忆展示和异常兜底。课程报告中的截图已经覆盖主要演示链路：查看分组短链列表、创建 7 天有效短链、展示工具调用成功率和模型调度状态。", body),
        table([
            ["测试项", "验证方式", "预期结果"],
            ["后端模块编译", "mvn -pl aggregation -am -DskipTests compile", "Agent 聚合模块与依赖模块可编译"],
            ["前端构建", "console-vue 执行 npm run build", "Agent 页面、图表和路由可打包"],
            ["创建短链", "输入“帮我给 https://www.zhihu.com 创建一个 7 天有效的短链”", "返回短链、原始链接、分组和工具 trace"],
            ["查询列表", "输入“查看当前分组短链列表”", "返回当前分组短链概览和 PV 数据"],
            ["模型降级", "禁用或模拟候选模型失败", "主策略熔断后切换 fallback，不影响页面可用性"],
            ["记忆展示", "连续多轮对话", "右侧会话记忆持续记录助手、用户和摘要信息"],
        ], styles, widths=[34 * mm, 76 * mm, 56 * mm]),
        para("课程收获方面，本项目最重要的认识是：企业级 Agent 改造不是给系统加一个聊天框，而是要围绕原业务系统建立“理解目标 - 选择工具 - 执行业务 - 追踪结果 - 压缩记忆 - 降级恢复”的完整工程闭环。与传统 CRUD 相比，Agentic AI 更强调上下文管理、工具边界、状态机、可观测性和失败恢复。", body),
        para("后续可以继续完善四个方向：第一，接入真正的 LLM Function Calling，让意图识别和参数抽取从规则规划器升级为模型工具调用；第二，把当前内存记忆升级为数据库 + Redis 的持久化记忆；第三，将 Tool Trace 持久化为 LLMOps 数据集，用于评估意图识别准确率和工具成功率；第四，补齐 Docker Compose 一键启动 MySQL、Redis、Nacos、Kafka 和各服务，降低演示部署成本。", body),
        Spacer(1, 8 * mm),
        para("说明：封面中的姓名、学号等信息仍为占位内容，提交前请替换为真实信息。", styles["SmallC"]),
    ]
    return story


def main():
    register_fonts()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    frame = Frame(18 * mm, 18 * mm, 174 * mm, 260 * mm, id="normal")
    doc = BookmarkDocTemplate(
        str(TMP_OUT),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=16 * mm,
        bottomMargin=18 * mm,
    )
    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=page_footer)])
    styles = build_styles()
    doc.build(make_story(styles))
    os.replace(TMP_OUT, OUT)
    print(OUT)


if __name__ == "__main__":
    main()
