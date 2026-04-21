<script setup lang="ts">
import { renderCodeToHtml, renderMarkdownToHtml } from '~/utils/notePreview'

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

interface NoteAnnotation {
  id: number
  noteId: number
  quotedText: string
  comment: string
  pageNumber: number | null
  anchorX: number | null
  anchorY: number | null
  selectionRects: Array<{
    x: number
    y: number
    width: number
    height: number
  }>
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const runtimeConfig = useRuntimeConfig()
const apiBase = runtimeConfig.public.apiBase
const noteId = route.params.id

const { data: response, error } = await useFetch<ApiResponse<StudyNote>>(`${apiBase}/api/notes/${noteId}`)
const note = computed(() => response.value?.data ?? null)

const previewableTextExtensions = new Set([
  'md',
  'markdown',
  'txt',
  'cpp',
  'cc',
  'cxx',
  'c',
  'h',
  'hpp',
  'py',
  'java',
  'js',
  'ts'
])

const isPdf = computed(() => note.value?.extension === 'pdf')
const isMarkdown = computed(() => note.value ? ['md', 'markdown'].includes(note.value.extension) : false)
const isTextPreviewable = computed(() => note.value ? previewableTextExtensions.has(note.value.extension) : false)
const supportsAnnotations = computed(() => Boolean(note.value && (isMarkdown.value || isPdf.value)))

const noteText = ref('')
const previewError = ref('')
const previewContainer = ref<HTMLElement | null>(null)
const annotationSidebarRef = ref<HTMLElement | null>(null)
const pdfViewerContainer = ref<HTMLDivElement | null>(null)
const pdfViewerPages = ref<HTMLDivElement | null>(null)
const pdfRenderLoading = ref(false)
const pdfRenderError = ref('')
const pdfDocumentHandle = shallowRef<any>(null)
const pdfLoadingTask = shallowRef<any>(null)
const pdfViewerInstance = shallowRef<any>(null)
const pdfEventBus = shallowRef<any>(null)
const pdfLinkService = shallowRef<any>(null)

const annotations = ref<NoteAnnotation[]>([])
const selectedAnnotationId = ref<number | null>(null)
const pendingFocusTarget = ref<'preview' | 'sidebar' | null>(null)
const annotationLoading = ref(false)
const annotationError = ref('')
const annotationSuccess = ref('')
const selectedQuote = ref('')
const selectedPageNumber = ref<number | null>(null)
const selectedAnchor = ref<{ pageNumber: number, x: number, y: number } | null>(null)
const selectedPdfSelectionRects = ref<Array<{ x: number, y: number, width: number, height: number }>>([])
const showAnnotationComposer = ref(false)
const editingAnnotationId = ref<number | null>(null)
const savingAnnotation = ref(false)
const deletingAnnotationId = ref<number | null>(null)
const noteEditMode = ref(false)
const savingNoteMeta = ref(false)
const deletingNote = ref(false)
const noteManagementError = ref('')
const noteManagementSuccess = ref('')
const annotationForm = reactive({
  comment: ''
})
const editingAnnotationForm = reactive({
  comment: ''
})
const noteMetaForm = reactive({
  title: '',
  description: '',
  tags: ''
})

const renderedPreview = computed(() => {
  if (!note.value || previewError.value || !noteText.value) {
    return ''
  }

  if (isMarkdown.value) {
    return renderMarkdownToHtml(noteText.value)
  }

  if (isTextPreviewable.value) {
    return renderCodeToHtml(noteText.value, note.value.extension)
  }

  return ''
})

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
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

function formatAnnotationDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function renderAnnotationComment(comment: string) {
  return renderMarkdownToHtml(comment)
}

function syncNoteMetaForm() {
  noteMetaForm.title = note.value?.title ?? ''
  noteMetaForm.description = note.value?.description ?? ''
  noteMetaForm.tags = note.value?.tags.join(', ') ?? ''
}

function annotationLocationLabel(annotation: NoteAnnotation) {
  return annotation.pageNumber ? `PDF 第 ${annotation.pageNumber} 页` : '文档批注'
}

function annotationDisplayQuote(annotation: NoteAnnotation) {
  return annotation.quotedText || 'PDF 锚点批注'
}

function shouldOpenAnnotationFromClick(event: MouseEvent, requireModifier = true) {
  return !requireModifier || event.ctrlKey || event.metaKey
}

function focusAnnotationInSidebar(annotationId: number) {
  pendingFocusTarget.value = 'sidebar'
  selectedAnnotationId.value = annotationId
}

function scrollToPdfAnnotation(annotationId: number) {
  const highlight = pdfViewerContainer.value?.querySelector(`[data-annotation-id="${annotationId}"]`)
  if (highlight instanceof HTMLElement) {
    highlight.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
    return
  }

  const anchor = pdfViewerContainer.value?.querySelector(`[data-annotation-anchor-id="${annotationId}"]`)
  if (anchor instanceof HTMLElement) {
    anchor.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
    return
  }

  const page = pdfViewerContainer.value?.querySelector(`.page[data-page-number="${annotations.value.find(item => item.id === annotationId)?.pageNumber ?? ''}"]`)
  if (page instanceof HTMLElement) {
    page.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

async function loadAnnotations() {
  if (!note.value) {
    return
  }

  annotationLoading.value = true
  annotationError.value = ''

  try {
    const annotationResponse = await $fetch<ApiResponse<NoteAnnotation[]>>(`${apiBase}/api/notes/${note.value.id}/annotations`)
    annotations.value = annotationResponse.data
  } catch {
    annotationError.value = '暂时无法读取批注列表。'
  } finally {
    annotationLoading.value = false
  }
}

async function saveNoteMeta() {
  if (!note.value) {
    return
  }

  savingNoteMeta.value = true
  noteManagementError.value = ''
  noteManagementSuccess.value = ''

  try {
    const updated = await $fetch<ApiResponse<StudyNote>>(`${apiBase}/api/notes/${note.value.id}`, {
      method: 'PUT',
      body: {
        title: noteMetaForm.title,
        description: noteMetaForm.description,
        tags: noteMetaForm.tags
          .split(',')
          .map(tag => tag.trim())
          .filter(Boolean)
      }
    })
    if (response.value) {
      response.value = {
        ...response.value,
        data: updated.data
      }
    }
    noteManagementSuccess.value = '笔记信息已更新。'
    noteEditMode.value = false
    syncNoteMetaForm()
  } catch (saveFailure: any) {
    noteManagementError.value = saveFailure?.data?.message ?? '更新笔记信息失败，请稍后重试。'
  } finally {
    savingNoteMeta.value = false
  }
}

async function deleteNote() {
  if (!note.value) {
    return
  }

  if (!window.confirm('确定要删除这篇学习笔记吗？文件本体和相关批注都会一起删除。')) {
    return
  }

  deletingNote.value = true
  noteManagementError.value = ''
  noteManagementSuccess.value = ''

  try {
    await $fetch<ApiResponse<null>>(`${apiBase}/api/notes/${note.value.id}`, {
      method: 'DELETE'
    })
    await router.push('/notes')
  } catch (deleteFailure: any) {
    noteManagementError.value = deleteFailure?.data?.message ?? '删除学习笔记失败，请稍后重试。'
  } finally {
    deletingNote.value = false
  }
}

function normalizeSelection(value: string) {
  return value.replace(/\s+/g, ' ').trim()
}

function clearSelectionState() {
  selectedQuote.value = ''
  selectedPageNumber.value = null
  selectedAnchor.value = null
  selectedPdfSelectionRects.value = []
  annotationForm.comment = ''
  showAnnotationComposer.value = false
}

function startEditingNoteMeta() {
  syncNoteMetaForm()
  noteManagementError.value = ''
  noteManagementSuccess.value = ''
  noteEditMode.value = true
}

function cancelEditingNoteMeta() {
  noteEditMode.value = false
  noteManagementError.value = ''
}

function startEditingAnnotation(annotation: NoteAnnotation) {
  editingAnnotationId.value = annotation.id
  editingAnnotationForm.comment = annotation.comment
  selectedAnnotationId.value = annotation.id
  pendingFocusTarget.value = null
  annotationError.value = ''
  annotationSuccess.value = ''
}

function cancelEditingAnnotation() {
  editingAnnotationId.value = null
  editingAnnotationForm.comment = ''
}

function handlePreviewSelection() {
  if (!note.value || !isMarkdown.value) {
    return
  }

  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
    return
  }

  const range = selection.getRangeAt(0)
  const commonNode = range.commonAncestorContainer
  const commonElement = commonNode.nodeType === Node.TEXT_NODE ? commonNode.parentElement : commonNode as Element | null

  if (!previewContainer.value || !commonElement || !previewContainer.value.contains(commonElement)) {
    return
  }

  selectedPageNumber.value = null
  selectedAnchor.value = null

  const isCodeSelection = Boolean(commonElement.closest('pre, code'))
  const quote = isCodeSelection
    ? selection.toString().trim()
    : normalizeSelection(selection.toString())
  if (!quote) {
    return
  }

  selectedQuote.value = quote.length > 300 ? quote.slice(0, 300) : quote
  annotationForm.comment = ''
  annotationSuccess.value = ''
  annotationError.value = ''
  showAnnotationComposer.value = true
}

function handlePdfTextSelection() {
  if (!note.value || !isPdf.value || !pdfViewerContainer.value) {
    return
  }

  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
    return
  }

  const range = selection.getRangeAt(0)
  const commonNode = range.commonAncestorContainer
  const commonElement = commonNode.nodeType === Node.TEXT_NODE ? commonNode.parentElement : commonNode as Element | null

  if (!commonElement || !pdfViewerContainer.value.contains(commonElement)) {
    return
  }

  const startPage = selection.anchorNode instanceof Node
    ? (selection.anchorNode.nodeType === Node.TEXT_NODE ? selection.anchorNode.parentElement : selection.anchorNode as Element | null)?.closest('.page')
    : null
  const endPage = selection.focusNode instanceof Node
    ? (selection.focusNode.nodeType === Node.TEXT_NODE ? selection.focusNode.parentElement : selection.focusNode as Element | null)?.closest('.page')
    : null

  if (!(startPage instanceof HTMLElement) || !(endPage instanceof HTMLElement) || startPage !== endPage) {
    return
  }

  const quote = selection.toString().trim()
  if (!quote) {
    return
  }

  const pageNumber = Number(startPage.dataset.pageNumber ?? '')
  if (!Number.isFinite(pageNumber)) {
    return
  }

  const selectionRect = range.getBoundingClientRect()
  const fallbackRect = range.getClientRects()[0]
  const pageRect = startPage.getBoundingClientRect()
  const activeRect = selectionRect.width > 0 && selectionRect.height > 0 ? selectionRect : fallbackRect
  const selectionRects = Array.from(range.getClientRects())
    .filter(rect => rect.width > 0 && rect.height > 0)
    .map((rect) => ({
      x: Math.min(Math.max((rect.left - pageRect.left) / pageRect.width, 0), 1),
      y: Math.min(Math.max((rect.top - pageRect.top) / pageRect.height, 0), 1),
      width: Math.min(rect.width / pageRect.width, 1),
      height: Math.min(rect.height / pageRect.height, 1)
    }))

  selectedPageNumber.value = pageNumber
  selectedAnchor.value = activeRect
    ? {
        pageNumber,
        x: Math.min(Math.max(((activeRect.left + activeRect.width / 2) - pageRect.left) / pageRect.width, 0), 1),
        y: Math.min(Math.max(((activeRect.top + activeRect.height / 2) - pageRect.top) / pageRect.height, 0), 1)
      }
    : null
  selectedPdfSelectionRects.value = selectionRects
  selectedQuote.value = quote.length > 300 ? quote.slice(0, 300) : quote
  annotationForm.comment = ''
  annotationSuccess.value = ''
  annotationError.value = ''
  showAnnotationComposer.value = true
}

function beginPdfAnchorAnnotation(event: MouseEvent, pageNumber: number) {
  const eventTarget = event.target as HTMLElement | null
  const wrapper = eventTarget?.closest('.page') as HTMLElement | null
  if (!wrapper) {
    return
  }

  const selection = window.getSelection()
  if (selection && normalizeSelection(selection.toString())) {
    return
  }

  const rect = wrapper.getBoundingClientRect()
  const x = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1)
  const y = Math.min(Math.max((event.clientY - rect.top) / rect.height, 0), 1)

