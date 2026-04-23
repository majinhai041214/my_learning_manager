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

interface ContributionCell {
  key: string
  date: Date
  level: number
  checked: boolean
  durationMinutes: number
  label: string
  isToday: boolean
  isFuture: boolean
}

interface ContributionWeek {
  key: string
  cells: ContributionCell[]
}

const runtimeConfig = useRuntimeConfig()
const apiBase = runtimeConfig.public.apiBase

const loading = ref(true)
const errorMessage = ref('')
const checkins = ref<CheckInItem[]>([])

const weekdayLabels = [
  { full: '一', short: '一' },
  { full: '二', short: '' },
  { full: '三', short: '三' },
  { full: '四', short: '' },
  { full: '五', short: '五' },
  { full: '六', short: '' },
  { full: '日', short: '日' }
]

const weeksToShow = 12

const today = computed(() => {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
})

const rangeStart = computed(() => {
  const start = new Date(today.value)
  const weekday = (start.getDay() + 6) % 7
  start.setDate(start.getDate() - weekday - (weeksToShow - 1) * 7)
  return start
})

const rangeLabel = computed(() => {
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric'
  })
  return `${formatter.format(rangeStart.value)} - ${formatter.format(today.value)}`
})

function compareCheckinsByRecency(left: CheckInItem, right: CheckInItem) {
  return right.date.localeCompare(left.date) || right.createdAt.localeCompare(left.createdAt)
}

const checkinsByDate = computed(() => {
  const map = new Map<string, CheckInItem[]>()

  for (const item of checkins.value) {
    const items = map.get(item.date) ?? []
    items.push(item)
    map.set(item.date, items)
  }

  return map
})

const checkinsInRange = computed(() =>
  checkins.value.filter((item) => {
    const date = parseLocalDate(item.date)
    return date >= rangeStart.value && date <= today.value
  })
)

const maxDailyMinutes = computed(() =>
  Math.max(
    1,
    ...buildDateRange(rangeStart.value, today.value).map((date) => {
      const key = formatDateKey(date)
      const items = checkinsByDate.value.get(key) ?? []
      return items.reduce((sum, item) => sum + item.durationMinutes, 0)
    })
  )
)

const contributionWeeks = computed<ContributionWeek[]>(() => {
  const weeks: ContributionWeek[] = []

  for (let weekIndex = 0; weekIndex < weeksToShow; weekIndex += 1) {
    const weekStart = new Date(rangeStart.value)
    weekStart.setDate(weekStart.getDate() + weekIndex * 7)
    const cells: ContributionCell[] = []

    for (let dayIndex = 0; dayIndex < 7; dayIndex += 1) {
      const currentDate = new Date(weekStart)
      currentDate.setDate(weekStart.getDate() + dayIndex)
      const dateKey = formatDateKey(currentDate)
      const items = checkinsByDate.value.get(dateKey) ?? []
      const durationMinutes = items.reduce((sum, item) => sum + item.durationMinutes, 0)
      const isFuture = currentDate > today.value

      cells.push({
        key: `${dateKey}-${dayIndex}`,
        date: currentDate,
        level: isFuture ? 0 : resolveLevel(durationMinutes, maxDailyMinutes.value),
        checked: !isFuture && items.length > 0,
        durationMinutes,
        label: resolveDayLabel(items, durationMinutes),
        isToday: isSameDate(currentDate, today.value),
        isFuture
      })
    }

    weeks.push({
      key: `week-${weekIndex}`,
      cells
    })
  }

  return weeks
})

const weekMonthLabels = computed(() => {
  let lastMonth = ''

  return contributionWeeks.value.map((week, index) => {
    const month = `${week.cells[0].date.getMonth() + 1}月`
    const label = month === lastMonth ? '' : month
    lastMonth = month
    return {
      key: `month-${index}`,
      label
    }
  })
})

const checkedDays = computed(() =>
  contributionWeeks.value.flatMap((week) => week.cells).filter(cell => cell.checked).length
)

const streak = computed(() => {
  let days = 0
  const cursor = new Date(today.value)

  while (true) {
    const key = formatDateKey(cursor)
    const items = checkinsByDate.value.get(key) ?? []
    if (!items.length) {
      break
    }

    days += 1
    cursor.setDate(cursor.getDate() - 1)
  }

  return days
})

