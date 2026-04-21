<script setup lang="ts">
const monthLabel = '2026 年 4 月'
const monthNames = ['一', '二', '三', '四', '五', '六', '日']

const checkinData = [
  { day: 1, level: 1, label: '复习数组与双指针' },
  { day: 2, level: 2, label: '完成 BFS 基础练习' },
  { day: 3, level: 0, label: '未打卡' },
  { day: 4, level: 3, label: '动态规划专题训练' },
  { day: 5, level: 2, label: '整理回溯模板' },
  { day: 6, level: 1, label: '图论入门阅读' },
  { day: 7, level: 0, label: '未打卡' },
  { day: 8, level: 2, label: '做了 3 道二分题' },
  { day: 9, level: 3, label: '完成树与递归复盘' },
  { day: 10, level: 1, label: '更新笔记' },
  { day: 11, level: 2, label: '前缀和专题' },
  { day: 12, level: 0, label: '未打卡' },
  { day: 13, level: 1, label: '刷题 1 小时' },
  { day: 14, level: 2, label: '补充字符串题解' },
  { day: 15, level: 3, label: '完成一组 DP 训练' },
  { day: 16, level: 2, label: '错题回看' },
  { day: 17, level: 1, label: '阅读并查集总结' },
  { day: 18, level: 0, label: '未打卡' },
  { day: 19, level: 3, label: '图论与最短路专题' },
  { day: 20, level: 2, label: '当前正在推进' },
  { day: 21, level: 0, label: '待记录' },
  { day: 22, level: 0, label: '待记录' },
  { day: 23, level: 0, label: '待记录' },
  { day: 24, level: 0, label: '待记录' },
  { day: 25, level: 0, label: '待记录' },
  { day: 26, level: 0, label: '待记录' },
  { day: 27, level: 0, label: '待记录' },
  { day: 28, level: 0, label: '待记录' },
  { day: 29, level: 0, label: '待记录' },
  { day: 30, level: 0, label: '待记录' }
]

const leadingEmptyDays = 1
const gridCells = [
  ...Array.from({ length: leadingEmptyDays }, () => ({ empty: true })),
  ...checkinData.map((item) => ({ ...item, empty: false }))
]

const checkedDays = checkinData.filter((item) => item.level > 0)
const streak = 5
const totalStudyDays = checkedDays.length
</script>

<template>
  <section class="checkin-calendar-card">
    <div class="calendar-header">
      <div>
        <p class="eyebrow">Daily Check-In</p>
        <h2>算法学习打卡</h2>
      </div>
      <div class="calendar-summary">
        <div>
          <strong>{{ streak }}</strong>
          <span>连续打卡</span>
        </div>
        <div>
          <strong>{{ totalStudyDays }}</strong>
          <span>本月已学习</span>
        </div>
      </div>
    </div>

    <div class="calendar-panel">
      <div class="calendar-meta">
        <strong>{{ monthLabel }}</strong>
        <p>用类似热力图的方式快速看到你每天的学习状态，后续这里会接入真实打卡数据。</p>
      </div>

      <div class="calendar-weekdays">
        <span v-for="name in monthNames" :key="name">{{ name }}</span>
      </div>

      <div class="calendar-grid">
        <div
          v-for="(cell, index) in gridCells"
          :key="index"
          class="calendar-cell"
          :class="[
            cell.empty ? 'is-empty' : '',
            !cell.empty ? `level-${cell.level}` : ''
          ]"
          :title="cell.empty ? '' : `4月${cell.day}日：${cell.label}`"
        >
          <span v-if="!cell.empty">{{ cell.day }}</span>
        </div>
      </div>

      <div class="calendar-legend">
        <span>少</span>
        <div class="legend-scale">
          <i class="calendar-cell level-0"></i>
          <i class="calendar-cell level-1"></i>
          <i class="calendar-cell level-2"></i>
          <i class="calendar-cell level-3"></i>
        </div>
        <span>多</span>
      </div>
    </div>

    <div class="calendar-notes">
      <article class="calendar-note-card">
        <p class="mini-label">最近一次高强度学习</p>
        <strong>4 月 19 日 · 图论与最短路专题</strong>
        <span>连续学习时长较长，适合后续继续扩写成专题笔记。</span>
      </article>
      <article class="calendar-note-card">
        <p class="mini-label">接入后端后的目标</p>
        <strong>把真实打卡、笔记和代码文件同步到这里</strong>
        <span>后续会根据每日记录自动计算连续打卡和学习热力图。</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.checkin-calendar-card {
  display: grid;
  gap: 18px;
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03));
  backdrop-filter: blur(12px);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: start;
}

.calendar-header h2 {
  margin: 10px 0 0;
  font-size: clamp(28px, 4vw, 42px);
}

.calendar-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  min-width: 280px;
}

.calendar-summary div,
.calendar-note-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
}

.calendar-summary strong {
  display: block;
  font-size: 28px;
}

.calendar-summary span,
.calendar-meta p,
.calendar-note-card span {
  color: var(--muted);
}

.calendar-panel {
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 24px;
  background: rgba(5, 16, 30, 0.58);
}

.calendar-meta strong {
  display: block;
  font-size: 22px;
  margin-bottom: 8px;
}

.calendar-meta p {
  margin: 0 0 18px;
  max-width: 760px;
  line-height: 1.8;
}

.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.calendar-weekdays {
  margin-bottom: 10px;
}

.calendar-weekdays span {
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

.calendar-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 52px;
  aspect-ratio: 1;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
  font-size: 13px;
}

.calendar-cell.is-empty {
  background: transparent;
  border-style: dashed;
  opacity: 0.25;
}

.calendar-cell.level-0 {
  background: rgba(255, 255, 255, 0.04);
}

.calendar-cell.level-1 {
  background: rgba(107, 211, 255, 0.18);
}

.calendar-cell.level-2 {
  background: rgba(107, 211, 255, 0.34);
}

.calendar-cell.level-3 {
  background: linear-gradient(135deg, rgba(107, 211, 255, 0.62), rgba(255, 211, 107, 0.55));
  color: #07111f;
  font-weight: 700;
}

.calendar-legend {
  display: flex;
  align-items: center;
  justify-content: end;
  gap: 10px;
  margin-top: 16px;
  color: var(--muted);
  font-size: 13px;
}

.legend-scale {
  display: flex;
  gap: 8px;
}

.legend-scale .calendar-cell {
  width: 18px;
  min-width: 18px;
  min-height: 18px;
  aspect-ratio: 1;
}

.calendar-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.calendar-note-card strong {
  display: block;
  margin: 8px 0;
  font-size: 20px;
}

@media (max-width: 860px) {
  .calendar-header,
  .calendar-summary,
  .calendar-notes {
    grid-template-columns: 1fr;
    display: grid;
  }

  .calendar-summary {
    min-width: 0;
  }

  .calendar-weekdays,
  .calendar-grid {
    gap: 8px;
  }

  .calendar-cell {
    min-height: 42px;
  }
}
</style>