  selectedPageNumber.value = pageNumber
  selectedAnchor.value = { pageNumber, x, y }
  selectedPdfSelectionRects.value = []
  selectedQuote.value = ''
  annotationForm.comment = ''
  annotationSuccess.value = ''
  annotationError.value = ''
  showAnnotationComposer.value = true
}

async function saveAnnotation() {
  if (!note.value) {
    return
  }

  const hasTextQuote = Boolean(selectedQuote.value)
  const hasPdfAnchor = Boolean(isPdf.value && selectedPageNumber.value && selectedAnchor.value && !selectedQuote.value)

  if (!hasTextQuote && !hasPdfAnchor) {
    annotationError.value = '请先选择需要批注的内容。'
    return
  }

  savingAnnotation.value = true
  annotationError.value = ''
  annotationSuccess.value = ''

  try {
    const annotationResponse = await $fetch<ApiResponse<NoteAnnotation>>(`${apiBase}/api/notes/${note.value.id}/annotations`, {
      method: 'POST',
      body: {
        quotedText: selectedQuote.value,
        comment: annotationForm.comment,
        pageNumber: isPdf.value ? selectedPageNumber.value : null,
        anchorX: isPdf.value ? selectedAnchor.value?.x ?? null : null,
        anchorY: isPdf.value ? selectedAnchor.value?.y ?? null : null
        ,
        selectionRects: isPdf.value ? selectedPdfSelectionRects.value : []
      }
    })

    annotations.value = [...annotations.value, annotationResponse.data]
    annotationSuccess.value = '批注已保存。'
    const selection = window.getSelection()
    selection?.removeAllRanges()
    clearSelectionState()
    await nextTick()
    if (isMarkdown.value) {
      syncMarkdownPreviewHtml()
    }
  } catch (saveFailure: any) {
    annotationError.value = saveFailure?.data?.message ?? '保存批注失败，请稍后重试。'
  } finally {
    savingAnnotation.value = false
  }
}

