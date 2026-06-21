<template>
  <div class="agent-page">
    <section class="chat-panel">
      <header class="panel-header">
        <div>
          <h2>智能短链运营助手</h2>
          <p>创建短链、查询分组、分析访问数据</p>
        </div>
        <el-button size="small" @click="resetChat">清空</el-button>
      </header>

      <div class="messages">
        <div
          v-for="item in messages"
          :key="item.id"
          class="message"
          :class="item.role"
        >
          <div class="bubble">{{ item.content }}</div>
        </div>
      </div>

      <div class="quick-actions">
        <el-button
          v-for="item in quickPrompts"
          :key="item"
          size="small"
          @click="sendPrompt(item)"
        >
          {{ item }}
        </el-button>
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

    <aside class="trace-panel">
      <div class="trace-block">
        <h3>意图</h3>
        <div class="intent">{{ latestResult?.intent || '等待输入' }}</div>
      </div>

      <div class="trace-block">
        <h3>建议</h3>
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
      </div>

      <div class="trace-block">
        <h3>工具调用</h3>
        <div v-if="latestResult?.toolCalls?.length" class="tool-list">
          <div v-for="(tool, index) in latestResult.toolCalls" :key="index" class="tool-item">
            <div class="tool-title">
              <span>{{ tool.toolName }}</span>
              <em>{{ tool.success ? '成功' : '失败' }}</em>
            </div>
            <p>{{ tool.message }}</p>
          </div>
        </div>
        <span v-else class="empty">暂无工具调用</span>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const API = proxy.$API
const loading = ref(false)
const message = ref('')
const latestResult = ref(null)
const messages = ref([
  {
    id: Date.now(),
    role: 'assistant',
    content: '我是智能短链运营助手，可以帮你创建短链、批量创建、查询分组和生成访问分析。'
  }
])
const quickPrompts = [
  '查看当前分组列表',
  '查看当前分组短链列表',
  '分析默认分组最近 7 天访问情况'
]

const resetChat = () => {
  messages.value = []
  latestResult.value = null
}

const sendPrompt = (content) => {
  message.value = content
  sendMessage()
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
  loading.value = true
  try {
    const res = await API.agent.chat({ message: content })
    const data = res?.data?.data
    latestResult.value = data
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: data?.answer || '没有获取到有效回复'
    })
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '智能助手请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.agent-page {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  padding: 16px;
  background: #f4f6f8;
  box-sizing: border-box;
}

.chat-panel,
.trace-panel {
  min-height: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.chat-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
}

.panel-header {
  height: 72px;
  padding: 0 18px;
  border-bottom: 1px solid #edf0f3;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0 0 6px;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.messages {
  padding: 18px;
  overflow-y: auto;
}

.message {
  display: flex;
  margin-bottom: 14px;

  &.user {
    justify-content: flex-end;
  }

  &.assistant {
    justify-content: flex-start;
  }
}

.bubble {
  max-width: min(680px, 82%);
  white-space: pre-wrap;
  line-height: 1.7;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #273142;
  font-size: 14px;
}

.message.user .bubble {
  color: #fff;
  background: #3478f6;
}

.quick-actions {
  padding: 0 18px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.composer {
  border-top: 1px solid #edf0f3;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 84px;
  gap: 10px;
}

.trace-panel {
  padding: 16px;
  overflow-y: auto;
}

.trace-block {
  margin-bottom: 18px;

  h3 {
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 600;
    color: #374151;
  }
}

.intent {
  min-height: 34px;
  line-height: 34px;
  padding: 0 10px;
  border-radius: 4px;
  background: #eef2ff;
  color: #3147a3;
  font-size: 13px;
}

.suggestions {
  display: grid;
  gap: 8px;

  button {
    text-align: left;
    border: 1px solid #e5e7eb;
    background: #fff;
    color: #374151;
    border-radius: 4px;
    padding: 8px 9px;
    cursor: pointer;
  }
}

.tool-list {
  display: grid;
  gap: 10px;
}

.tool-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px;

  p {
    margin: 8px 0 0;
    color: #6b7280;
    font-size: 12px;
  }
}

.tool-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #111827;

  em {
    font-style: normal;
    color: #14804a;
  }
}

.empty {
  color: #9ca3af;
  font-size: 13px;
}

@media (max-width: 900px) {
  .agent-page {
    grid-template-columns: 1fr;
  }

  .trace-panel {
    max-height: 300px;
  }
}
</style>
