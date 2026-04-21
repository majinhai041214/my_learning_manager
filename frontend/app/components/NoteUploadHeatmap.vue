<script setup lang="ts">
interface StudyNoteHeatmapItem {
  id: number
  uploadedAt: string
}

const props = defineProps<{
  notes: StudyNoteHeatmapItem[]
}>()

const dayLabels = ['一', '二', '三', '四', '五', '六', '日']

const calendarCells = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const start = new Date(today)
  start.setDate(today.getDate() - 27)

  const counts = new Map<string, number>()
  for (const note of props.notes) {
    const date = new Date(note.uploadedAt)
    date.setHours(0, 0, 0, 0)
    const key = date.toISOString().slice(0, 10)
    counts.set(key, (counts.get(key) ?? 0) + 1)
  }

  const cells = []
  for (let index = 0; index < 28; index += 1) {
    const current = new Date(start)
    current.setDate(start.getDate() + index)
    const key = current.toISOString().slice(0, 10)
    const count = counts.get(key) ?? 0

    cells.push({
      key,
      label: `${current.getMonth() + 1}月${current.getDate()}日`,
      count,
      level: count >= 4 ? 4 : count >= 3 ? 3 : count >= 2 ? 2 : count >= 1 ? 1 : 0
    })
  }

  return cells
})

const totalUploadDays = computed(() =>
  calendarCells.value.filter(cell => cell.count > 0).length
)

const busiestDay = computed(() => {
  const sorted = [...calendarCells.value]
    .filter(cell => cell.count > 0)
    .sort((left, right) => right.count - left.count)
  return sorted[0] ?? null
})
</script>

<template>
  <article class="content-card upload-heatmap-card">
    <div class="upload-heatmap-head">
      <div>
        <p class="eyebrow">Upload Activity</p>
        <h2>最近 28 天上传热力图</h2>
      </div>

      <div class="upload-heatmap-summary">
        <div>
          <strong>{{ totalUploadDays }}</strong>
          <span>有上传的天数</span>
        </div>
        <div>
          <strong>{{ busiestDay?.count ?? 0 }}</strong>
          <span>{{ busiestDay ? `${busiestDay.label} 最多` : '暂无峰值' }}</span>
        </div>
      </div>
    </div>

    <div class="upload-heatmap-panel">
      <div class="upload-heatmap-weekdays">
        <span v-for="label in dayLabels" :key="label">{{ label }}</span>
      </div>

      <div class="upload-heatmap-grid">
        <div
          v-for="cell in calendarCells"
          :key="cell.key"
          class="upload-heatmap-cell"
          :class="`level-${cell.level}`"
          :title="`${cell.label}：上传 ${cell.count} 份笔记`"
        >
          <span>{{ cell.count > 0 ? cell.count : '' }}</span>
        </div>
      </div>

      <div class="upload-heatmap-legend">
        <span>少</span>
        <div class="legend-scale">
          <i class="upload-heatmap-cell level-0"></i>
          <i class="upload-heatmap-cell level-1"></i>
          <i class="upload-heatmap-cell level-2"></i>
          <i class="upload-heatmap-cell level-3"></i>
          <i class="upload-heatmap-cell level-4"></i>
        </div>
        <span>多</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.upload-heatmap-card,
.upload-heatmap-panel {
  display: grid;
  gap: 18px;
}

.upload-heatmap-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}

.upload-heatmap-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-width: 260px;
}

.upload-heatmap-summary div {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.upload-heatmap-summary strong {
  display: block;
  font-size: 24px;
}

.upload-heatmap-summary span,
.upload-heatmap-weekdays span,
.upload-heatmap-legend {
  color: var(--muted);
}

.upload-heatmap-weekdays,
.upload-heatmap-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.upload-heatmap-weekdays span {
  text-align: center;
  font-size: 13px;
}

.upload-heatmap-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
}

.upload-heatmap-cell.level-0 {
  background: rgba(255, 255, 255, 0.03);
}

.upload-heatmap-cell.level-1 {
  background: rgba(107, 211, 255, 0.16);
}

.upload-heatmap-cell.level-2 {
  background: rgba(107, 211, 255, 0.3);
}

.upload-heatmap-cell.level-3 {
  background: rgba(255, 211, 107, 0.35);
  color: #07111f;
  font-weight: 700;
}

.upload-heatmap-cell.level-4 {
  background: linear-gradient(135deg, rgba(107, 211, 255, 0.72), rgba(255, 211, 107, 0.66));
  color: #07111f;
  font-weight: 700;
}

.upload-heatmap-legend,
.legend-scale {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-heatmap-legend {
  justify-content: end;
  font-size: 13px;
}

.legend-scale .upload-heatmap-cell {
  width: 16px;
  min-width: 16px;
  min-height: 16px;
}

@media (max-width: 860px) {
  .upload-heatmap-head,
  .upload-heatmap-summary {
    display: grid;
    grid-template-columns: 1fr;
  }

  .upload-heatmap-summary {
    min-width: 0;
  }
}
</style>
