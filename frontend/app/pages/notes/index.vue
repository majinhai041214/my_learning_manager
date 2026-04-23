<script setup lang="ts">
import { renderInputTextToHtml } from '~/utils/notePreview'

interface StudyNote {
  id: number
  title: string
  description: string | null
  originalFilename: string
  extension: string
  contentType: string
  size: number
  uploadedAt: string
  tags: string[]
  viewUrl: string
  downloadUrl: string
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
const router = useRouter()
const route = useRoute()

const notes = ref<StudyNote[]>([])
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const selectedFile = ref<File | null>(null)

const form = reactive({
  title: '',
  description: '',
  tags: ''
})

const selectedTag = ref('ALL')

const supportedTypes = [
  '.md',
  '.markdown',
  '.pdf',
  '.txt',
  '.cpp',
  '.cc',
  '.cxx',
  '.c',
  '.h',
  '.hpp',
  '.py',
  '.java',
  '.js',
  '.ts'
].join(',')

const totalSizeLabel = computed(() => {
  const total = notes.value.reduce((sum, item) => sum + item.size, 0)
  return formatFileSize(total)
})

const tagSummaries = computed(() => {
  const tagMap = new Map<string, number>()
  for (const note of notes.value) {
    for (const tag of note.tags) {
      tagMap.set(tag, (tagMap.get(tag) ?? 0) + 1)
    }
  }

  return Array.from(tagMap.entries())
    .map(([tag, count]) => ({ tag, count }))
    .sort((left, right) => right.count - left.count || left.tag.localeCompare(right.tag, 'zh-CN'))
})

const filteredNotes = computed(() => {
  if (selectedTag.value === 'ALL') {
    return notes.value
  }
  return notes.value.filter(note => note.tags.includes(selectedTag.value))
})

const noteCountLabel = computed(() =>
  selectedTag.value === 'ALL'
    ? `共 ${filteredNotes.value.length} 份学习笔记`
    : `标签“${selectedTag.value}”下共 ${filteredNotes.value.length} 份学习笔记`
)

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function formatFileSize(value: number) {
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function renderDescriptionText(value: string | null, fallback: string) {
  return renderInputTextToHtml(value?.trim() || fallback)
}

async function loadNotes() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await $fetch<ApiResponse<StudyNote[]>>('/notes', {
      baseURL: apiBase
    })
    notes.value = response.data
  } catch {
    errorMessage.value = '暂时无法读取学习笔记，请确认后端服务已经启动。'
  } finally {
    loading.value = false
  }
}

async function applyTagFilter(tag: string) {
  selectedTag.value = tag
  await router.replace({
    query: tag === 'ALL' ? {} : { tag }
  })
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
}

async function uploadNote() {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择一个学习笔记文件。'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const payload = new FormData()
    payload.append('file', selectedFile.value)

    if (form.title.trim()) {
      payload.append('title', form.title.trim())
    }

    if (form.description.trim()) {
      payload.append('description', form.description.trim())
    }

    if (form.tags.trim()) {
      payload.append('tags', form.tags.trim())
    }

    const response = await $fetch<ApiResponse<StudyNote>>('/notes/upload', {
      baseURL: apiBase,
      method: 'POST',
      body: payload
    })

    const createdNote = response.data

    notes.value = [createdNote, ...notes.value]
      .sort((left, right) => new Date(right.uploadedAt).getTime() - new Date(left.uploadedAt).getTime())
    successMessage.value = '学习笔记上传成功，正在跳转到详情页。'
    form.title = ''
    form.description = ''
    form.tags = ''
    selectedFile.value = null

    const fileInput = document.getElementById('note-file-input') as HTMLInputElement | null
    if (fileInput) {
      fileInput.value = ''
    }

    await router.push(`/notes/${createdNote.id}`)
  } catch (error: any) {
    errorMessage.value = error?.data?.message ?? '上传失败，请检查文件类型或稍后重试。'
  } finally {
    submitting.value = false
  }
}

onMounted(loadNotes)

watch(
  () => route.query.tag,
  (tag) => {
    if (typeof tag === 'string' && tag.trim()) {
      selectedTag.value = tag
      return
    }
    selectedTag.value = 'ALL'
  },
  { immediate: true }
)
</script>