async function deleteAnnotation(annotation: NoteAnnotation) {
  if (!note.value) {
    return
  }

  if (!window.confirm('确定要删除这条批注吗？删除后将无法恢复。')) {
    return
  }

  deletingAnnotationId.value = annotation.id
  annotationError.value = ''
  annotationSuccess.value = ''

  try {
    await $fetch<ApiResponse<null>>(`${apiBase}/api/notes/${note.value.id}/annotations/${annotation.id}`, {
      method: 'DELETE'
    })

    annotations.value = annotations.value.filter(item => item.id !== annotation.id)
    if (selectedAnnotationId.value === annotation.id) {
      selectedAnnotationId.value = null
    }
    if (editingAnnotationId.value === annotation.id) {
      cancelEditingAnnotation()
    }
    await loadAnnotations()
    annotationSuccess.value = '批注已删除。'
    await nextTick()
    if (isMarkdown.value) {
      syncMarkdownPreviewHtml()
    } else if (isPdf.value) {
      syncPdfAnchorOverlays()
    }
  } catch (deleteFailure: any) {
    const statusCode = deleteFailure?.status ?? deleteFailure?.response?.status
    if (statusCode === 404 || statusCode === 405) {
      annotationError.value = '当前后端似乎还没有加载删除批注接口，先重启本地后端后再试一次。'
    } else {
      annotationError.value = deleteFailure?.data?.message ?? '删除批注失败，请稍后重试。'
    }
  } finally {
    deletingAnnotationId.value = null
  }
}

async function updateAnnotation(annotation: NoteAnnotation) {
  if (!note.value) {
    return
  }

  savingAnnotation.value = true
  annotationError.value = ''
  annotationSuccess.value = ''

  try {
    const annotationResponse = await $fetch<ApiResponse<NoteAnnotation>>(
      `${apiBase}/api/notes/${note.value.id}/annotations/${annotation.id}`,
      {
        method: 'PUT',
        body: {
          quotedText: annotation.quotedText,
          comment: editingAnnotationForm.comment,
          pageNumber: annotation.pageNumber,
          anchorX: annotation.anchorX,
          anchorY: annotation.anchorY,
          selectionRects: annotation.selectionRects
        }
      }
    )

    annotations.value = annotations.value.map(item => item.id === annotation.id ? annotationResponse.data : item)
    selectedAnnotationId.value = annotation.id
    annotationSuccess.value = '批注已更新。'
    cancelEditingAnnotation()
    await nextTick()
    if (isMarkdown.value) {
      syncMarkdownPreviewHtml()
    }
  } catch (updateFailure: any) {
    annotationError.value = updateFailure?.data?.message ?? '更新批注失败，请稍后重试。'
  } finally {
    savingAnnotation.value = false
  }
}

function createAnnotationMark(annotation: NoteAnnotation) {
  const mark = document.createElement('mark')
  mark.className = `note-annotation-highlight${selectedAnnotationId.value === annotation.id ? ' is-active' : ''}`
  mark.dataset.annotationId = String(annotation.id)
  const requiresModifier = Boolean(annotation.pageNumber)
  mark.title = requiresModifier
    ? `${annotation.comment}\nCtrl/Cmd + 单击可跳转到右侧批注`
    : `${annotation.comment}\n单击可跳转到右侧批注`
  mark.addEventListener('click', (event: MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    if (shouldOpenAnnotationFromClick(event, requiresModifier)) {
      focusAnnotationInSidebar(annotation.id)
    }
  })
  return mark
}

function collectHighlightableTextNodes(root: HTMLElement, options?: { textLayerOnly?: boolean }) {
  const textNodes: Text[] = []
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parentElement = node.parentElement
      if (!parentElement) {
        return NodeFilter.FILTER_REJECT
      }
      if (parentElement.closest('mark')) {
        return NodeFilter.FILTER_REJECT
      }
      if (options?.textLayerOnly && !parentElement.closest('.textLayer')) {
        return NodeFilter.FILTER_SKIP
      }
      return node.textContent ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP
    }
  })

  let currentNode = walker.nextNode() as Text | null
  while (currentNode) {
    textNodes.push(currentNode)
    currentNode = walker.nextNode() as Text | null
  }

  return textNodes
}

function findRawRange(root: HTMLElement, quote: string, options?: { textLayerOnly?: boolean }) {
  const textNodes = collectHighlightableTextNodes(root, options)
  if (!textNodes.length) {
    return null
  }

  let fullText = ''
  const nodeRanges = textNodes.map((node) => {
    const start = fullText.length
    fullText += node.textContent ?? ''
    return {
      node,
      start,
      end: fullText.length
    }
  })

  const startIndex = fullText.indexOf(quote)
  if (startIndex < 0) {
    return null
  }

  const endIndex = startIndex + quote.length
  const startEntry = nodeRanges.find(entry => startIndex >= entry.start && startIndex < entry.end)
  const endEntry = nodeRanges.find(entry => endIndex > entry.start && endIndex <= entry.end)

  if (!startEntry || !endEntry) {
    return null
  }

  const range = document.createRange()
  range.setStart(startEntry.node, startIndex - startEntry.start)
  range.setEnd(endEntry.node, endIndex - endEntry.start)
  return range
}

function findRawRangeNearAnchor(
  root: HTMLElement,
  quote: string,
  anchor: { x: number, y: number },
  options?: { textLayerOnly?: boolean }
) {
  const textNodes = collectHighlightableTextNodes(root, options)
  if (!textNodes.length) {
    return null
  }

  let fullText = ''
  const nodeRanges = textNodes.map((node) => {
    const start = fullText.length
    fullText += node.textContent ?? ''
    return {
      node,
      start,
      end: fullText.length
    }
  })

  const pageRect = root.closest('.page')?.getBoundingClientRect()
  if (!pageRect) {
    return findRawRange(root, quote, options)
  }

  const targetX = pageRect.left + pageRect.width * anchor.x
  const targetY = pageRect.top + pageRect.height * anchor.y
  const matches: Array<{ range: Range, distance: number }> = []

  let searchStart = 0
  while (searchStart < fullText.length) {
    const startIndex = fullText.indexOf(quote, searchStart)
    if (startIndex < 0) {
      break
    }

    const endIndex = startIndex + quote.length
    const startEntry = nodeRanges.find(entry => startIndex >= entry.start && startIndex < entry.end)
    const endEntry = nodeRanges.find(entry => endIndex > entry.start && endIndex <= entry.end)

    if (startEntry && endEntry) {
      const range = document.createRange()
      range.setStart(startEntry.node, startIndex - startEntry.start)
      range.setEnd(endEntry.node, endIndex - endEntry.start)
      const rect = range.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2
      matches.push({
        range,
        distance: Math.hypot(centerX - targetX, centerY - targetY)
      })
    }

    searchStart = startIndex + quote.length
  }

  if (!matches.length) {
    return null
  }

  matches.sort((left, right) => left.distance - right.distance)
  return matches[0]?.range ?? null
}

function wrapAnnotationRange(range: Range, annotation: NoteAnnotation) {
  const mark = createAnnotationMark(annotation)
  try {
    range.surroundContents(mark)
  } catch {
    const contents = range.extractContents()
    mark.appendChild(contents)
    range.insertNode(mark)
  }
}

