<script setup lang="ts">
import { renderInputTextToHtml } from '~/utils/notePreview'

type StudyCategory =
  | 'DATA_STRUCTURE'
  | 'SEARCH'
  | 'DYNAMIC_PROGRAMMING'
  | 'GRAPH'
  | 'STRING'
  | 'MATH'
  | 'REVIEW'
  | 'OTHER'

interface CheckInItem {
  id: number
  date: string
  title: string
  summary: string | null
  category: StudyCategory
  durationMinutes: number
  tags: string[]
  createdAt: string
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

const runtimeConfig = useRuntimeConfig()
const apiBase = runtimeConfig.public.apiBase

const categoryOptions: Array<{ value: StudyCategory; label: string }> = [
  { value: 'DATA_STRUCTURE', label: '基础数据结构' },
  { value: 'SEARCH', label: '搜索与回溯' },
  { value: 'DYNAMIC_PROGRAMMING', label: '动态规划' },
  { value: 'GRAPH', label: '图论' },
  { value: 'STRING', label: '字符串' },
  { value: 'MATH', label: '数学与技巧' },
  { value: 'REVIEW', label: '复盘整理' },
  { value: 'OTHER', label: '其他' }
]

const checkins = ref<CheckInItem[]>([])
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const isTimerRunning = ref(false)
const elapsedSeconds = ref(0)
const currentPage = ref(1)
const editingCheckinId = ref<number | null>(null)
const savingCheckinId = ref<number | null>(null)
const deletingCheckinId = ref<number | null>(null)
let timerId: ReturnType<typeof setInterval> | null = null

const form = reactive({
  date: new Date().toISOString().slice(0, 10),
  title: '',
  summary: '',
  category: 'REVIEW' as StudyCategory,
  durationMinutes: 60,
  tags: ''
})

const editForm = reactive({
  date: '',
  title: '',
  summary: '',
  category: 'REVIEW' as StudyCategory,
  durationMinutes: 60,
  tags: ''
})

const incompleteMasteryTag = '尚未完全掌握'

const totalMinutes = computed(() =>
  checkins.value.reduce((sum, item) => sum + item.durationMinutes, 0)
)

const totalDays = computed(() => checkins.value.length)

function compareCheckinsByRecency(left: CheckInItem, right: CheckInItem) {
  return right.date.localeCompare(left.date) || right.createdAt.localeCompare(left.createdAt)
}

const prioritizedCheckins = computed(() =>
  [...checkins.value].sort((left, right) => {
    const leftPriority = Number(hasIncompleteMasteryTag(left))
    const rightPriority = Number(hasIncompleteMasteryTag(right))
    if (leftPriority !== rightPriority) {
      return rightPriority - leftPriority
    }

    return compareCheckinsByRecency(left, right)
  })
)

const recentTitles = computed(() => prioritizedCheckins.value.slice(0, 3))
const pageSize = 3

const totalPages = computed(() => Math.max(1, Math.ceil(checkins.value.length / pageSize)))

const paginatedCheckins = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return prioritizedCheckins.value.slice(start, start + pageSize)
})

const timerMinutes = computed(() => Math.max(1, Math.ceil(elapsedSeconds.value / 60)))

const formattedElapsedTime = computed(() => {
  const hours = Math.floor(elapsedSeconds.value / 3600)
  const minutes = Math.floor((elapsedSeconds.value % 3600) / 60)
  const seconds = elapsedSeconds.value % 60

  return [hours, minutes, seconds]
    .map(value => String(value).padStart(2, '0'))
    .join(':')
})

function formatCategory(category: StudyCategory) {
  return categoryOptions.find((item) => item.value === category)?.label ?? category
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric'
  }).format(new Date(value))
}

function renderSummaryText(value: string | null) {
  return renderInputTextToHtml(value?.trim() || '这条记录还没有补充摘要。')
}

function normalizeTag(tag: string) {
  const normalized = tag.trim()
  if (normalized === '未完全掌握' || normalized === '尚未掌握' || normalized.toLowerCase() === 'not-mastered') {
    return incompleteMasteryTag
  }
  return normalized
}

