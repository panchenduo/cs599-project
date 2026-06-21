<template>
  <div class="agent-page">
    <section class="workspace">
      <header class="workspace-header">
        <div>
          <h2>智能短链运营助手</h2>
          <p>自然语言编排短链创建、分组查询、访问统计和运营分析</p>
        </div>
        <div class="header-actions">
          <span class="status-dot"></span>
          <span>Java Agent</span>
          <el-button size="small" @click="resetChat">清空会话</el-button>
        </div>
      </header>

      <div ref="messageContainer" class="messages">
        <div
          v-for="item in messages"
          :key="item.id"
          class="message"
          :class="item.role"
        >
          <div class="avatar">{{ item.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <pre>{{ item.content }}</pre>
          </div>
        </div>
      </div>

      <div class="quick-actions">
        <button
          v-for="item in quickPrompts"
          :key="item"
          type="button"
          @click="sendPrompt(item)"
        >
          {{ item }}
        </button>
      </div>

      <div class="composer">
        <el-input
          v-model="message"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入指令，例如：帮我给 https://www.zhihu.com 创建一个 7 天有效的短链"
          @keydown.ctrl.enter="sendMessage"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </section>

    <aside class="inspector">
      <section class="metric-row">
        <div>
          <span>意图</span>
          <strong>{{ latestResult?.intent || '待识别' }}</strong>
        </div>
        <div>
          <span>工具</span>
          <strong>{{ latestResult?.toolCalls?.length || 0 }}</strong>
        </div>
      </section>

      <section class="agent-metrics">
        <div>
          <span>成功率</span>
          <strong>{{ toolSuccessRate }}%</strong>
        </div>
        <div>
          <span>平均耗时</span>
          <strong>{{ avgDuration }}ms</strong>
        </div>
        <div>
          <span>会话轮次</span>
          <strong>{{ userMessageCount }}</strong>
        </div>
      </section>

      <section class="panel trace-panel">
        <h3>链路追踪概览</h3>
        <div ref="traceChartRef" class="chart-box"></div>
      </section>

      <section class="panel chart-grid">
        <div>
          <h3>工具状态</h3>
          <div ref="statusChartRef" class="chart-box small"></div>
        </div>
        <div>
          <h3>意图分布</h3>
          <div ref="intentChartRef" class="chart-box small"></div>
        </div>
      </section>

      <section class="panel">
        <h3>调度状态</h3>
        <p class="muted">{{ latestResult?.dispatchStatus || '等待首次调度' }}</p>
      </section>

      <section class="panel">
        <h3>会话记忆</h3>
        <p class="memory">{{ latestResult?.memorySummary || '暂无压缩记忆' }}</p>
      </section>

      <section class="panel">
        <h3>建议动作</h3>
        <div v-if="latestResult?.suggestions?.length" class="suggestions">
          <button
            v-for="item in latestResult.suggestions"
            :key="item"
            type="button"
            @click="sendPrompt(item)"
          >
            {{ item }}
          </button>
        </div>
        <span v-else class="empty">暂无建议</span>
      </section>

      <section class="panel">
        <h3>工具调用轨迹</h3>
        <div v-if="latestResult?.toolCalls?.length" class="tool-list">
          <div
            v-for="(tool, index) in latestResult.toolCalls"
            :key="index"
            class="tool-item"
            :class="{ failed: !tool.success }"
          >
            <div class="tool-title">
              <span>{{ tool.toolName }}</span>
              <em>{{ tool.success ? '成功' : '失败' }}</em>
            </div>
            <p>{{ tool.message }} · {{ tool.durationMs || 0 }}ms</p>
          </div>
        </div>
        <span v-else class="empty">暂无工具调用</span>
      </section>
    </aside>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { ref, getCurrentInstance, nextTick, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const API = proxy.$API
const loading = ref(false)
const message = ref('')
const latestResult = ref(null)
const messageContainer = ref(null)
const traceChartRef = ref(null)
const statusChartRef = ref(null)
const intentChartRef = ref(null)
let traceChart = null
let statusChart = null
let intentChart = null
const conversationId = ref(localStorage.getItem('shortlink_agent_conversation_id') || '')
const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '我是智能短链运营助手。你可以让我创建短链、批量创建、查询分组、查看短链列表，或分析最近访问表现。'
  }
])
const quickPrompts = [
  '查看当前分组列表',
  '查看当前分组短链列表',
  '分析默认分组最近 7 天访问情况',
  '帮我给 https://www.zhihu.com 创建一个 7 天有效的短链'
]