const latestHighIntensityRecord = computed(() => {
  const allCells = contributionWeeks.value
    .flatMap((week) => week.cells)
    .filter((cell) => cell.checked)
    .sort((left, right) => right.date.getTime() - left.date.getTime())

  const highlighted = allCells.find(cell => cell.level === 3)
  return highlighted ?? allCells[0] ?? null
})

const latestHighIntensityTitle = computed(() => {
  if (!latestHighIntensityRecord.value) {
    return '最近 12 周还没有打卡记录'
  }

  return `${formatReadableDate(latestHighIntensityRecord.value.date)} · ${latestHighIntensityRecord.value.label}`
})

const incompleteRecords = computed(() =>
  checkinsInRange.value.filter(item => item.tags?.includes('尚未完全掌握')).length
)

const totalMinutesInRange = computed(() =>
  checkinsInRange.value.reduce((sum, item) => sum + item.durationMinutes, 0)
)

const averageMinutesPerActiveDay = computed(() => {
  if (!checkedDays.value) {
    return 0
  }

  return Math.round(totalMinutesInRange.value / checkedDays.value)
})

const strongestDay = computed(() => {
  const allCells = contributionWeeks.value
    .flatMap((week) => week.cells)
    .filter((cell) => cell.checked)
    .sort((left, right) => right.durationMinutes - left.durationMinutes)

  return allCells[0] ?? null
})

const recentLearningItems = computed(() =>
  checkinsInRange.value
    .slice()
    .sort(compareCheckinsByRecency)
    .slice(0, 2)
    .map((item) => ({
      id: item.id,
      title: item.title,
      meta: `${formatReadableDate(parseLocalDate(item.date))} · ${item.durationMinutes} 分钟`
    }))
)

const todayFocusMinutes = computed(() => {
  const todayKey = formatDateKey(today.value)
  return (checkinsByDate.value.get(todayKey) ?? []).reduce((sum, item) => sum + item.durationMinutes, 0)
})

const incompleteFocusItems = computed(() =>
  checkinsInRange.value
    .filter(item => item.tags?.includes('尚未完全掌握'))
    .slice()
    .sort(compareCheckinsByRecency)
    .slice(0, 3)
    .map((item) => ({
      id: item.id,
      title: item.title,
      meta: `${formatReadableDate(parseLocalDate(item.date))} · ${item.durationMinutes} 分钟`,
      summary: item.summary?.trim() || '这条记录还没有补充摘要，可以优先回看并补充复盘。'
    }))
)

async function loadCheckins() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<CheckInItem[]>>(`${apiBase}/api/checkins`)
    checkins.value = response.data.map(item => ({
      ...item,
      tags: Array.isArray(item.tags) ? item.tags : []
    }))
  } catch {
    errorMessage.value = '暂时无法读取真实打卡数据。'
  } finally {
    loading.value = false
  }
}

function parseLocalDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function formatDateKey(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function isSameDate(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear()
    && left.getMonth() === right.getMonth()
    && left.getDate() === right.getDate()
}

function buildDateRange(start: Date, end: Date) {
  const dates: Date[] = []
  const cursor = new Date(start)

  while (cursor <= end) {
    dates.push(new Date(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }

  return dates
}

function resolveLevel(durationMinutes: number, maxMinutes: number) {
  if (durationMinutes <= 0) {
    return 0
  }

  const ratio = durationMinutes / maxMinutes
  if (ratio >= 0.72 || durationMinutes >= 180) {
    return 3
  }
  if (ratio >= 0.4 || durationMinutes >= 90) {
    return 2
  }
  return 1
}

function resolveDayLabel(items: CheckInItem[], durationMinutes: number) {
  if (!items.length) {
    return '未打卡'
  }

  if (items.length === 1) {
    return `${items[0].title} · ${durationMinutes} 分钟`
  }

  return `${items.length} 条记录 · ${durationMinutes} 分钟`
}

function formatReadableDate(value: Date) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric'
  }).format(value)
}

function formatCellTitle(cell: ContributionCell) {
  const day = formatReadableDate(cell.date)
  if (cell.isFuture) {
    return `${day}：尚未到达`
  }
  return `${day}：${cell.label}`
}

function renderSummaryText(value: string) {
  return renderInputTextToHtml(value)
}

onMounted(loadCheckins)
</script>