function parseTags(rawTags: string) {
  return rawTags
    .split(/[,\n，]/)
    .map(tag => normalizeTag(tag))
    .filter(Boolean)
    .filter((tag, index, items) => items.indexOf(tag) === index)
}

function hasIncompleteMasteryTag(item: CheckInItem) {
  return item.tags.includes(incompleteMasteryTag)
}

function normalizeCheckin(item: CheckInItem) {
  return {
    ...item,
    tags: Array.isArray(item.tags) ? item.tags.map(tag => normalizeTag(tag)).filter(Boolean) : []
  }
}

function formatTagsInput(tags: string[]) {
  return tags.join(', ')
}

function startEditingCheckin(item: CheckInItem) {
  editingCheckinId.value = item.id
  editForm.date = item.date
  editForm.title = item.title
  editForm.summary = item.summary ?? ''
  editForm.category = item.category
  editForm.durationMinutes = item.durationMinutes
  editForm.tags = formatTagsInput(item.tags)
  errorMessage.value = ''
  successMessage.value = ''
}

function cancelEditingCheckin() {
  editingCheckinId.value = null
  editForm.date = ''
  editForm.title = ''
  editForm.summary = ''
  editForm.category = 'REVIEW'
  editForm.durationMinutes = 60
  editForm.tags = ''
}

async function saveCheckin(item: CheckInItem) {
  savingCheckinId.value = item.id
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<CheckInItem>>(`/checkins/${item.id}`, {
      baseURL: apiBase,
      method: 'PUT',
      body: {
        date: editForm.date,
        title: editForm.title,
        summary: editForm.summary.trim() || null,
        category: editForm.category,
        durationMinutes: Number(editForm.durationMinutes),
        tags: parseTags(editForm.tags)
      }
    })

    const updated = normalizeCheckin(response.data)
    checkins.value = checkins.value.map(current => current.id === updated.id ? updated : current)
      .sort(compareCheckinsByRecency)
    successMessage.value = '这条学习记录已更新。'
    cancelEditingCheckin()
  } catch {
    errorMessage.value = '更新学习记录失败，请稍后再试。'
  } finally {
    savingCheckinId.value = null
  }
}

function markAsIncompleteMastery(item: CheckInItem) {
  const nextTags = item.tags.includes(incompleteMasteryTag)
    ? item.tags
    : [incompleteMasteryTag, ...item.tags]
  startEditingCheckin(item)
  editForm.tags = formatTagsInput(nextTags)
}

async function deleteCheckin(item: CheckInItem) {
  if (!window.confirm(`确认删除“${item.title}”这条学习记录吗？`)) {
    return
  }

  deletingCheckinId.value = item.id
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await $fetch<ApiResponse<null>>(`/checkins/${item.id}`, {
      baseURL: apiBase,
      method: 'DELETE'
    })

    checkins.value = checkins.value.filter(current => current.id !== item.id)
    if (editingCheckinId.value === item.id) {
      cancelEditingCheckin()
    }
    currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(checkins.value.length / pageSize)))
    successMessage.value = '这条学习记录已删除。'
  } catch {
    errorMessage.value = '删除学习记录失败，请稍后再试。'
  } finally {
    deletingCheckinId.value = null
  }
}

function stopTimerInterval() {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
}

function startTimer() {
  if (isTimerRunning.value) {
    return
  }

  successMessage.value = ''
  errorMessage.value = ''
  isTimerRunning.value = true
  timerId = setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

function stopTimer() {
  if (!isTimerRunning.value) {
    return
  }

  stopTimerInterval()
  isTimerRunning.value = false
  form.durationMinutes = timerMinutes.value
  successMessage.value = `本次计时已停止，学习时长已同步为 ${timerMinutes.value} 分钟。`
}

function resetTimer(options?: { silent?: boolean }) {
  stopTimerInterval()
  isTimerRunning.value = false
  elapsedSeconds.value = 0
  if (!options?.silent) {
    successMessage.value = '计时器已重置，你可以重新开始记录。'
  }
}

function goToPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

async function loadCheckins() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<CheckInItem[]>>('/checkins', {
      baseURL: apiBase
    })
    checkins.value = response.data
      .map(normalizeCheckin)
      .sort(compareCheckinsByRecency)
    currentPage.value = 1
  } catch {
    errorMessage.value = '暂时无法读取打卡记录，请确认后端服务已经启动。'
  } finally {
    loading.value = false
  }
}