function clearAnnotationMarks(root: ParentNode) {
  const marks = root.querySelectorAll('mark.note-annotation-highlight')
  for (const mark of marks) {
    mark.replaceWith(document.createTextNode(mark.textContent ?? ''))
  }
  if (root instanceof HTMLElement) {
    root.normalize()
  }
}

function applyAnnotationHighlight(root: HTMLElement, annotation: NoteAnnotation, options?: { textLayerOnly?: boolean }) {
  const rawQuote = annotation.quotedText.trim()
  const quote = normalizeSelection(annotation.quotedText)
  if (!quote) {
    return
  }

  if (rawQuote) {
    const rawRange = annotation.anchorX !== null && annotation.anchorY !== null
      ? findRawRangeNearAnchor(root, rawQuote, { x: annotation.anchorX, y: annotation.anchorY }, options)
      : findRawRange(root, rawQuote, options)
    if (rawRange) {
      wrapAnnotationRange(rawRange, annotation)
      return
    }
  }

  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parentElement = node.parentElement
      if (!parentElement) {
        return NodeFilter.FILTER_REJECT
      }
      if (parentElement.closest('mark')) {
        return NodeFilter.FILTER_REJECT
      }
      if (options?.textLayerOnly && !parentElement.closest('.textLayer')) {
        return NodeFilter.FILTER_SKIP
      }
      return normalizeSelection(node.textContent ?? '').includes(quote)
        ? NodeFilter.FILTER_ACCEPT
        : NodeFilter.FILTER_SKIP
    }
  })

  let currentNode = walker.nextNode() as Text | null
  while (currentNode) {
    const originalText = currentNode.textContent ?? ''
    const collapsedText = normalizeSelection(originalText)
    const collapsedIndex = collapsedText.indexOf(quote)

    if (collapsedIndex >= 0) {
      const start = originalText.indexOf(quote)
      if (start >= 0) {
        const end = start + quote.length
        const range = document.createRange()
        range.setStart(currentNode, start)
        range.setEnd(currentNode, end)
        wrapAnnotationRange(range, annotation)
        return
      }
    }

    currentNode = walker.nextNode() as Text | null
  }
}

function syncMarkdownPreviewHtml() {
  if (!previewContainer.value) {
    return
  }

  previewContainer.value.innerHTML = renderedPreview.value

  if (!isMarkdown.value) {
    return
  }

  for (const annotation of annotations.value) {
    applyAnnotationHighlight(previewContainer.value, annotation)
  }
}

function syncPdfAnchorOverlays() {
  if (!pdfViewerContainer.value) {
    return
  }

  const pageElements = pdfViewerContainer.value.querySelectorAll<HTMLElement>('.page')
  for (const pageElement of pageElements) {
    let highlightLayer = pageElement.querySelector<HTMLElement>('.pdf-page-highlight-layer')
    if (!highlightLayer) {
      highlightLayer = document.createElement('div')
      highlightLayer.className = 'pdf-page-highlight-layer'
      pageElement.appendChild(highlightLayer)
    }
    highlightLayer.innerHTML = ''

    const textLayer = pageElement.querySelector<HTMLElement>('.textLayer')
    if (textLayer) {
      clearAnnotationMarks(textLayer)

      const pageAnnotations = annotations.value.filter(annotation =>
        annotation.pageNumber === Number(pageElement.dataset.pageNumber ?? '')
        && normalizeSelection(annotation.quotedText)
      )

      for (const annotation of pageAnnotations) {
        if (annotation.selectionRects?.length) {
          for (const rect of annotation.selectionRects) {
            const segment = document.createElement('button')
            segment.type = 'button'
            segment.className = `pdf-selection-highlight${selectedAnnotationId.value === annotation.id ? ' active' : ''}`
            segment.dataset.annotationId = String(annotation.id)
            segment.dataset.pdfOverlayInteractive = 'true'
            segment.dataset.shortcutHint = 'Ctrl+点击查看批注'
            segment.style.left = `${rect.x * 100}%`
            segment.style.top = `${rect.y * 100}%`
            segment.style.width = `${rect.width * 100}%`
            segment.style.height = `${rect.height * 100}%`
            segment.addEventListener('mousedown', (event: MouseEvent) => {
              event.stopPropagation()
            })
            segment.addEventListener('click', (event: MouseEvent) => {
              event.preventDefault()
              event.stopPropagation()
              if (shouldOpenAnnotationFromClick(event)) {
                focusAnnotationInSidebar(annotation.id)
              }
            })
            highlightLayer.appendChild(segment)
          }
          continue
        }

        applyAnnotationHighlight(textLayer, annotation, { textLayerOnly: true })
      }
    }

    let layer = pageElement.querySelector<HTMLElement>('.pdf-page-anchor-layer')
    if (!layer) {
      layer = document.createElement('div')
      layer.className = 'pdf-page-anchor-layer'
      pageElement.appendChild(layer)
    }
    layer.innerHTML = ''

    const pageNumber = Number(pageElement.dataset.pageNumber ?? '')
    if (!Number.isFinite(pageNumber)) {
      continue
    }

    const pageAnnotations = annotations.value.filter(annotation =>
      annotation.pageNumber === pageNumber
      && annotation.anchorX !== null
      && annotation.anchorY !== null
      && (!normalizeSelection(annotation.quotedText) || annotation.quotedText.trim() === 'PDF 锚点批注')
    )

    for (const annotation of pageAnnotations) {
      const anchor = document.createElement('button')
      anchor.type = 'button'
      anchor.className = `pdf-anchor${selectedAnnotationId.value === annotation.id ? ' active' : ''}`
      anchor.dataset.annotationAnchorId = String(annotation.id)
      anchor.dataset.pdfOverlayInteractive = 'true'
      anchor.dataset.shortcutHint = 'Ctrl+点击查看批注'
      anchor.style.left = `${(annotation.anchorX ?? 0.5) * 100}%`
      anchor.style.top = `${(annotation.anchorY ?? 0.5) * 100}%`
      anchor.textContent = String(annotations.value.findIndex(item => item.id === annotation.id) + 1)
      anchor.addEventListener('mousedown', (event: MouseEvent) => {
        event.stopPropagation()
      })
      anchor.addEventListener('click', (event: MouseEvent) => {
        event.preventDefault()
        event.stopPropagation()
        if (shouldOpenAnnotationFromClick(event)) {
          focusAnnotationInSidebar(annotation.id)
        }
      })
      layer.appendChild(anchor)
    }

    if (selectedAnchor.value?.pageNumber === pageNumber) {
      const pending = document.createElement('span')
      pending.className = 'pdf-anchor pending'
      pending.style.left = `${selectedAnchor.value.x * 100}%`
      pending.style.top = `${selectedAnchor.value.y * 100}%`
      layer.appendChild(pending)
    }
  }
}