<template>
  <section class="checkin-calendar-card">
    <div class="calendar-header">
      <div class="calendar-title-group">
        <p class="eyebrow">Daily Check-In</p>
        <h2>算法学习打卡</h2>
        <p class="calendar-subtitle">用真实记录快速看见最近 12 周的学习密度与节奏。</p>
      </div>

      <div class="calendar-summary">
        <div>
          <strong>{{ streak }}</strong>
          <span>连续打卡</span>
        </div>
        <div>
          <strong>{{ checkedDays }}</strong>
          <span>近 12 周活跃天数</span>
        </div>
      </div>
    </div>

    <div class="calendar-panel">
      <div class="calendar-meta">
        <div>
          <strong>最近 12 周贡献图</strong>
          <p v-if="loading">正在同步真实打卡数据...</p>
          <p v-else-if="errorMessage">{{ errorMessage }}</p>
          <p v-else>{{ rangeLabel }}，颜色越深表示当天累计学习时长越长。</p>
        </div>

        <div class="calendar-legend">
          <span>少</span>
          <div class="legend-scale">
            <i class="contribution-cell level-0"></i>
            <i class="contribution-cell level-1"></i>
            <i class="contribution-cell level-2"></i>
            <i class="contribution-cell level-3"></i>
          </div>
          <span>多</span>
        </div>
      </div>

      <div class="contribution-layout">
        <div class="left-column">
          <div class="contribution-board">
            <div class="month-label-row">
              <span class="month-corner"></span>
              <div class="month-label-grid">
                <span v-for="item in weekMonthLabels" :key="item.key">{{ item.label }}</span>
              </div>
            </div>

            <div class="board-main">
              <div class="weekday-column">
                <span v-for="item in weekdayLabels" :key="item.full">{{ item.short }}</span>
              </div>

              <div class="contribution-grid" role="img" aria-label="最近 12 周学习打卡贡献图">
                <div v-for="week in contributionWeeks" :key="week.key" class="contribution-week">
                  <div
                    v-for="cell in week.cells"
                    :key="cell.key"
                    class="contribution-cell"
                    :class="[
                      `level-${cell.level}`,
                      cell.checked ? 'is-checked' : '',
                      cell.isToday ? 'is-today' : '',
                      cell.isFuture ? 'is-future' : ''
                    ]"
                    :title="formatCellTitle(cell)"
                  >
                    <span v-if="cell.checked" class="check-mark">✓</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <article class="side-fill-card recent-learning-card">
            <p class="mini-label">最近学习</p>
            <div v-if="recentLearningItems.length" class="recent-learning-list">
              <div v-for="item in recentLearningItems" :key="item.id" class="recent-learning-item">
                <strong>{{ item.title }}</strong>
                <span>{{ item.meta }}</span>
              </div>
            </div>
            <span v-else class="side-fill-empty">补一条学习记录后，这里会自动出现。</span>
          </article>
        </div>

        <div class="focus-column">
          <article class="focus-card center-focus-card focus-alert-card">
            <div class="focus-header">
              <div>
                <p class="mini-label">重点回看</p>
                <strong>“尚未完全掌握”的重点项</strong>
              </div>
            </div>

            <div v-if="incompleteFocusItems.length" class="focus-list">
              <article v-for="item in incompleteFocusItems" :key="item.id" class="focus-item">
                <span class="focus-dot"></span>
                <div class="focus-copy">
                  <b>{{ item.title }}</b>
                  <small>{{ item.meta }}</small>
                  <div class="focus-summary rich-text-rendered" v-html="renderSummaryText(item.summary)"></div>
                </div>
              </article>
            </div>
            <div v-else class="focus-empty">
              当前没有“尚未完全掌握”的记录，说明这段时间的回看压力不高。
            </div>
          </article>
        </div>

        <aside class="contribution-sidecar">
          <article class="sidecar-card">
            <p class="mini-label">累计时长</p>
            <strong>{{ totalMinutesInRange }} 分钟</strong>
            <span>最近 12 周的真实学习投入。</span>
          </article>

          <article class="sidecar-card">
            <p class="mini-label">活跃日均值</p>
            <strong>{{ averageMinutesPerActiveDay }} 分钟</strong>
            <span>按有打卡的日期计算平均学习时长。</span>
          </article>

          <article class="sidecar-card">
            <p class="mini-label">最强单日</p>
            <strong v-if="strongestDay">{{ formatReadableDate(strongestDay.date) }}</strong>
            <strong v-else>暂无记录</strong>
            <span v-if="strongestDay">{{ strongestDay.durationMinutes }} 分钟</span>
            <span v-else>补第一条记录后会自动展示。</span>
          </article>

          <article class="side-fill-card focus-card-mini">
            <p class="mini-label">今日专注</p>
            <strong>{{ todayFocusMinutes }} 分钟</strong>
            <span>
              {{ todayFocusMinutes > 0 ? '今天已经有真实学习投入，继续保持这个节奏。' : '今天还没有记录，开始第一条专注学习吧。' }}
            </span>
          </article>
        </aside>
      </div>
    </div>

    <div class="calendar-notes">
      <article class="calendar-note-card">
        <p class="mini-label">最近一次高强度学习</p>
        <strong>{{ latestHighIntensityTitle }}</strong>
        <span>
          {{ latestHighIntensityRecord ? '适合继续沿着这条主题补笔记或做复盘。' : '先补第一条真实记录，这里就会自动生成总结。' }}
        </span>
      </article>

      <article class="calendar-note-card accent-card">
        <p class="mini-label">需要回看的记录</p>
        <strong>{{ incompleteRecords }} 条尚未完全掌握</strong>
        <span>这部分会和下方“真实记录概况”联动，优先提醒你回看。</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.checkin-calendar-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(107, 211, 255, 0.1), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.025));
  backdrop-filter: blur(10px);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.calendar-title-group {
  display: grid;
  gap: 6px;
}