async function submitCheckin() {
  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<CheckInItem>>('/checkins', {
      baseURL: apiBase,
      method: 'POST',
      body: {
        date: form.date,
        title: form.title,
        summary: form.summary.trim() || null,
        category: form.category,
        durationMinutes: Number(form.durationMinutes),
        tags: parseTags(form.tags)
      }
    })

    checkins.value = [normalizeCheckin(response.data), ...checkins.value]
      .sort(compareCheckinsByRecency)
    currentPage.value = 1
    successMessage.value = '打卡已保存，算法页现在展示的是你的真实记录。'
    form.title = ''
    form.summary = ''
    form.durationMinutes = 60
    form.tags = ''
    resetTimer({ silent: true })
  } catch {
    errorMessage.value = '提交失败，请检查表单内容或确认后端接口是否可用。'
  } finally {
    submitting.value = false
  }
}

onMounted(loadCheckins)
watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

onBeforeUnmount(() => {
  stopTimerInterval()
})
</script>

<template>
  <section class="checkin-workspace">
    <div class="section-heading">
      <p class="eyebrow">Real Check-In</p>
      <h2>这里已经可以记录和查看真实打卡</h2>
      <p>
        先把最核心的学习记录能力做成闭环。现在你可以直接新增打卡，算法页会即时展示最近的学习轨迹。
      </p>
    </div>

    <div class="checkin-workspace-grid">
      <article class="content-card checkin-form-card">
        <p class="eyebrow">New Entry</p>
        <h3>新增一条今日学习记录</h3>

        <form class="checkin-form" @submit.prevent="submitCheckin">
          <div class="timer-panel">
            <div class="timer-display-block">
              <span>专注计时器</span>
              <strong>{{ formattedElapsedTime }}</strong>
              <small>点击停止后，会自动同步到下方“学习时长”输入框。</small>
            </div>

            <div class="timer-actions">
              <button
                v-if="!isTimerRunning"
                class="button secondary"
                type="button"
                @click="startTimer"
              >
                开始计时
              </button>
              <button
                v-else
                class="button primary"
                type="button"
                @click="stopTimer"
              >
                停止并同步
              </button>
              <button
                class="button secondary"
                type="button"
                :disabled="isTimerRunning || elapsedSeconds === 0"
                @click="resetTimer"
              >
                重置
              </button>
            </div>
          </div>

          <label>
            <span>日期</span>
            <input v-model="form.date" type="date" required />
          </label>

          <label>
            <span>标题</span>
            <input
              v-model="form.title"
              type="text"
              maxlength="80"
              placeholder="例如：完成一组动态规划题单"
              required
            />
          </label>

          <label>
            <span>分类</span>
            <select v-model="form.category">
              <option
                v-for="option in categoryOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </label>

          <label>
            <span>学习时长（分钟）</span>
            <input v-model="form.durationMinutes" type="number" min="1" max="1440" required />
          </label>

          <label>
            <span>标签（可选）</span>
            <input
              v-model="form.tags"
              type="text"
              placeholder="例如：尚未完全掌握, 二分, 复盘"
            />
          </label>

          <label>
            <span>摘要</span>
            <textarea
              v-model="form.summary"
              rows="4"
              maxlength="300"
              placeholder="记录一下今天做了什么、卡在哪、后面准备怎么补。"
            />
          </label>

          <div class="form-feedback" aria-live="polite">
            <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
            <p v-else-if="successMessage" class="feedback success">{{ successMessage }}</p>
          </div>

          <button class="button primary submit-button" :disabled="submitting">
            {{ submitting ? '保存中...' : '保存打卡' }}
          </button>
        </form>
      </article>

      <div class="checkin-side-column">
        <article class="content-card metric-card live-metric-card">
          <p class="eyebrow">Live Snapshot</p>
          <h3>当前真实记录概况</h3>

          <div class="metric-row">
            <div>
              <strong>{{ totalDays }}</strong>
              <span>累计记录</span>
            </div>
            <div>
              <strong>{{ totalMinutes }}</strong>
              <span>累计分钟</span>
            </div>
          </div>

          <ul class="recent-list" v-if="recentTitles.length">
            <li v-for="item in recentTitles" :key="item.id">
              <div class="recent-list-title">
                <strong>{{ item.title }}</strong>
                <span
                  v-if="hasIncompleteMasteryTag(item)"
                  class="record-tag record-tag-alert"
                >
                  {{ incompleteMasteryTag }}
                </span>
              </div>
              <span>{{ formatDate(item.date) }} · {{ formatCategory(item.category) }}</span>
            </li>
          </ul>
          <p v-else class="empty-copy">还没有真实打卡，先从今天这条开始。</p>
        </article>

        <article class="content-card recent-card">
          <p class="eyebrow">Recent Records</p>
          <h3>最近的学习记录</h3>

          <div v-if="loading" class="state-copy">正在加载打卡记录...</div>
          <div v-else-if="!checkins.length" class="state-copy">当前还没有打卡记录，提交第一条后会显示在这里。</div>
          <div v-else class="recent-records">
            <article v-for="item in paginatedCheckins" :key="item.id" class="recent-record">
              <div class="recent-record-meta">
                <span>{{ formatDate(item.date) }}</span>
                <span>{{ formatCategory(item.category) }}</span>
                <span>{{ item.durationMinutes }} 分钟</span>
              </div>
              <h4>{{ item.title }}</h4>
              <div v-if="item.tags.length" class="record-tag-list">
                <span
                  v-for="tag in item.tags"
                  :key="`${item.id}-${tag}`"
                  class="record-tag"
                  :class="{ 'record-tag-alert': tag === incompleteMasteryTag }"
                >
                  {{ tag }}
                </span>
              </div>
              <div v-if="editingCheckinId === item.id" class="record-editor">
                <label>
                  <span>日期</span>
                  <input v-model="editForm.date" type="date" required />
                </label>
                <label>
                  <span>标题</span>
                  <input
                    v-model="editForm.title"
                    type="text"
                    maxlength="80"
                    placeholder="例如：完成一组动态规划题单"
                    required
                  />
                </label>
                <label>
                  <span>分类</span>
                  <select v-model="editForm.category">
                    <option
                      v-for="option in categoryOptions"
                      :key="`edit-${item.id}-${option.value}`"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>学习时长（分钟）</span>
                  <input v-model="editForm.durationMinutes" type="number" min="1" max="1440" required />
                </label>
                <label>
                  <span>标签（可选）</span>
                  <input
                    v-model="editForm.tags"
                    type="text"
                    placeholder="例如：尚未完全掌握, 二分, 复盘"
                  />
                </label>
                <label>
                  <span>摘要</span>
                  <textarea
                    v-model="editForm.summary"
                    rows="4"
                    maxlength="300"
                    placeholder="记录一下今天做了什么、卡在哪、后面准备怎么补。"
                  />
                </label>
                <div class="record-editor-actions">
                  <button
                    class="button primary"
                    type="button"
                    :disabled="savingCheckinId === item.id"
                    @click="saveCheckin(item)"
                  >
                    {{ savingCheckinId === item.id ? '保存中...' : '保存修改' }}
                  </button>
                  <button
                    class="button secondary"
                    type="button"
                    :disabled="savingCheckinId === item.id"
                    @click="cancelEditingCheckin"
                  >
                    取消
                  </button>
                </div>
              </div>
              <div v-else class="record-actions">
                <button
                  class="button secondary action-button"
                  type="button"
                  @click="startEditingCheckin(item)"
                >
                  编辑记录
                </button>
                <button
                  v-if="!hasIncompleteMasteryTag(item)"
                  class="button secondary action-button alert-button"
                  type="button"
                  @click="markAsIncompleteMastery(item)"
                >
                  标记为尚未完全掌握
                </button>
                <button
                  class="button secondary action-button danger-button"
                  type="button"
                  :disabled="deletingCheckinId === item.id"
                  @click="deleteCheckin(item)"
                >
                  {{ deletingCheckinId === item.id ? '删除中...' : '删除记录' }}
                </button>
              </div>
              <div class="record-summary rich-text-rendered" v-html="renderSummaryText(item.summary)"></div>
            </article>

            <div v-if="totalPages > 1" class="pagination-bar">
              <button
                class="button secondary pagination-button"
                type="button"
                :disabled="currentPage === 1"
                @click="goToPage(currentPage - 1)"
              >
                上一页
              </button>
              <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
              <button
                class="button secondary pagination-button"
                type="button"
                :disabled="currentPage === totalPages"
                @click="goToPage(currentPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.checkin-workspace {
  display: grid;
  gap: 18px;
}