async function setupPdfViewer() {
  if (!import.meta.client || !note.value || !isPdf.value) {
    return
  }

  pdfRenderLoading.value = true
  pdfRenderError.value = ''

  try {
    const [pdfjs, workerUrl] = await Promise.all([
      import('pdfjs-dist/legacy/build/pdf.mjs'),
      import('pdfjs-dist/legacy/build/pdf.worker.min.mjs?url')
    ])

    ;(globalThis as typeof globalThis & { pdfjsLib?: typeof pdfjs }).pdfjsLib = pdfjs

    const pdfViewerModule = await import('pdfjs-dist/legacy/web/pdf_viewer.mjs')

    pdfjs.GlobalWorkerOptions.workerSrc = workerUrl.default

    const pdfResponse = await fetch(`${apiBase}${note.value.viewUrl}`)
    if (!pdfResponse.ok) {
      throw new Error('PDF_FETCH_FAILED')
    }
    const pdfData = new Uint8Array(await pdfResponse.arrayBuffer())

    pdfLoadingTask.value = pdfjs.getDocument({
      data: pdfData,
      useWorkerFetch: false,
      isEvalSupported: false,
      enableXfa: false
    })
    pdfDocumentHandle.value = await pdfLoadingTask.value.promise

    if (!pdfViewerContainer.value || !pdfViewerPages.value) {
      throw new Error('PDF_VIEWER_CONTAINER_MISSING')
    }

    pdfViewerPages.value.innerHTML = ''

    pdfEventBus.value = new pdfViewerModule.EventBus()
    pdfLinkService.value = new pdfViewerModule.PDFLinkService({
      eventBus: pdfEventBus.value
    })
    pdfViewerInstance.value = new pdfViewerModule.PDFViewer({
      container: pdfViewerContainer.value,
      viewer: pdfViewerPages.value,
      eventBus: pdfEventBus.value,
      linkService: pdfLinkService.value,
      textLayerMode: 1,
      annotationMode: 0
    })

    pdfLinkService.value.setViewer(pdfViewerInstance.value)
    pdfViewerInstance.value.setDocument(pdfDocumentHandle.value)
    pdfLinkService.value.setDocument(pdfDocumentHandle.value)

    pdfEventBus.value.on('pagesinit', () => {
      if (pdfViewerInstance.value) {
        pdfViewerInstance.value.currentScaleValue = 'page-width'
      }
    })

    pdfEventBus.value.on('pagesloaded', () => {
      syncPdfAnchorOverlays()
    })

    pdfEventBus.value.on('pagerendered', () => {
      syncPdfAnchorOverlays()
    })

    await nextTick()
    syncPdfAnchorOverlays()
  } catch (renderFailure) {
    console.error('PDF render failed:', renderFailure)
    const detail = renderFailure instanceof Error ? renderFailure.message : ''
    pdfRenderError.value = detail
      ? `当前无法加载 PDF 页面预览：${detail}`
      : '当前无法加载 PDF 页面预览，你仍然可以直接打开原文件。'
  } finally {
    pdfRenderLoading.value = false
  }
}