const toolCalls = computed(() => latestResult.value?.toolCalls || [])
const userMessageCount = computed(() => messages.value.filter((item) => item.role === 'user').length)
const toolSuccessRate = computed(() => {
  if (!toolCalls.value.length) return 0
  const successCount = toolCalls.value.filter((item) => item.success).length
  return Math.round((successCount / toolCalls.value.length) * 100)
})
const avgDuration = computed(() => {
  if (!toolCalls.value.length) return 0
  const total = toolCalls.value.reduce((sum, item) => sum + Number(item.durationMs || 0), 0)
  return Math.round(total / toolCalls.value.length)
})
const intentHistory = computed(() => {
  const counter = {}
  messages.value
    .filter((item) => item.intent)
    .forEach((item) => {
      counter[item.intent] = (counter[item.intent] || 0) + 1
    })
  if (latestResult.value?.intent) {
    counter[latestResult.value.intent] = (counter[latestResult.value.intent] || 0) + 1
  }
  return Object.entries(counter).map(([name, value]) => ({ name, value }))
})

const ensureCharts = () => {
  if (traceChartRef.value && !traceChart) traceChart = echarts.init(traceChartRef.value)
  if (statusChartRef.value && !statusChart) statusChart = echarts.init(statusChartRef.value)
  if (intentChartRef.value && !intentChart) intentChart = echarts.init(intentChartRef.value)
}

const renderCharts = () => {
  ensureCharts()
  const names = toolCalls.value.map((item) => item.toolName)
  const durations = toolCalls.value.map((item) => Number(item.durationMs || 0))
  traceChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 14, top: 18, bottom: 34 },
    xAxis: { type: 'category', data: names.length ? names : ['等待调用'], axisLabel: { color: '#718096' } },
    yAxis: { type: 'value', axisLabel: { color: '#718096' }, splitLine: { lineStyle: { color: '#eef2f6' } } },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: durations.length ? durations : [0],
      areaStyle: { color: 'rgba(37, 99, 235, .12)' },
      lineStyle: { color: '#2563eb', width: 3 },
      itemStyle: { color: '#2563eb' }
    }]
  })

  const successCount = toolCalls.value.filter((item) => item.success).length
  const failedCount = Math.max(toolCalls.value.length - successCount, 0)
  statusChart?.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['50%', '72%'],
      label: { color: '#475569' },
      data: [
        { name: '成功', value: successCount },
        { name: '失败', value: failedCount }
      ],
      color: ['#10b981', '#ef4444']
    }]
  })

  intentChart?.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['0%', '72%'],
      label: { color: '#475569' },
      data: intentHistory.value.length ? intentHistory.value : [{ name: '待识别', value: 1 }],
      color: ['#2563eb', '#14b8a6', '#f59e0b', '#8b5cf6', '#64748b']
    }]
  })
}

const resizeCharts = () => {
  traceChart?.resize()
  statusChart?.resize()
  intentChart?.resize()
}

const resetChat = () => {
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: '会话已清空。我会开启新的上下文。'
    }
  ]
  latestResult.value = null
  conversationId.value = ''
  localStorage.removeItem('shortlink_agent_conversation_id')
  nextTick(renderCharts)
}

