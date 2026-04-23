<script setup lang="ts">
import { renderInputTextToHtml } from '~/utils/notePreview'

interface RoadmapGroup {
  id: string
  name: string
  items: string[]
}

interface RoadmapBranch {
  id: string
  title: string
  tone: string
  summary: string
  groups: RoadmapGroup[]
}

interface AlgorithmRoadmap {
  title: string
  subtitle: string
  branches: RoadmapBranch[]
  milestones: string[]
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

type SelectedNodeKind = 'center' | 'branch' | 'group' | 'item' | 'milestone'
type DragNodeKind = 'branch' | 'group' | 'item'

const toneOptions = [
  { value: 'cyan', label: '冰蓝' },
  { value: 'gold', label: '暖金' },
  { value: 'emerald', label: '青绿' },
  { value: 'violet', label: '雾紫' },
  { value: 'rose', label: '玫粉' },
  { value: 'slate', label: '石墨' }
] as const

const runtimeConfig = useRuntimeConfig()
const apiBase = runtimeConfig.public.apiBase

function createId(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`
}

function createRoadmapDraft(): AlgorithmRoadmap {
  return {
    title: '',
    subtitle: '',
    branches: [],
    milestones: []
  }
}

function cloneRoadmap(source: AlgorithmRoadmap): AlgorithmRoadmap {
  return {
    title: source.title,
    subtitle: source.subtitle,
    branches: source.branches.map(branch => ({
      id: branch.id,
      title: branch.title,
      tone: branch.tone,
      summary: branch.summary,
      groups: branch.groups.map(group => ({
        id: group.id,
        name: group.name,
        items: [...group.items]
      }))
    })),
    milestones: [...source.milestones]
  }
}

function createEmptyBranch(): RoadmapBranch {
  return {
    id: createId('branch'),
    title: '新的主干路线',
    tone: 'cyan',
    summary: '在这里补充这条主干路线的学习目标和说明。',
    groups: []
  }
}

function createEmptyGroup(): RoadmapGroup {
  return {
    id: createId('group'),
    name: '新的子节点组',
    items: []
  }
}

function renderSummaryText(value: string) {
  return renderInputTextToHtml(value)
}

function clearDropIndicators() {
  dragState.overBranchId = null
  dragState.overGroupId = null
  dragState.overItemKey = null
}

function clearDragState() {
  dragState.kind = null
  dragState.branchId = null
  dragState.groupId = null
  dragState.itemIndex = null
  clearDropIndicators()
}

function startBranchDrag(branchId: string) {
  dragState.kind = 'branch'
  dragState.branchId = branchId
  dragState.groupId = null
  dragState.itemIndex = null
  clearDropIndicators()
}

function startGroupDrag(branchId: string, groupId: string) {
  dragState.kind = 'group'
  dragState.branchId = branchId
  dragState.groupId = groupId
  dragState.itemIndex = null
  clearDropIndicators()
}

function startItemDrag(branchId: string, groupId: string, itemIndex: number) {
  dragState.kind = 'item'
  dragState.branchId = branchId
  dragState.groupId = groupId
  dragState.itemIndex = itemIndex
  clearDropIndicators()
}

function findBranchIndex(branchId: string) {
  return roadmap.branches.findIndex(branch => branch.id === branchId)
}

function findGroupLocation(branchId: string, groupId: string) {
  const branchIndex = findBranchIndex(branchId)
  if (branchIndex < 0) {
    return null
  }

  const groupIndex = roadmap.branches[branchIndex].groups.findIndex(group => group.id === groupId)
  if (groupIndex < 0) {
    return null
  }

  return { branchIndex, groupIndex }
}

function moveBranch(sourceBranchId: string, targetBranchId: string) {
  const sourceIndex = findBranchIndex(sourceBranchId)
  const targetIndex = findBranchIndex(targetBranchId)

  if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) {
    return
  }

  const [branch] = roadmap.branches.splice(sourceIndex, 1)
  const insertIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  roadmap.branches.splice(insertIndex, 0, branch)
  selectBranch(branch.id)
}

function moveGroupToBranch(sourceBranchId: string, groupId: string, targetBranchId: string) {
  const sourceLocation = findGroupLocation(sourceBranchId, groupId)
  const targetBranchIndex = findBranchIndex(targetBranchId)

  if (!sourceLocation || targetBranchIndex < 0) {
    return
  }

  const sourceBranch = roadmap.branches[sourceLocation.branchIndex]
  const [group] = sourceBranch.groups.splice(sourceLocation.groupIndex, 1)
  roadmap.branches[targetBranchIndex].groups.push(group)
  selectGroup(targetBranchId, group.id)
}

function moveGroupBeforeGroup(
  sourceBranchId: string,
  groupId: string,
  targetBranchId: string,
  targetGroupId: string
) {
  const sourceLocation = findGroupLocation(sourceBranchId, groupId)
  const targetLocation = findGroupLocation(targetBranchId, targetGroupId)

  if (!sourceLocation || !targetLocation) {
    return
  }

  if (sourceBranchId === targetBranchId && groupId === targetGroupId) {
    return
  }

  const sourceBranch = roadmap.branches[sourceLocation.branchIndex]
  const [group] = sourceBranch.groups.splice(sourceLocation.groupIndex, 1)
  const targetBranch = roadmap.branches[targetLocation.branchIndex]
  let insertIndex = targetLocation.groupIndex

  if (sourceLocation.branchIndex === targetLocation.branchIndex && sourceLocation.groupIndex < targetLocation.groupIndex) {
    insertIndex -= 1
  }

  targetBranch.groups.splice(insertIndex, 0, group)
  selectGroup(targetBranch.id, group.id)
}

function moveItemToGroup(
  sourceBranchId: string,
  sourceGroupId: string,
  sourceItemIndex: number,
  targetBranchId: string,
  targetGroupId: string
) {
  const sourceLocation = findGroupLocation(sourceBranchId, sourceGroupId)
  const targetLocation = findGroupLocation(targetBranchId, targetGroupId)

  if (!sourceLocation || !targetLocation) {
    return
  }

  const sourceGroup = roadmap.branches[sourceLocation.branchIndex].groups[sourceLocation.groupIndex]
  const [item] = sourceGroup.items.splice(sourceItemIndex, 1)
  const targetGroup = roadmap.branches[targetLocation.branchIndex].groups[targetLocation.groupIndex]
  targetGroup.items.push(item)
  selectItem(targetBranchId, targetGroupId, targetGroup.items.length - 1)
}

function moveItemBeforeItem(
  sourceBranchId: string,
  sourceGroupId: string,
  sourceItemIndex: number,
  targetBranchId: string,
  targetGroupId: string,
  targetItemIndex: number
) {
  const sourceLocation = findGroupLocation(sourceBranchId, sourceGroupId)
  const targetLocation = findGroupLocation(targetBranchId, targetGroupId)

  if (!sourceLocation || !targetLocation) {
    return
  }

  if (
    sourceBranchId === targetBranchId
    && sourceGroupId === targetGroupId
    && sourceItemIndex === targetItemIndex
  ) {
    return
  }

  const sourceGroup = roadmap.branches[sourceLocation.branchIndex].groups[sourceLocation.groupIndex]
  const [item] = sourceGroup.items.splice(sourceItemIndex, 1)
  const targetGroup = roadmap.branches[targetLocation.branchIndex].groups[targetLocation.groupIndex]
  let insertIndex = targetItemIndex

  if (
    sourceBranchId === targetBranchId
    && sourceGroupId === targetGroupId
    && sourceItemIndex < targetItemIndex
  ) {
    insertIndex -= 1
  }

  targetGroup.items.splice(insertIndex, 0, item)
  selectItem(targetBranchId, targetGroupId, insertIndex)
}

function handleBranchDragOver(branchId: string) {
  if (!dragState.kind) {
    return
  }

  dragState.overBranchId = branchId
  dragState.overGroupId = null
  dragState.overItemKey = null
}

function handleGroupDragOver(branchId: string, groupId: string) {
  if (!dragState.kind) {
    return
  }

  dragState.overBranchId = branchId
  dragState.overGroupId = groupId
  dragState.overItemKey = null
}

function handleItemDragOver(branchId: string, groupId: string, itemIndex: number) {
  if (!dragState.kind) {
    return
  }

  dragState.overBranchId = branchId
  dragState.overGroupId = groupId
  dragState.overItemKey = `${groupId}:${itemIndex}`
}

function handleBranchDrop(branchId: string) {
  if (dragState.kind === 'branch' && dragState.branchId) {
    moveBranch(dragState.branchId, branchId)
  } else if (dragState.kind === 'group' && dragState.branchId && dragState.groupId) {
    moveGroupToBranch(dragState.branchId, dragState.groupId, branchId)
  }

  clearDragState()
}

function handleGroupDrop(branchId: string, groupId: string) {
  if (dragState.kind === 'group' && dragState.branchId && dragState.groupId) {
    moveGroupBeforeGroup(dragState.branchId, dragState.groupId, branchId, groupId)
  } else if (
    dragState.kind === 'item'
    && dragState.branchId
    && dragState.groupId
    && dragState.itemIndex !== null
  ) {
    moveItemToGroup(dragState.branchId, dragState.groupId, dragState.itemIndex, branchId, groupId)
  }

  clearDragState()
}

function handleItemDrop(branchId: string, groupId: string, itemIndex: number) {
  if (
    dragState.kind === 'item'
    && dragState.branchId
    && dragState.groupId
    && dragState.itemIndex !== null
  ) {
    moveItemBeforeItem(dragState.branchId, dragState.groupId, dragState.itemIndex, branchId, groupId, itemIndex)
  }

  clearDragState()
}

const { data: response, error, refresh } = await useFetch<ApiResponse<AlgorithmRoadmap>>(
  `${apiBase}/api/algorithms/roadmap`
)

const roadmap = reactive<AlgorithmRoadmap>(createRoadmapDraft())
const isApplyingSnapshot = ref(false)
const savingRoadmap = ref(false)
const saveError = ref('')
const saveSuccess = ref('')
const hasUnsavedChanges = ref(false)
const dragState = reactive<{
  kind: DragNodeKind | null
  branchId: string | null
  groupId: string | null
  itemIndex: number | null
  overBranchId: string | null
  overGroupId: string | null
  overItemKey: string | null
}>({
  kind: null,
  branchId: null,
  groupId: null,
  itemIndex: null,
  overBranchId: null,
  overGroupId: null,
  overItemKey: null
})

const selectedNode = reactive<{
  kind: SelectedNodeKind
  branchId: string | null
  groupId: string | null
  itemIndex: number | null
  milestoneIndex: number | null
}>({
  kind: 'center',
  branchId: null,
  groupId: null,
  itemIndex: null,
  milestoneIndex: null
})

function selectCenterNode() {
  selectedNode.kind = 'center'
  selectedNode.branchId = null
  selectedNode.groupId = null
  selectedNode.itemIndex = null
  selectedNode.milestoneIndex = null
}

function selectBranch(branchId: string) {
  selectedNode.kind = 'branch'
  selectedNode.branchId = branchId
  selectedNode.groupId = null
  selectedNode.itemIndex = null
  selectedNode.milestoneIndex = null
}

function selectGroup(branchId: string, groupId: string) {
  selectedNode.kind = 'group'
  selectedNode.branchId = branchId
  selectedNode.groupId = groupId
  selectedNode.itemIndex = null
  selectedNode.milestoneIndex = null
}

function selectItem(branchId: string, groupId: string, itemIndex: number) {
  selectedNode.kind = 'item'
  selectedNode.branchId = branchId
  selectedNode.groupId = groupId
  selectedNode.itemIndex = itemIndex
  selectedNode.milestoneIndex = null
}

function selectMilestone(index: number) {
  selectedNode.kind = 'milestone'
  selectedNode.branchId = null
  selectedNode.groupId = null
  selectedNode.itemIndex = null
  selectedNode.milestoneIndex = index
}

const selectedBranch = computed(() => {
  if (!selectedNode.branchId) {
    return null
  }
  return roadmap.branches.find(branch => branch.id === selectedNode.branchId) ?? null
})

const selectedGroup = computed(() => {
  if (!selectedBranch.value || !selectedNode.groupId) {
    return null
  }
  return selectedBranch.value.groups.find(group => group.id === selectedNode.groupId) ?? null
})

const selectedItem = computed({
  get() {
    if (!selectedGroup.value || selectedNode.itemIndex === null) {
      return ''
    }
    return selectedGroup.value.items[selectedNode.itemIndex] ?? ''
  },
  set(value: string) {
    if (!selectedGroup.value || selectedNode.itemIndex === null) {
      return
    }
    selectedGroup.value.items[selectedNode.itemIndex] = value
  }
})

const selectedMilestone = computed({
  get() {
    if (selectedNode.milestoneIndex === null) {
      return ''
    }
    return roadmap.milestones[selectedNode.milestoneIndex] ?? ''
  },
  set(value: string) {
    if (selectedNode.milestoneIndex === null) {
      return
    }
    roadmap.milestones[selectedNode.milestoneIndex] = value
  }
})

function applyRoadmapSnapshot(source: AlgorithmRoadmap) {
  isApplyingSnapshot.value = true
  const next = cloneRoadmap(source)
  roadmap.title = next.title
  roadmap.subtitle = next.subtitle
  roadmap.branches = next.branches
  roadmap.milestones = next.milestones
  ensureSelectionStillExists()
  hasUnsavedChanges.value = false
  isApplyingSnapshot.value = false
}

function ensureSelectionStillExists() {
  if (selectedNode.kind === 'center') {
    return
  }

  if (selectedNode.kind === 'milestone') {
    if (selectedNode.milestoneIndex === null || !roadmap.milestones[selectedNode.milestoneIndex]) {
      selectCenterNode()
    }
    return
  }

  const branch = roadmap.branches.find(item => item.id === selectedNode.branchId)
  if (!branch) {
    selectCenterNode()
    return
  }

  if (selectedNode.kind === 'branch') {
    return
  }

  const group = branch.groups.find(item => item.id === selectedNode.groupId)
  if (!group) {
    selectBranch(branch.id)
    return
  }

  if (selectedNode.kind === 'group') {
    return
  }

  if (selectedNode.itemIndex === null || !group.items[selectedNode.itemIndex]) {
    selectGroup(branch.id, group.id)
  }
}

function addBranch() {
  const branch = createEmptyBranch()
  roadmap.branches.push(branch)
  selectBranch(branch.id)
}

function addGroup() {
  if (!selectedBranch.value) {
    return
  }
  const group = createEmptyGroup()
  selectedBranch.value.groups.push(group)
  selectGroup(selectedBranch.value.id, group.id)
}

function addItem() {
  if (!selectedGroup.value || !selectedBranch.value) {
    return
  }
  selectedGroup.value.items.push('新的知识节点')
  selectItem(selectedBranch.value.id, selectedGroup.value.id, selectedGroup.value.items.length - 1)
}

function addMilestone() {
  roadmap.milestones.push('新的阶段目标')
  selectMilestone(roadmap.milestones.length - 1)
}

function removeSelectedBranch() {
  if (!selectedBranch.value) {
    return
  }
  roadmap.branches = roadmap.branches.filter(branch => branch.id !== selectedBranch.value?.id)
  selectCenterNode()
}

function removeSelectedGroup() {
  if (!selectedBranch.value || !selectedGroup.value) {
    return
  }
  selectedBranch.value.groups = selectedBranch.value.groups.filter(group => group.id !== selectedGroup.value?.id)
  selectBranch(selectedBranch.value.id)
}

function removeSelectedItem() {
  if (!selectedGroup.value || selectedNode.itemIndex === null || !selectedBranch.value) {
    return
  }
  selectedGroup.value.items.splice(selectedNode.itemIndex, 1)
  selectGroup(selectedBranch.value.id, selectedGroup.value.id)
}

function removeSelectedMilestone() {
  if (selectedNode.milestoneIndex === null) {
    return
  }
  roadmap.milestones.splice(selectedNode.milestoneIndex, 1)
  selectCenterNode()
}

async function saveRoadmap() {
  savingRoadmap.value = true
  saveError.value = ''
  saveSuccess.value = ''

  try {
    const saved = await $fetch<ApiResponse<AlgorithmRoadmap>>(`${apiBase}/api/algorithms/roadmap`, {
      method: 'PUT',
      body: cloneRoadmap(roadmap)
    })
    applyRoadmapSnapshot(saved.data)
    saveSuccess.value = '学习路线图已保存。'
  } catch (saveFailure: any) {
    saveError.value = saveFailure?.data?.message ?? '保存路线图失败，请稍后重试。'
  } finally {
    savingRoadmap.value = false
  }
}

async function resetRoadmap() {
  saveError.value = ''
  saveSuccess.value = ''
  await refresh()
  if (response.value?.data) {
    applyRoadmapSnapshot(response.value.data)
  }
}

watch(
  () => response.value?.data,
  value => {
    if (value) {
      applyRoadmapSnapshot(value)
    }
  },
  { immediate: true }
)

watch(
  roadmap,
  () => {
    if (!isApplyingSnapshot.value) {
      hasUnsavedChanges.value = true
    }
  },
  { deep: true }
)
</script>

<template>
  <div class="page-stack algorithm-roadmap-page">
    <section class="page-hero roadmap-hero">
      <div>
        <p class="eyebrow">Mind Map</p>
        <h1>算法学习路线图</h1>
        <p>
          这张路线图现在不只是展示页面了，你可以把它当作算法知识图谱的编辑台。
          点击任意主干、子组或叶子节点，右侧就能直接修改、补充或删除内容；也支持鼠标拖拽重排和跨分支迁移。
        </p>
      </div>

      <div class="hero-actions">
        <NuxtLink class="button secondary" to="/algorithms">返回算法学习页</NuxtLink>
        <NuxtLink class="button secondary" to="/notes">进入学习笔记页</NuxtLink>
        <button class="button secondary" type="button" @click="addBranch">新增主干</button>
        <button class="button secondary" type="button" @click="addMilestone">新增阶段目标</button>
        <button class="button primary" type="button" :disabled="savingRoadmap" @click="saveRoadmap">
          {{ savingRoadmap ? '保存中...' : '保存路线图' }}
        </button>
      </div>
    </section>

    <section class="content-card roadmap-status-card">
      <div class="roadmap-status-copy">
        <strong>{{ hasUnsavedChanges ? '当前有未保存修改' : '当前修改已同步到本地草稿' }}</strong>
        <span>改动不会自动提交到后端，确认无误后再点击“保存路线图”。</span>
      </div>
      <div class="hero-actions">
        <button class="button secondary compact" type="button" @click="resetRoadmap">撤回到最近一次保存</button>
      </div>
      <p v-if="saveError" class="roadmap-feedback error">{{ saveError }}</p>
      <p v-else-if="saveSuccess" class="roadmap-feedback success">{{ saveSuccess }}</p>
      <p v-else-if="error?.data?.message" class="roadmap-feedback error">{{ error.data.message }}</p>
    </section>

    <section class="roadmap-workbench">
      <article class="content-card roadmap-overview-card">
        <p class="eyebrow">Knowledge Graph</p>
        <h2>可点击的学习图谱</h2>
        <div class="mindmap-shell">
          <button
            class="mindmap-center"
            :class="{ active: selectedNode.kind === 'center' }"
            type="button"
            @click="selectCenterNode"
          >
            <span class="mindmap-center-kicker">Algorithm Lab</span>
            <strong>{{ roadmap.title || '算法学习主路线' }}</strong>
            <small>{{ roadmap.subtitle || '从基础到专题，再到综合能力' }}</small>
          </button>

          <div v-if="roadmap.branches.length" class="mindmap-branches">
            <article
              v-for="branch in roadmap.branches"
              :key="branch.id"
              class="mindmap-branch-card"
              :class="[
                `tone-${branch.tone}`,
                {
                  active: selectedNode.branchId === branch.id,
                  'is-drop-target': dragState.overBranchId === branch.id && !dragState.overGroupId
                }
              ]"
              draggable="true"
              @dragstart="startBranchDrag(branch.id)"
              @dragend="clearDragState"
              @dragover.prevent="handleBranchDragOver(branch.id)"
              @drop.prevent="handleBranchDrop(branch.id)"
            >
              <button class="mindmap-branch-hitbox" type="button" @click="selectBranch(branch.id)">
                <div class="mindmap-node-head">
                  <h3>{{ branch.title }}</h3>
                  <div class="branch-summary rich-text-rendered" v-html="renderSummaryText(branch.summary)" />
                </div>
              </button>

              <div class="mindmap-group-list">
                <section
                  v-for="group in branch.groups"
                  :key="group.id"
                  class="mindmap-leaf-group"
                  :class="{
                    active: selectedNode.groupId === group.id,
                    'is-drop-target': dragState.overBranchId === branch.id && dragState.overGroupId === group.id && !dragState.overItemKey
                  }"
                  draggable="true"
                  @dragstart.stop="startGroupDrag(branch.id, group.id)"
                  @dragend="clearDragState"
                  @dragover.prevent.stop="handleGroupDragOver(branch.id, group.id)"
                  @drop.prevent.stop="handleGroupDrop(branch.id, group.id)"
                >
                  <button class="mindmap-group-hitbox" type="button" @click="selectGroup(branch.id, group.id)">
                    <strong>{{ group.name }}</strong>
                  </button>
                  <div class="mindmap-item-list">
                    <button
                      v-for="(item, itemIndex) in group.items"
                      :key="`${group.id}-${itemIndex}-${item}`"
                      class="mindmap-item-pill"
                      :class="{
                        active: selectedNode.groupId === group.id && selectedNode.itemIndex === itemIndex,
                        'is-drop-target': dragState.overItemKey === `${group.id}:${itemIndex}`
                      }"
                      type="button"
                      draggable="true"
                      @click="selectItem(branch.id, group.id, itemIndex)"
                      @dragstart.stop="startItemDrag(branch.id, group.id, itemIndex)"
                      @dragend="clearDragState"
                      @dragover.prevent.stop="handleItemDragOver(branch.id, group.id, itemIndex)"
                      @drop.prevent.stop="handleItemDrop(branch.id, group.id, itemIndex)"
                    >
                      {{ item }}
                    </button>
                  </div>
                </section>
              </div>
            </article>
          </div>

          <div v-else class="mindmap-empty-state">
            <p>当前还没有主干节点，先从右上角新增一条主干路线吧。</p>
          </div>
        </div>
      </article>

      <aside class="roadmap-editor-stack">
        <article class="content-card roadmap-editor-card">
          <p class="eyebrow">Node Editor</p>
          <h2>节点编辑器</h2>

          <div v-if="selectedNode.kind === 'center'" class="roadmap-form-grid">
            <label class="roadmap-field">
              <span>中心主题</span>
              <input v-model="roadmap.title" class="roadmap-input" maxlength="80" />
            </label>
            <label class="roadmap-field">
              <span>中心副标题</span>
              <textarea v-model="roadmap.subtitle" class="roadmap-textarea" rows="4" maxlength="200" />
            </label>
          </div>

          <div v-else-if="selectedNode.kind === 'branch' && selectedBranch" class="roadmap-form-grid">
            <label class="roadmap-field">
              <span>主干标题</span>
              <input v-model="selectedBranch.title" class="roadmap-input" maxlength="60" />
            </label>
            <label class="roadmap-field">
              <span>主干色调</span>
              <select v-model="selectedBranch.tone" class="roadmap-select">
                <option v-for="tone in toneOptions" :key="tone.value" :value="tone.value">
                  {{ tone.label }}
                </option>
              </select>
            </label>
            <label class="roadmap-field">
              <span>主干说明</span>
              <textarea v-model="selectedBranch.summary" class="roadmap-textarea" rows="5" maxlength="240" />
            </label>
            <div class="roadmap-editor-actions">
              <button class="button secondary compact" type="button" @click="addGroup">新增子节点组</button>
              <button class="button secondary compact danger" type="button" @click="removeSelectedBranch">删除主干</button>
            </div>
          </div>

          <div v-else-if="selectedNode.kind === 'group' && selectedGroup" class="roadmap-form-grid">
            <label class="roadmap-field">
              <span>子节点组名称</span>
              <input v-model="selectedGroup.name" class="roadmap-input" maxlength="60" />
            </label>
            <div class="roadmap-editor-actions">
              <button class="button secondary compact" type="button" @click="addItem">新增叶子节点</button>
              <button class="button secondary compact danger" type="button" @click="removeSelectedGroup">删除子节点组</button>
            </div>
          </div>

          <div v-else-if="selectedNode.kind === 'item'" class="roadmap-form-grid">
            <label class="roadmap-field">
              <span>叶子节点内容</span>
              <textarea v-model="selectedItem" class="roadmap-textarea" rows="4" maxlength="80" />
            </label>
            <div class="roadmap-editor-actions">
              <button class="button secondary compact danger" type="button" @click="removeSelectedItem">删除叶子节点</button>
            </div>
          </div>

          <div v-else-if="selectedNode.kind === 'milestone'" class="roadmap-form-grid">
            <label class="roadmap-field">
              <span>阶段目标</span>
              <textarea v-model="selectedMilestone" class="roadmap-textarea" rows="4" maxlength="120" />
            </label>
            <div class="roadmap-editor-actions">
              <button class="button secondary compact danger" type="button" @click="removeSelectedMilestone">删除阶段目标</button>
            </div>
          </div>

          <div v-else class="roadmap-empty-editor">
            <p>点击左侧任意节点后，这里会显示对应的可编辑字段。</p>
          </div>
        </article>

        <article class="content-card roadmap-milestone-card">
          <div class="roadmap-card-head">
            <div>
              <p class="eyebrow">Milestones</p>
              <h2>阶段目标</h2>
            </div>
            <button class="button secondary compact" type="button" @click="addMilestone">新增目标</button>
          </div>

          <div v-if="roadmap.milestones.length" class="roadmap-milestone-list">
            <button
              v-for="(milestone, milestoneIndex) in roadmap.milestones"
              :key="`${milestoneIndex}-${milestone}`"
              class="roadmap-milestone-item"
              :class="{ active: selectedNode.kind === 'milestone' && selectedNode.milestoneIndex === milestoneIndex }"
              type="button"
              @click="selectMilestone(milestoneIndex)"
            >
              {{ milestone }}
            </button>
          </div>
          <div v-else class="roadmap-empty-editor">
            <p>还没有阶段目标，你可以新增一条作为阶段推进标记。</p>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.algorithm-roadmap-page {
  width: min(1560px, 100%);
  margin: 0 auto;
}

.roadmap-hero {
  display: grid;
  gap: 18px;
}

.roadmap-status-card {
  display: grid;
  gap: 14px;
}

.roadmap-status-copy {
  display: grid;
  gap: 8px;
}

.roadmap-status-copy strong {
  font-size: 18px;
}

.roadmap-status-copy span,
.roadmap-feedback {
  color: var(--muted);
}

.roadmap-feedback {
  margin: 0;
}

.roadmap-feedback.error {
  color: #ff9d9d;
}

.roadmap-feedback.success {
  color: #99efc5;
}

.roadmap-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(360px, 0.8fr);
  gap: 24px;
  align-items: start;
}

.roadmap-overview-card {
  overflow: hidden;
}

.roadmap-editor-stack {
  display: grid;
  gap: 24px;
  position: sticky;
  top: 104px;
}

.roadmap-editor-card,
.roadmap-milestone-card {
  display: grid;
  gap: 18px;
}

.roadmap-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.roadmap-card-head h2 {
  margin: 8px 0 0;
}

.mindmap-shell {
  position: relative;
  display: grid;
  gap: 26px;
  min-height: 780px;
  padding: 28px;
  border: 1px solid var(--border);
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(107, 211, 255, 0.14), transparent 24%),
    radial-gradient(circle at 85% 22%, rgba(255, 211, 107, 0.1), transparent 20%),
    rgba(255, 255, 255, 0.03);
}

.mindmap-shell::before {
  content: "";
  position: absolute;
  inset: 50% 10% auto;
  height: 2px;
  background: linear-gradient(90deg, rgba(107, 211, 255, 0.12), rgba(255, 211, 107, 0.24), rgba(107, 211, 255, 0.12));
  transform: translateY(-50%);
}

.mindmap-center {
  position: relative;
  z-index: 1;
  width: min(380px, 100%);
  margin: 0 auto;
  padding: 24px 28px;
  border: 1px solid rgba(107, 211, 255, 0.3);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(107, 211, 255, 0.18), rgba(255, 211, 107, 0.12));
  text-align: center;
  box-shadow: 0 24px 60px rgba(3, 10, 20, 0.32);
  cursor: pointer;
}

.mindmap-center.active,
.mindmap-branch-card.active,
.mindmap-leaf-group.active,
.mindmap-item-pill.active,
.roadmap-milestone-item.active {
  box-shadow: 0 0 0 2px rgba(107, 211, 255, 0.42);
}

.mindmap-branch-card.is-drop-target,
.mindmap-leaf-group.is-drop-target,
.mindmap-item-pill.is-drop-target {
  box-shadow: 0 0 0 2px rgba(255, 211, 107, 0.4);
}

.mindmap-center-kicker {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(7, 17, 31, 0.34);
  color: var(--secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.mindmap-center strong {
  display: block;
  font-size: clamp(28px, 4vw, 40px);
}

.mindmap-center small {
  display: block;
  margin-top: 10px;
  color: rgba(237, 244, 255, 0.78);
  font-size: 15px;
}

.mindmap-branches {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.mindmap-branch-card {
  position: relative;
  display: grid;
  gap: 18px;
  min-height: 100%;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 24px;
  background: rgba(12, 22, 37, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  cursor: grab;
}

.mindmap-branch-card::before {
  content: "";
  position: absolute;
  top: -36px;
  left: 50%;
  width: 2px;
  height: 36px;
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(-50%);
}

.mindmap-branch-card::after {
  content: "";
  position: absolute;
  top: -42px;
  left: 50%;
  width: 12px;
  height: 12px;
  border: 2px solid currentColor;
  border-radius: 999px;
  background: #08101c;
  transform: translateX(-50%);
  opacity: 0.78;
}

.mindmap-branch-hitbox,
.mindmap-group-hitbox {
  all: unset;
  cursor: pointer;
}

.mindmap-node-head {
  display: grid;
  gap: 10px;
}

.mindmap-node-head h3 {
  margin: 0;
  font-size: 22px;
}

.mindmap-node-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.75;
  font-size: 15px;
}

.branch-summary {
  color: var(--muted);
  line-height: 1.75;
  font-size: 15px;
}

.mindmap-group-list {
  display: grid;
  gap: 14px;
}

.mindmap-leaf-group {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: grab;
}

.mindmap-leaf-group strong {
  font-size: 15px;
}

.mindmap-item-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mindmap-item-pill,
.roadmap-milestone-item {
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--muted);
  text-align: left;
  cursor: pointer;
}

.mindmap-item-pill {
  max-width: 100%;
  cursor: grab;
}

.mindmap-empty-state,
.roadmap-empty-editor {
  padding: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 18px;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.02);
}

.roadmap-form-grid {
  display: grid;
  gap: 16px;
}

.roadmap-field {
  display: grid;
  gap: 8px;
}

.roadmap-field span {
  color: var(--muted);
  font-size: 14px;
}

.roadmap-input,
.roadmap-textarea,
.roadmap-select {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  font: inherit;
}

.roadmap-textarea {
  resize: vertical;
  min-height: 110px;
}

.roadmap-editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.roadmap-milestone-list {
  display: grid;
  gap: 12px;
}

.button.danger {
  border-color: rgba(255, 157, 157, 0.24);
  color: #ffbcbc;
}

.tone-cyan {
  color: #84defd;
}

.tone-gold {
  color: #ffd98b;
}

.tone-emerald {
  color: #9af3c9;
}

.tone-violet {
  color: #c0b3ff;
}

.tone-rose {
  color: #ffb7c4;
}

.tone-slate {
  color: #c8d1e1;
}

.tone-cyan .mindmap-node-head h3,
.tone-gold .mindmap-node-head h3,
.tone-emerald .mindmap-node-head h3,
.tone-violet .mindmap-node-head h3,
.tone-rose .mindmap-node-head h3,
.tone-slate .mindmap-node-head h3 {
  color: inherit;
}

@media (max-width: 1360px) {
  .roadmap-workbench {
    grid-template-columns: 1fr;
  }

  .roadmap-editor-stack {
    position: static;
  }

  .mindmap-branches {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mindmap-shell::before {
    display: none;
  }

  .mindmap-branch-card::before,
  .mindmap-branch-card::after {
    display: none;
  }
}

@media (max-width: 860px) {
  .mindmap-shell {
    min-height: auto;
    padding: 18px;
  }

  .mindmap-branches {
    grid-template-columns: 1fr;
  }

  .mindmap-center,
  .roadmap-card-head {
    width: 100%;
  }

  .roadmap-card-head {
    display: grid;
  }
}
</style>