watch(selectedAnnotationId, async () => {
  await nextTick()

  if (isMarkdown.value) {
    syncMarkdownPreviewHtml()
  } else if (isPdf.value) {
    syncPdfAnchorOverlays()
  }

  await nextTick()

  if (!selectedAnnotationId.value) {
    return
  }

  if (pendingFocusTarget.value === 'sidebar') {
    const card = annotationSidebarRef.value?.querySelector(`[data-annotation-card-id="${selectedAnnotationId.value}"]`)
    if (card instanceof HTMLElement) {
      card.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  if (pendingFocusTarget.value === 'preview') {
    if (isPdf.value && selectedAnnotationId.value) {
      scrollToPdfAnnotation(selectedAnnotationId.value)
    } else {
      const mark = previewContainer.value?.querySelector(`[data-annotation-id="${selectedAnnotationId.value}"]`)
      if (mark instanceof HTMLElement) {
        mark.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
      }
    }
  }

  pendingFocusTarget.value = null
})

if (note.value && isTextPreviewable.value) {
  try {
    noteText.value = await $fetch<string>(`${apiBase}${note.value.viewUrl}`, {
      responseType: 'text'
    })
  } catch {
    previewError.value = '当前无法读取文本预览，你仍然可以直接打开或下载原文件。'
  }
}

if (note.value && supportsAnnotations.value) {
  await loadAnnotations()
}

watch([renderedPreview, annotations], async () => {
  if (!isMarkdown.value) {
    return
  }
  await nextTick()
  syncMarkdownPreviewHtml()
})

watch(annotations, async () => {
  if (isMarkdown.value) {
    await nextTick()
    syncMarkdownPreviewHtml()
  } else if (isPdf.value) {
    await nextTick()
    syncPdfAnchorOverlays()
  }
}, { deep: true })

watch(selectedAnchor, async () => {
  if (!isPdf.value) {
    return
  }
  await nextTick()
  syncPdfAnchorOverlays()
}, { deep: true })

onMounted(async () => {
  syncNoteMetaForm()
  if (isMarkdown.value) {
    syncMarkdownPreviewHtml()
    return
  }

  if (isPdf.value) {
    await nextTick()
    await setupPdfViewer()
  }
})

onBeforeUnmount(async () => {
  try {
    pdfViewerInstance.value?.cleanup?.()
    pdfViewerInstance.value?.setDocument?.(null)
    pdfLinkService.value?.setDocument?.(null)
    await pdfLoadingTask.value?.destroy?.()
    await pdfDocumentHandle.value?.destroy?.()
  } catch {
    // Best-effort cleanup for pdf.js resources.
  }
})
</script>

<template>
  <div class="page-stack note-detail-page">
    <section v-if="note" class="page-hero">
      <p class="eyebrow">Study Note Detail</p>
      <h1>{{ note.title }}</h1>
      <p>
        {{ note.description || '这份学习笔记当前还没有补充说明，你可以先直接查看原文件内容。' }}
      </p>

      <div class="note-detail-meta">
        <span>{{ note.originalFilename }}</span>
        <span>{{ note.extension.toUpperCase() }}</span>
        <span>{{ formatFileSize(note.size) }}</span>
        <span>{{ formatDate(note.uploadedAt) }}</span>
      </div>

      <div v-if="note.tags.length" class="note-tag-list">
        <span v-for="tag in note.tags" :key="tag" class="note-tag">{{ tag }}</span>
      </div>

      <div class="content-card note-management-card">
        <div class="note-management-head">
          <div>
            <p class="eyebrow">Management</p>
            <h3>笔记信息管理</h3>
          </div>
          <div class="note-management-actions">
            <button
              v-if="!noteEditMode"
              class="button secondary"
              type="button"
              @click="startEditingNoteMeta"
            >
              编辑信息
            </button>
            <button
              class="button secondary danger-button"
              type="button"
              :disabled="deletingNote"
              @click="deleteNote"
            >
              {{ deletingNote ? '删除中...' : '删除整篇笔记' }}
            </button>
          </div>
        </div>

        <div v-if="noteEditMode" class="note-management-form">
          <label class="note-management-field">
            <span>标题</span>
            <input v-model="noteMetaForm.title" class="note-management-input" maxlength="80" />
          </label>
          <label class="note-management-field">
            <span>描述</span>
            <textarea
              v-model="noteMetaForm.description"
              class="note-management-textarea"
              rows="3"
              maxlength="240"
            />
          </label>
          <label class="note-management-field">
            <span>标签</span>
            <input
              v-model="noteMetaForm.tags"
              class="note-management-input"
              placeholder="用英文逗号分隔，如：图论, 动态规划, 总结"
            />
          </label>
          <div class="note-management-actions">
            <button class="button primary" type="button" :disabled="savingNoteMeta" @click="saveNoteMeta">
              {{ savingNoteMeta ? '保存中...' : '保存笔记信息' }}
            </button>
            <button class="button secondary" type="button" @click="cancelEditingNoteMeta">
              取消
            </button>
          </div>
        </div>

        <div v-else class="note-management-summary">
          <p><strong>当前标题：</strong>{{ note.title }}</p>
          <p><strong>当前描述：</strong>{{ note.description || '暂无描述' }}</p>
          <p><strong>当前标签：</strong>{{ note.tags.length ? note.tags.join(' / ') : '暂无标签' }}</p>
        </div>

        <p v-if="noteManagementError" class="annotation-feedback error">{{ noteManagementError }}</p>
        <p v-else-if="noteManagementSuccess" class="annotation-feedback success">{{ noteManagementSuccess }}</p>
      </div>

      <div class="hero-actions">
        <a class="button primary" :href="`${apiBase}${note.viewUrl}`" target="_blank" rel="noreferrer">
          打开原文件
        </a>
        <a class="button secondary" :href="`${apiBase}${note.downloadUrl}`" target="_blank" rel="noreferrer">
          下载文件
        </a>
        <NuxtLink class="button secondary" to="/notes">返回学习笔记页</NuxtLink>
      </div>
    </section>

    <section v-if="note && supportsAnnotations" class="note-annotation-layout">
      <article class="content-card note-preview-card">
        <p class="eyebrow">{{ isPdf ? 'PDF Preview' : 'Markdown Preview' }}</p>
        <h2>{{ isPdf ? '可批注 PDF 预览' : '渲染预览' }}</h2>
        <p class="preview-copy">
          {{ isPdf
            ? '左侧保留原始 PDF 预览，下方同时支持两种 PDF 批注方式：鼠标划选文本直接高亮批注，或点击页面空白处落锚点批注。'
            : '鼠标左键选中需要批注的内容，右侧会弹出批注编辑框。保存后的批注会长期保留并在正文里高亮显示。'
          }}
        </p>

        <p v-if="previewError" class="preview-copy">{{ previewError }}</p>

        <div
          v-if="isMarkdown && !previewError"
          ref="previewContainer"
          class="note-rendered-preview selectable-preview"
          @mouseup="handlePreviewSelection"
        />

        <div
          v-else-if="isPdf"
          class="pdf-preview-stack"
        >
          <iframe class="note-pdf-frame" :src="`${apiBase}${note.viewUrl}`" :title="note.title" />
          <div class="pdf-preview-toolbar">
            <div>
              <strong>PDF 锚点模式</strong>
              <span>上方是稳定的原始预览，下方支持划选文本高亮，也支持点击空白处添加锚点说明</span>
            </div>
            <a class="button secondary" :href="`${apiBase}${note.viewUrl}`" target="_blank" rel="noreferrer">
              打开原 PDF
            </a>
          </div>
          <p v-if="pdfRenderError" class="preview-copy">{{ pdfRenderError }}</p>
          <div v-else class="pdfjs-shell" :class="{ loading: pdfRenderLoading }">
            <div
              ref="pdfViewerContainer"
              class="pdfjs-scroll-container"
              @mouseup="handlePdfTextSelection"
              @click="
                (() => {
                  const target = $event.target as HTMLElement | null
                  if (target?.closest('[data-pdf-overlay-interactive], mark.note-annotation-highlight')) return
                  const page = target?.closest('.page') as HTMLElement | null
                  if (!page) return
                  const pageNumber = Number(page.dataset.pageNumber ?? '')
                  if (!Number.isFinite(pageNumber)) return
                  beginPdfAnchorAnnotation($event as MouseEvent, pageNumber)
                })()
              "
            >
              <div v-if="pdfRenderLoading" class="pdfjs-loading-copy">正在加载 PDF 页面预览...</div>
              <div ref="pdfViewerPages" class="pdfViewer" />
            </div>
          </div>
        </div>
      </article>

      <aside ref="annotationSidebarRef" class="content-card annotation-sidebar">
        <p class="eyebrow">Annotations</p>
        <h2>文档批注</h2>

        <div v-if="showAnnotationComposer" class="annotation-composer">
          <p class="annotation-section-title">新建批注</p>
          <p v-if="selectedPageNumber" class="annotation-location-chip">PDF 第 {{ selectedPageNumber }} 页</p>
          <blockquote class="annotation-quote">
            {{ selectedQuote || (isPdf ? '已在当前 PDF 页面落下批注锚点。' : '') }}
          </blockquote>
          <textarea
            v-model="annotationForm.comment"
            class="annotation-textarea"
            rows="5"
            maxlength="1000"
            placeholder="输入你对这段内容的理解、补充或问题。"
          />
          <div class="annotation-actions">
            <button class="button primary" :disabled="savingAnnotation" @click="saveAnnotation">
              {{ savingAnnotation ? '保存中...' : '保存批注' }}
            </button>
            <button class="button secondary" type="button" @click="clearSelectionState">
              取消
            </button>
          </div>
        </div>

        <div v-else class="annotation-empty-box">
          <p>{{ isPdf ? '先在左侧 PDF 页面上点击一个位置落锚点，这里就会出现批注框。' : '先在左侧选中一段文字，这里就会出现批注框。' }}</p>
        </div>

        <p v-if="annotationError" class="annotation-feedback error">{{ annotationError }}</p>
        <p v-else-if="annotationSuccess" class="annotation-feedback success">{{ annotationSuccess }}</p>

        <div class="annotation-list-head">
          <strong>已保存批注</strong>
          <span>{{ annotations.length }} 条</span>
        </div>

        <div v-if="annotationLoading" class="annotation-state-copy">正在加载批注...</div>
        <div v-else-if="!annotations.length" class="annotation-state-copy">当前还没有保存的批注。</div>
        <div v-else class="annotation-list">
          <article
            v-for="annotation in annotations"
            :key="annotation.id"
            class="annotation-card"
            :class="{ active: selectedAnnotationId === annotation.id }"
            :data-annotation-card-id="annotation.id"
            @click="
              pendingFocusTarget = 'preview';
              selectedAnnotationId = annotation.id
            "
          >
            <div class="annotation-card-head">
              <p class="annotation-location-chip">{{ annotationLocationLabel(annotation) }}</p>
            </div>
            <blockquote class="annotation-quote">{{ annotationDisplayQuote(annotation) }}</blockquote>
            <div v-if="editingAnnotationId === annotation.id" class="annotation-card-editor">
              <textarea
                v-model="editingAnnotationForm.comment"
                class="annotation-textarea annotation-edit-textarea"
                rows="6"
                maxlength="1000"
                placeholder="继续完善这条批注内容。"
                @click.stop
              />
              <div class="annotation-actions annotation-inline-actions">
                <button
                  class="button primary"
                  :disabled="savingAnnotation"
                  @click.stop="updateAnnotation(annotation)"
                >
                  {{ savingAnnotation ? '保存中...' : '保存修改' }}
                </button>
                <button class="button secondary" type="button" @click.stop="cancelEditingAnnotation">
                  取消
                </button>
                <button
                  class="button secondary annotation-card-button danger"
                  type="button"
                  :disabled="deletingAnnotationId === annotation.id"
                  @click.stop="deleteAnnotation(annotation)"
                >
                  {{ deletingAnnotationId === annotation.id ? '删除中...' : '删除批注' }}
                </button>
              </div>
            </div>
            <div v-else class="annotation-comment markdown-comment" v-html="renderAnnotationComment(annotation.comment)" />
            <div class="annotation-card-actions">
              <button
                v-if="editingAnnotationId !== annotation.id"
                class="button secondary annotation-card-button"
                type="button"
                @click.stop="startEditingAnnotation(annotation)"
              >
                编辑批注
              </button>
              <button
                v-if="editingAnnotationId !== annotation.id"
                class="button secondary annotation-card-button danger"
                type="button"
                :disabled="deletingAnnotationId === annotation.id"
                @click.stop="deleteAnnotation(annotation)"
              >
                {{ deletingAnnotationId === annotation.id ? '删除中...' : '删除批注' }}
              </button>
            </div>
            <span class="annotation-time">{{ formatAnnotationDate(annotation.createdAt) }}</span>
          </article>
        </div>
      </aside>
    </section>

    <section v-else-if="note && isTextPreviewable" class="content-card note-preview-card">
      <p class="eyebrow">Code Preview</p>
      <h2>高亮预览</h2>
      <p v-if="previewError" class="preview-copy">{{ previewError }}</p>
      <div
        v-else
        ref="previewContainer"
        class="note-rendered-preview"
      />
    </section>

    <section v-else-if="note" class="content-card note-preview-card">
      <p class="eyebrow">Preview</p>
      <h2>当前文件类型不提供内嵌预览</h2>
      <p class="preview-copy">你可以直接打开原文件，或者下载到本地继续查看。</p>
    </section>

    <section v-else class="content-card note-preview-card">
      <p class="eyebrow">Unavailable</p>
      <h2>暂时无法读取这份学习笔记</h2>
      <p class="preview-copy">
        {{ error?.data?.message || '可能是后端服务未启动，或当前笔记不存在。' }}
      </p>
      <NuxtLink class="button secondary" to="/notes">返回学习笔记页</NuxtLink>
    </section>
  </div>
</template>

<style scoped>
.note-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--muted);
  font-size: 14px;
}