.calendar-header h2 {
  margin: 0;
  font-size: clamp(36px, 5vw, 56px);
  line-height: 1.1;
}

.calendar-subtitle,
.calendar-meta p,
.calendar-note-card span {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.calendar-subtitle {
  font-size: 18px;
}

.calendar-note-card span {
  font-size: 16px;
}

.calendar-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(88px, 1fr));
  gap: 10px;
}

.calendar-summary div,
.calendar-note-card {
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.035);
}

.calendar-summary strong {
  display: block;
  font-size: 22px;
  line-height: 1;
}

.calendar-summary span {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}

.calendar-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(7, 17, 31, 0.82), rgba(7, 17, 31, 0.66));
}

.calendar-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-end;
}

.calendar-meta strong {
  display: block;
  font-size: 28px;
  margin-bottom: 4px;
}

.calendar-meta p {
  font-size: 18px;
}

.contribution-layout {
  display: grid;
  grid-template-columns: auto minmax(420px, 1fr) 260px;
  gap: 20px;
  align-items: stretch;
}

.left-column {
  display: grid;
  gap: 14px;
  align-content: start;
}

.contribution-board {
  display: grid;
  gap: 14px;
  width: max-content;
}

.month-label-row {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 8px;
  align-items: end;
}

.month-corner {
  display: block;
}

.month-label-grid {
  display: grid;
  grid-template-columns: repeat(12, 22px);
  gap: 8px;
  width: max-content;
}

.month-label-grid span {
  color: var(--muted);
  font-size: 12px;
  min-height: 14px;
  width: 22px;
}

.board-main {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 8px;
}

.weekday-column {
  display: grid;
  grid-template-rows: repeat(7, 1fr);
  gap: 8px;
}

.weekday-column span {
  height: 22px;
  display: inline-flex;
  align-items: center;
  color: var(--muted);
  font-size: 14px;
}

.contribution-grid {
  display: grid;
  grid-template-columns: repeat(12, 22px);
  gap: 8px;
  width: max-content;
}

.contribution-week {
  display: grid;
  grid-template-rows: repeat(7, 22px);
  gap: 8px;
  width: 22px;
}

.contribution-cell {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(255, 255, 255, 0.025);
  color: rgba(235, 244, 255, 0.92);
  transition: transform 120ms ease, border-color 120ms ease, box-shadow 120ms ease;
}

.contribution-cell:not(.is-future):hover {
  transform: translateY(-1px);
  border-color: rgba(107, 211, 255, 0.28);
}

.contribution-cell.level-0 {
  background: rgba(255, 255, 255, 0.035);
}

.contribution-cell.level-1 {
  background: rgba(107, 211, 255, 0.16);
}

.contribution-cell.level-2 {
  background: rgba(107, 211, 255, 0.32);
}

.contribution-cell.level-3 {
  background: linear-gradient(135deg, rgba(107, 211, 255, 0.72), rgba(255, 214, 122, 0.58));
  color: #08111f;
  font-weight: 700;
}