.checkin-workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
}

.checkin-form-card h3,
.live-metric-card h3,
.recent-card h3 {
  margin: 10px 0 18px;
  font-size: 28px;
}

.checkin-form {
  display: grid;
  gap: 14px;
}

.timer-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(107, 211, 255, 0.2);
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(107, 211, 255, 0.12), transparent 38%),
    rgba(255, 255, 255, 0.03);
}

.timer-display-block {
  display: grid;
  gap: 6px;
}

.timer-display-block strong {
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1;
  letter-spacing: 0.06em;
}

.timer-display-block small {
  color: var(--muted);
  line-height: 1.6;
}

.timer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.checkin-form label {
  display: grid;
  gap: 8px;
}

.checkin-form span {
  color: var(--muted);
  font-size: 14px;
}

.checkin-form input,
.checkin-form select,
.checkin-form textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  font: inherit;
}

.checkin-form textarea {
  resize: vertical;
  min-height: 120px;
}

.checkin-form input::placeholder,
.checkin-form textarea::placeholder {
  color: var(--muted);
}

.form-feedback {
  min-height: 24px;
}

.feedback {
  margin: 0;
  font-size: 14px;
}

.feedback.error {
  color: #ff9d9d;
}

.feedback.success {
  color: #99efc5;
}

.submit-button {
  width: fit-content;
}