<template>
  <div class="page-stack">
    <section class="page-hero">
      <p class="eyebrow">Study Notes</p>
      <h1>把学习笔记上传到独立页面统一管理</h1>
      <p>
        学习笔记不再混在算法页里。这里专门负责上传、查看和整理 Markdown、PDF 以及常见代码文件，
        让算法页继续聚焦在路线和打卡，而笔记页负责内容沉淀。
      </p>
    </section>

    <section class="section-block notes-workspace-grid">
      <article class="content-card note-upload-card">
        <p class="eyebrow">Upload</p>
        <h2>上传新的学习笔记</h2>

        <form class="note-upload-form" @submit.prevent="uploadNote">
          <label>
            <span>文件</span>
            <input
              id="note-file-input"
              type="file"
              :accept="supportedTypes"
              @change="handleFileChange"
              required
            />
          </label>

          <label>
            <span>标题（可选）</span>
            <input
              v-model="form.title"
              type="text"
              maxlength="80"
              placeholder="不填则默认使用文件名"
            />
          </label>

          <label>
            <span>说明（可选）</span>
            <textarea
              v-model="form.description"
              rows="4"
              maxlength="300"
              placeholder="例如：这份笔记主要整理了图论最短路模板和错题反思。"
            />
          </label>

          <label>
            <span>标签（可选）</span>
            <input
              v-model="form.tags"
              type="text"
              maxlength="120"
              placeholder="例如：图论, 最短路, 模板题"
            />
          </label>

          <p class="upload-hint">
            当前支持：Markdown、PDF、TXT、CPP、PY、JAVA、JS、TS 等常见学习笔记文件。
          </p>

          <div class="form-feedback" aria-live="polite">
            <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
            <p v-else-if="successMessage" class="feedback success">{{ successMessage }}</p>
          </div>

          <button class="button primary submit-button" :disabled="submitting">
            {{ submitting ? '上传中...' : '上传学习笔记' }}
          </button>
        </form>
      </article>

      <div class="notes-side-column">
        <article class="content-card notes-summary-card">
          <p class="eyebrow">Library Snapshot</p>
          <h2>当前笔记库概况</h2>

          <div class="metric-row">
            <div>
              <strong>{{ notes.length }}</strong>
              <span>已上传笔记</span>
            </div>
            <div>
              <strong>{{ totalSizeLabel }}</strong>
              <span>累计体积</span>
            </div>
          </div>
        </article>

        <NoteUploadHeatmap :notes="notes" />
      </div>
    </section>

    <section class="section-block">
      <article class="content-card notes-library-card">
        <div class="notes-library-head">
          <div>
            <p class="eyebrow">All Notes</p>
            <h2>所有学习笔记列表</h2>
            <p class="notes-library-copy">{{ noteCountLabel }}</p>
          </div>
        </div>

        <div class="notes-filters-panel">
          <div class="notes-filters-head">
            <strong>标签筛选</strong>
            <span>点击标签后只看对应分类的全部笔记</span>
          </div>

          <div class="tag-filter-list">
            <button
              type="button"
              class="tag-filter"
              :class="{ active: selectedTag === 'ALL' }"
              @click="applyTagFilter('ALL')"
            >
              全部
              <span>{{ notes.length }}</span>
            </button>

            <button
              v-for="item in tagSummaries"
              :key="item.tag"
              type="button"
              class="tag-filter"
              :class="{ active: selectedTag === item.tag }"
              @click="applyTagFilter(item.tag)"
            >
              {{ item.tag }}
              <span>{{ item.count }}</span>
            </button>
          </div>

          <p v-if="!tagSummaries.length" class="filters-empty-copy">
            当前还没有可用标签。你可以在上传学习笔记时填写标签，后面这里就会自动生成筛选按钮。
          </p>
        </div>

        <div v-if="loading" class="state-copy">正在加载学习笔记...</div>
        <div v-else-if="!notes.length" class="state-copy">当前还没有学习笔记，先上传第一份吧。</div>
        <div v-else-if="!filteredNotes.length" class="state-copy">当前标签下还没有学习笔记。</div>
        <div v-else class="all-notes-grid">
          <NuxtLink
            v-for="note in filteredNotes"
            :key="note.id"
            class="note-card-link"
            :to="`/notes/${note.id}`"
          >
            <article class="note-card">
              <div class="note-card-meta">
                <span>{{ note.extension.toUpperCase() }}</span>
                <span>{{ formatFileSize(note.size) }}</span>
                <span>{{ formatDate(note.uploadedAt) }}</span>
              </div>
              <h3>{{ note.title }}</h3>
              <div
                class="note-card-description rich-text-rendered"
                v-html="renderDescriptionText(note.description, note.originalFilename)"
              ></div>
              <div v-if="note.tags.length" class="note-tags">
                <span v-for="tag in note.tags" :key="tag" class="note-tag">{{ tag }}</span>
              </div>
            </article>
          </NuxtLink>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.notes-workspace-grid {
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
}

.notes-workspace-grid,
.notes-side-column,
.note-upload-form {
  display: grid;
  gap: 18px;
}

.note-upload-form label {
  display: grid;
  gap: 8px;
}

.note-upload-form span,
.upload-hint,
.note-card-description,
.note-card-meta,
.state-copy {
  color: var(--muted);
}

.note-upload-form input,
.note-upload-form textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  font: inherit;
}

.note-upload-form textarea {
  resize: vertical;
  min-height: 120px;
}

.upload-hint,
.state-copy {
  margin: 0;
  line-height: 1.8;
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

.notes-summary-card .metric-row {
  margin-top: 10px;
}

.notes-library-card {
  display: grid;
  gap: 20px;
}

.notes-filters-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.03);
}

.notes-library-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.notes-library-copy {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.8;
}

.notes-filters-head {
  display: grid;
  gap: 6px;
}

.notes-filters-head strong {
  font-size: 18px;
}

.notes-filters-head span,
.filters-empty-copy {
  color: var(--muted);
}

.filters-empty-copy {
  margin: 0;
  line-height: 1.8;
}

.tag-filter-list,
.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
  cursor: pointer;
  font: inherit;
}

.tag-filter span,
.note-tag {
  color: var(--muted);
}

.tag-filter.active {
  border-color: rgba(107, 211, 255, 0.35);
  background: rgba(107, 211, 255, 0.1);
}

.all-notes-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.note-card-link {
  display: block;
}

.note-card {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.03);
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.note-card-link:hover .note-card {
  transform: translateY(-2px);
  border-color: rgba(107, 211, 255, 0.35);
}

.note-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}

.note-card h3 {
  margin: 12px 0 10px;
  font-size: 22px;
}

.note-card-description {
  margin: 0;
  line-height: 1.8;
}

.note-tags {
  margin-top: 14px;
}

.note-tag {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 13px;
}

@media (max-width: 860px) {
  .notes-workspace-grid {
    grid-template-columns: 1fr;
  }

  .all-notes-grid {
    grid-template-columns: 1fr;
  }

  .submit-button {
    width: 100%;
  }
}
</style>