.contribution-cell.is-future {
  background: transparent;
  border-color: transparent;
}

.contribution-cell.is-today {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.32) inset;
}

.check-mark {
  font-size: 11px;
  line-height: 1;
  font-weight: 700;
}

.focus-column {
  display: grid;
  gap: 14px;
  min-width: 0;
  align-content: stretch;
}

.focus-card {
  padding: 14px 16px;
  border: 1px solid rgba(107, 211, 255, 0.12);
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(107, 211, 255, 0.08), transparent 45%),
    rgba(255, 255, 255, 0.035);
}

.center-focus-card {
  min-height: 420px;
  height: 100%;
}

.focus-alert-card {
  border-color: rgba(255, 102, 102, 0.2);
  background:
    radial-gradient(circle at top right, rgba(255, 102, 102, 0.12), transparent 42%),
    linear-gradient(180deg, rgba(255, 102, 102, 0.05), rgba(255, 255, 255, 0.03));
}

.focus-header {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.focus-card strong {
  display: block;
  margin: 0;
  font-size: 20px;
  line-height: 1.4;
}

.focus-list {
  display: grid;
  gap: 10px;
}

.focus-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.focus-item:first-child {
  border-top: 0;
  padding-top: 0;
}

.focus-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  margin-top: 6px;
  background: linear-gradient(135deg, #ff7d7d, #ffb36b);
  box-shadow: 0 0 0 4px rgba(255, 102, 102, 0.1);
}

.focus-copy {
  display: grid;
  gap: 4px;
}

.focus-copy b {
  font-size: 18px;
  line-height: 1.5;
  font-weight: 700;
  color: #ffe0e0;
}

.focus-alert-card .mini-label {
  color: #ff9f9f;
}

.focus-copy small,
.focus-empty {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.focus-summary {
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.contribution-sidecar {
  display: grid;
  gap: 12px;
  align-content: start;
}

.sidecar-card,
.side-fill-card {
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.035);
}

.sidecar-card strong,
.focus-card-mini strong,
.recent-learning-item strong {
  display: block;
  margin: 6px 0 4px;
  font-size: 20px;
  line-height: 1.35;
}

.sidecar-card span,
.side-fill-card span {
  color: var(--muted);
  font-size: 15px;
  line-height: 1.6;
}

.recent-learning-list {
  display: grid;
  gap: 10px;
}

.recent-learning-item {
  display: grid;
  gap: 4px;
  padding: 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.recent-learning-item:first-child {
  border-top: 0;
  padding-top: 0;
}

.side-fill-empty {
  display: block;
  margin-top: 6px;
}

.calendar-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.legend-scale {
  display: flex;
  gap: 6px;
}

.legend-scale .contribution-cell {
  width: 12px;
  min-width: 12px;
  min-height: 12px;
  border-radius: 4px;
}

.calendar-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.calendar-note-card strong {
  display: block;
  margin: 6px 0;
  font-size: 22px;
  line-height: 1.5;
}

.mini-label {
  margin: 0;
  color: #8dd8ff;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.accent-card {
  background:
    linear-gradient(180deg, rgba(255, 102, 102, 0.09), rgba(255, 255, 255, 0.035));
}

@media (max-width: 860px) {
  .calendar-header,
  .calendar-meta,
  .calendar-notes {
    display: grid;
    grid-template-columns: 1fr;
  }

  .calendar-summary {
    width: 100%;
  }

  .calendar-legend {
    justify-content: flex-start;
  }

  .contribution-layout {
    grid-template-columns: 1fr;
  }

  .month-label-row,
  .board-main {
    grid-template-columns: 24px minmax(0, 1fr);
  }

  .weekday-column span {
    font-size: 11px;
    height: 16px;
  }

  .contribution-grid,
  .contribution-week,
  .weekday-column,
  .month-label-grid {
    gap: 4px;
  }

  .month-label-grid {
    grid-template-columns: repeat(12, 16px);
  }

  .month-label-grid span {
    width: 16px;
    font-size: 11px;
  }

  .contribution-grid {
    grid-template-columns: repeat(12, 16px);
  }

  .contribution-week {
    grid-template-rows: repeat(7, 16px);
    width: 16px;
  }

  .contribution-cell {
    width: 16px;
    height: 16px;
  }
}
</style>