.checkin-side-column,
.recent-records {
  display: grid;
  gap: 18px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-bar span {
  color: var(--muted);
  font-size: 14px;
}

.pagination-button {
  min-width: 104px;
}

.live-metric-card .metric-row {
  margin-top: 8px;
}

.recent-list {
  display: grid;
  gap: 14px;
  padding: 0;
  margin: 20px 0 0;
  list-style: none;
}

.recent-list li {
  display: grid;
  gap: 4px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.recent-list-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.recent-list span,
.empty-copy,
.state-copy,
.recent-record p,
.recent-record-meta {
  color: var(--muted);
}

.recent-record {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.03);
}

.recent-record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}

.recent-record h4 {
  margin: 12px 0 10px;
  font-size: 20px;
}

.record-summary {
  margin: 0;
  line-height: 1.8;
}

.record-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.record-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(107, 211, 255, 0.2);
  background: rgba(107, 211, 255, 0.08);
  color: #ccefff;
  font-size: 12px;
  line-height: 1.2;
}

.record-tag-alert {
  border-color: rgba(255, 98, 98, 0.35);
  background: rgba(255, 98, 98, 0.14);
  color: #ff9d9d;
}

.record-editor {
  display: grid;
  gap: 10px;
  margin-bottom: 12px;
}

.record-editor label {
  display: grid;
  gap: 8px;
}

.record-editor span {
  color: var(--muted);
  font-size: 14px;
}

.record-editor input,
.record-editor select,
.record-editor textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  font: inherit;
}

.record-editor textarea {
  resize: vertical;
  min-height: 110px;
}

.record-editor input::placeholder,
.record-editor textarea::placeholder {
  color: var(--muted);
}

.record-editor-actions,
.record-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.action-button {
  min-width: 112px;
}

.alert-button {
  border-color: rgba(255, 98, 98, 0.3);
  color: #ffb1b1;
}

.danger-button {
  border-color: rgba(255, 128, 128, 0.26);
  color: #ffc0c0;
}

.recent-record p,
.state-copy,
.empty-copy {
  margin: 0;
  line-height: 1.8;
}

@media (max-width: 860px) {
  .checkin-workspace-grid {
    grid-template-columns: 1fr;
  }

  .submit-button {
    width: 100%;
  }

  .timer-actions > .button,
  .pagination-button,
  .record-editor-actions > .button,
  .record-actions > .button {
    width: 100%;
  }

  .pagination-bar {
    align-items: stretch;
  }
}
</style>