.note-detail-page {
  width: min(1480px, 100%);
  margin: 0 auto;
}

.note-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.note-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--muted);
  font-size: 13px;
}

.note-preview-card {
  display: grid;
  gap: 18px;
  min-width: 0;
  overflow: hidden;
}

.note-management-card,
.note-management-head,
.note-management-actions,
.note-management-form,
.note-management-summary {
  display: grid;
  gap: 12px;
}

.note-management-head {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 18px;
}

.note-management-field {
  display: grid;
  gap: 8px;
  color: var(--muted);
}

.note-management-input,
.note-management-textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  padding: 12px 14px;
  font: inherit;
}

.note-management-textarea {
  resize: vertical;
}

.note-management-summary p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.danger-button {
  border-color: rgba(255, 107, 107, 0.28);
  color: #ffd0d0;
}

.danger-button:hover:not(:disabled) {
  border-color: rgba(255, 107, 107, 0.46);
  background: rgba(255, 107, 107, 0.08);
}

.note-annotation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(360px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.note-preview-card h2 {
  margin: 0;
  font-size: 32px;
}

.note-annotation-layout > .note-preview-card,
.note-annotation-layout > .annotation-sidebar {
  max-height: calc(100vh - 118px);
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.pdf-preview-stack {
  display: grid;
  gap: 18px;
}

.note-pdf-frame {
  width: 100%;
  min-height: 78vh;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.pdf-preview-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  padding: 18px 20px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.pdf-preview-toolbar > div {
  display: grid;
  gap: 6px;
}

.pdf-preview-toolbar strong {
  font-size: 18px;
}

.pdf-preview-toolbar span,
.pdf-annotation-page-label {
  color: var(--muted);
}

.pdfjs-shell {
  position: relative;
  min-height: 60vh;
  max-height: 110vh;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background:
    radial-gradient(circle at top, rgba(107, 211, 255, 0.06), transparent 42%),
    rgba(8, 17, 31, 0.34);
}

.pdfjs-shell.loading {
  min-height: 60vh;
}

.pdfjs-scroll-container {
  position: absolute;
  inset: 20px;
  overflow: auto;
}

.pdfjs-loading-copy {
  position: sticky;
  top: 0;
  z-index: 12;
  padding: 8px 0 18px;
  color: var(--muted);
  line-height: 1.8;
}

.pdfjs-shell :deep(.pdfViewer) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.pdfjs-shell :deep(.page) {
  position: relative;
  margin: 0 auto !important;
  box-shadow:
    0 18px 44px rgba(0, 0, 0, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  cursor: crosshair;
}

.pdfjs-shell :deep(.page::before) {
  content: '第 ' attr(data-page-number) ' 页';
  position: absolute;
  top: -34px;
  left: 0;
  color: var(--muted);
  font-size: 13px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.pdfjs-shell :deep(.pdf-page-anchor-layer) {
  position: absolute;
  inset: 0;
  z-index: 8;
  pointer-events: none;
}

.pdfjs-shell :deep(.pdf-page-highlight-layer) {
  position: absolute;
  inset: 0;
  z-index: 7;
  pointer-events: none;
}

.pdfjs-shell :deep(.pdf-selection-highlight) {
  position: absolute;
  border: 0;
  padding: 0;
  border-radius: 3px;
  background: rgba(255, 211, 107, 0.4);
  box-shadow: 0 0 0 1px rgba(255, 211, 107, 0.18);
  pointer-events: auto;
  cursor: pointer;
}

.pdfjs-shell :deep(.pdf-selection-highlight::after),
.pdfjs-shell :deep(.pdf-anchor::before) {
  content: attr(data-shortcut-hint);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 10px);
  transform: translate(-50%, 6px);
  padding: 6px 10px;
  border: 1px solid rgba(107, 211, 255, 0.24);
  border-radius: 10px;
  background: rgba(8, 17, 31, 0.92);
  color: #dff6ff;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease, transform 0.16s ease;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.26);
}

.pdfjs-shell :deep(.pdf-selection-highlight:hover::after),
.pdfjs-shell :deep(.pdf-selection-highlight:focus-visible::after),
.pdfjs-shell :deep(.pdf-anchor:hover::before),
.pdfjs-shell :deep(.pdf-anchor:focus-visible::before) {
  opacity: 1;
  transform: translate(-50%, 0);
}

.pdfjs-shell :deep(.pdf-selection-highlight.active) {
  background: rgba(107, 211, 255, 0.4);
  box-shadow: 0 0 0 1px rgba(107, 211, 255, 0.28);
}

.pdfjs-shell :deep(.textLayer) {
  z-index: 6;
}

.pdfjs-shell :deep(.textLayer mark.note-annotation-highlight) {
  background: rgba(255, 211, 107, 0.42);
  color: inherit;
  border-radius: 4px;
  box-shadow: 0 0 0 1px rgba(255, 211, 107, 0.2);
}

.pdfjs-shell :deep(.textLayer mark.note-annotation-highlight.is-active) {
  background: rgba(107, 211, 255, 0.44);
  box-shadow: 0 0 0 1px rgba(107, 211, 255, 0.32);
}

.pdfjs-shell :deep(.pdf-anchor) {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  border: 2px solid rgba(255, 255, 255, 0.92);
  border-radius: 999px;
  background: linear-gradient(180deg, #6bd3ff, #5ea8ff);
  color: #08111f;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(12, 20, 35, 0.28);
  pointer-events: auto;
  cursor: pointer;
}

.pdfjs-shell :deep(.pdf-anchor::after) {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: 999px;
  border: 1px solid rgba(107, 211, 255, 0.25);
}

.pdfjs-shell :deep(.pdf-anchor.active) {
  background: linear-gradient(180deg, #ffd36b, #ffb85e);
}

.pdfjs-shell :deep(.pdf-anchor.pending) {
  display: block;
  background: linear-gradient(180deg, #99efc5, #6bd3ff);
  pointer-events: none;
}

.pdfjs-shell :deep(.pdf-anchor.pending::before) {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 999px;
  background: rgba(8, 17, 31, 0.22);
}

.note-rendered-preview {
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  min-width: 0;
  overflow: hidden;
}

.selectable-preview {
  user-select: text;
}

.preview-copy {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.annotation-sidebar,
.annotation-composer,
.annotation-list {
  display: grid;
  gap: 16px;
}

.annotation-sidebar {
  position: sticky;
  top: 104px;
  align-self: start;
}

.annotation-sidebar h2 {
  margin: 0;
  font-size: 32px;
}

.annotation-empty-box,
.annotation-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.annotation-card {
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.annotation-card.active {
  border-color: rgba(107, 211, 255, 0.4);
  background: rgba(107, 211, 255, 0.08);
}

.annotation-card:hover {
  transform: translateY(-2px);
}

.annotation-empty-box p,
.annotation-comment,
.annotation-state-copy,
.annotation-time,
.annotation-list-head span,
.annotation-section-title {
  margin: 0;
  color: var(--muted);
}

.annotation-list-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.annotation-card-head {
  display: flex;
  justify-content: flex-end;
}

.annotation-location-chip {
  margin: 0;
  color: var(--secondary);
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.annotation-quote {
  margin: 0;
  padding-left: 14px;
  border-left: 3px solid rgba(107, 211, 255, 0.45);
  color: var(--text);
  line-height: 1.8;
}

.annotation-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  font: inherit;
  resize: vertical;
  min-height: 120px;
}

.annotation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.annotation-inline-actions {
  margin-top: 4px;
}

.annotation-actions .button {
  min-width: 140px;
}

.annotation-card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.annotation-card-button {
  min-width: 112px;
  border-color: rgba(107, 211, 255, 0.26);
  color: #dff6ff;
  background: rgba(107, 211, 255, 0.08);
}

.annotation-card-button:hover:not(:disabled) {
  border-color: rgba(107, 211, 255, 0.46);
  background: rgba(107, 211, 255, 0.14);
}

.annotation-card-button.danger {
  border-color: rgba(255, 107, 107, 0.26);
  color: #ffc0c0;
}

.annotation-card-button.danger:hover:not(:disabled) {
  border-color: rgba(255, 107, 107, 0.44);
  background: rgba(255, 107, 107, 0.08);
}

.annotation-card-editor {
  display: grid;
  gap: 12px;
}

.annotation-edit-textarea {
  min-height: 140px;
}

.annotation-feedback {
  margin: 0;
  font-size: 14px;
}

.annotation-feedback.error {
  color: #ff9d9d;
}

.annotation-feedback.success {
  color: #99efc5;
}

.annotation-comment {
  line-height: 1.8;
}

.annotation-time {
  font-size: 13px;
}

.note-rendered-preview :deep(h2),
.note-rendered-preview :deep(h3),
.note-rendered-preview :deep(h4) {
  margin: 0 0 14px;
}

.note-rendered-preview :deep(p),
.note-rendered-preview :deep(li),
.note-rendered-preview :deep(blockquote),
.note-rendered-preview :deep(a) {
  line-height: 1.9;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.note-rendered-preview :deep(p),
.note-rendered-preview :deep(ul),
.note-rendered-preview :deep(blockquote),
.note-rendered-preview :deep(pre) {
  margin: 0 0 16px;
}

.note-rendered-preview :deep(ul) {
  padding-left: 20px;
}

.note-rendered-preview :deep(blockquote) {
  padding-left: 16px;
  border-left: 3px solid rgba(107, 211, 255, 0.4);
  color: var(--muted);
}

.note-rendered-preview :deep(code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  font-family: Consolas, "Courier New", monospace;
}

.note-rendered-preview :deep(.markdown-code-block) {
  max-width: 100%;
  overflow-x: auto;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(5, 16, 30, 0.72);
  box-sizing: border-box;
}

.note-rendered-preview :deep(.markdown-code-block code) {
  display: block;
  min-width: 0;
  padding: 0;
  background: transparent;
  white-space: pre;
  line-height: 1.7;
}

.note-rendered-preview :deep(.token-comment) {
  color: #7f93ad;
}

.note-rendered-preview :deep(.token-keyword) {
  color: #6bd3ff;
}

.note-rendered-preview :deep(.token-string) {
  color: #ffd36b;
}

.note-rendered-preview :deep(.token-number) {
  color: #99efc5;
}

.note-rendered-preview :deep(.note-annotation-highlight) {
  padding: 0 2px;
  border-radius: 4px;
  background: rgba(255, 211, 107, 0.32);
  color: inherit;
  cursor: pointer;
}

.note-rendered-preview :deep(.note-annotation-highlight.is-active) {
  background: rgba(107, 211, 255, 0.32);
}

.markdown-comment :deep(p),
.markdown-comment :deep(ul),
.markdown-comment :deep(blockquote),
.markdown-comment :deep(pre) {
  margin: 0 0 12px;
}

.markdown-comment :deep(p),
.markdown-comment :deep(li),
.markdown-comment :deep(blockquote),
.markdown-comment :deep(a) {
  line-height: 1.8;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-comment :deep(ul) {
  padding-left: 20px;
}

.markdown-comment :deep(code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  font-family: Consolas, "Courier New", monospace;
}

.note-rendered-preview :deep(.math-inline),
.note-rendered-preview :deep(.markdown-math-block),
.markdown-comment :deep(.math-inline),
.markdown-comment :deep(.markdown-math-block) {
  display: inline-flex;
  max-width: 100%;
  padding: 4px 8px;
  border-radius: 10px;
  background: rgba(107, 211, 255, 0.08);
  overflow-x: auto;
}

.note-rendered-preview :deep(.markdown-math-block),
.markdown-comment :deep(.markdown-math-block) {
  display: block;
}

.note-rendered-preview :deep(.math-expression),
.markdown-comment :deep(.math-expression) {
  font-family: "Cambria Math", "Times New Roman", serif;
  white-space: pre-wrap;
}

@media (max-width: 980px) {
  .note-annotation-layout {
    grid-template-columns: 1fr;
  }

  .note-annotation-layout > .note-preview-card,
  .note-annotation-layout > .annotation-sidebar {
    max-height: none;
    overflow: visible;
  }

  .annotation-sidebar {
    position: static;
  }

  .pdf-preview-toolbar {
    align-items: flex-start;
  }
}
</style>
