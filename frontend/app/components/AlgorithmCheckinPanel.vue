<script setup lang="ts">
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

const form = reactive({
  date: new Date().toISOString().slice(0, 10),
  title: '',
  summary: '',
  category: 'REVIEW' as StudyCategory,
  durationMinutes: 60
})

const totalMinutes = computed(() =>
  checkins.value.reduce((sum, item) => sum + item.durationMinutes, 0)
)

const totalDays = computed(() => checkins.value.length)

const recentTitles = computed(() => checkins.value.slice(0, 3))

function formatCategory(category: StudyCategory) {
  return categoryOptions.find((item) => item.value === category)?.label ?? category
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric'
  }).format(new Date(value))
}

async function loadCheckins() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<CheckInItem[]>>(`${apiBase}/api/checkins`)
    checkins.value = response.data
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
    const response = await $fetch<ApiResponse<CheckInItem>>(`${apiBase}/api/checkins`, {
      method: 'POST',
      body: {
        date: form.date,
        title: form.title,
        summary: form.summary.trim() || null,
        category: form.category,
        durationMinutes: Number(form.durationMinutes)
      }
    })

    checkins.value = [response.data, ...checkins.value]
      .sort((left, right) => right.date.localeCompare(left.date))
    successMessage.value = '打卡已保存，算法页现在展示的是你的真实记录。'
    form.title = ''
    form.summary = ''
    form.durationMinutes = 60
  } catch {
    errorMessage.value = '提交失败，请检查表单内容或确认后端接口是否可用。'
  } finally {
    submitting.value = false
  }
}

onMounted(loadCheckins)
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
              <strong>{{ item.title }}</strong>
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
            <article v-for="item in checkins" :key="item.id" class="recent-record">
              <div class="recent-record-meta">
                <span>{{ formatDate(item.date) }}</span>
                <span>{{ formatCategory(item.category) }}</span>
                <span>{{ item.durationMinutes }} 分钟</span>
              </div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary || '这条记录还没有补充摘要。' }}</p>
            </article>
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
}
</style>