const sendPrompt = (content) => {
  message.value = content
  sendMessage()
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  const content = message.value.trim()
  if (!content) {
    ElMessage.warning('请输入指令')
    return
  }
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content
  })
  message.value = ''
  await scrollToBottom()
  loading.value = true
  try {
    const res = await API.agent.chat({
      conversationId: conversationId.value,
      message: content
    })
    const data = res?.data?.data
    latestResult.value = data
    if (data?.conversationId) {
      conversationId.value = data.conversationId
      localStorage.setItem('shortlink_agent_conversation_id', data.conversationId)
    }
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      intent: data?.intent,
      content: data?.answer || '没有获取到有效回复'
    })
  } catch (error) {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: error?.response?.data?.message || '智能助手请求失败，请确认后端服务和网关已经启动。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

watch(latestResult, () => nextTick(renderCharts), { deep: true })
watch(messages, () => nextTick(renderCharts), { deep: true })

onMounted(() => {
  nextTick(renderCharts)
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  traceChart?.dispose()
  statusChart?.dispose()
  intentChart?.dispose()
})
</script>

<style lang="scss" scoped>
.agent-page {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
  padding: 14px;
  box-sizing: border-box;
  background: #eef2f5;
  color: #1f2933;
}

.workspace,
.inspector {
  min-height: 0;
}

.workspace {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  background: #ffffff;
  border: 1px solid #dde4ec;
  border-radius: 8px;
  overflow: hidden;
}

.workspace-header {
  height: 76px;
  padding: 0 18px;
  border-bottom: 1px solid #e8edf2;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0 0 7px;
    font-size: 19px;
    font-weight: 650;
    color: #152033;
  }

  p {
    margin: 0;
    color: #6c7888;
    font-size: 13px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #526071;
  font-size: 13px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #24a148;
}

.messages {
  padding: 18px;
  overflow-y: auto;
  background:
    linear-gradient(#ffffff, #ffffff) padding-box,
    repeating-linear-gradient(90deg, rgba(30, 64, 175, .04) 0 1px, transparent 1px 44px);
}

.message {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  margin-bottom: 16px;
  align-items: start;

  &.user {
    grid-template-columns: minmax(0, 1fr) 34px;

    .avatar {
      grid-column: 2;
      background: #1d4ed8;
      color: #fff;
    }

    .bubble {
      grid-column: 1;
      grid-row: 1;
      justify-self: end;
      background: #2457c5;
      color: #fff;
    }
  }
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.bubble {
  max-width: min(760px, 88%);
  padding: 11px 13px;
  border-radius: 8px;
  background: #f4f7fb;
  color: #263241;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .05);

  pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.72;
    font-family: inherit;
    font-size: 14px;
  }
}

.quick-actions {
  padding: 0 18px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  button {
    border: 1px solid #d3dce7;
    background: #fff;
    color: #334155;
    border-radius: 4px;
    padding: 7px 9px;
    cursor: pointer;
  }
}

.composer {
  border-top: 1px solid #e8edf2;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 86px;
  gap: 10px;
  background: #fbfcfe;
}

.inspector {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.metric-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;

  div {
    min-height: 64px;
    padding: 12px;
    background: #fff;
    border: 1px solid #dde4ec;
    border-radius: 8px;
  }

  span {
    display: block;
    color: #778396;
    font-size: 12px;
    margin-bottom: 8px;
  }

  strong {
    color: #152033;
    font-size: 16px;
    font-weight: 650;
    word-break: break-word;
  }
}

.agent-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;

  div {
    min-height: 58px;
    padding: 10px;
    background: #ffffff;
    border: 1px solid #dde4ec;
    border-radius: 8px;
  }

  span {
    display: block;
    color: #778396;
    font-size: 12px;
    margin-bottom: 7px;
  }

  strong {
    display: block;
    color: #172033;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
  }
}

.trace-panel {
  padding-bottom: 10px;
}

.chart-box {
  width: 100%;
  height: 180px;

  &.small {
    height: 150px;
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;

  > div {
    min-width: 0;
  }

  h3 {
    margin-bottom: 4px;
  }
}

.panel {
  padding: 14px;
  background: #fff;
  border: 1px solid #dde4ec;
  border-radius: 8px;

  h3 {
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 650;
    color: #1f2933;
  }
}

.muted,
.memory,
.empty {
  color: #6c7888;
  font-size: 13px;
  line-height: 1.65;
}

.memory {
  max-height: 120px;
  overflow-y: auto;
}

.suggestions {
  display: grid;
  gap: 8px;

  button {
    text-align: left;
    border: 1px solid #dbe3ed;
    background: #f9fbfd;
    color: #334155;
    border-radius: 5px;
    padding: 9px 10px;
    line-height: 1.45;
    cursor: pointer;
  }
}

.tool-list {
  display: grid;
  gap: 10px;
}

.tool-item {
  border: 1px solid #dce8df;
  background: #f8fcf9;
  border-radius: 7px;
  padding: 10px;

  &.failed {
    border-color: #f0d1d1;
    background: #fff8f8;

    em {
      color: #b42318;
    }
  }

  p {
    margin: 8px 0 0;
    color: #6c7888;
    font-size: 12px;
  }
}

.tool-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;

  em {
    flex: none;
    font-style: normal;
    color: #198754;
  }
}

@media (max-width: 1000px) {
  .agent-page {
    grid-template-columns: 1fr;
  }

  .inspector {
    max-height: 360px;
  }
}
</style>
