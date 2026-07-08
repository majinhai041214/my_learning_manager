<script setup lang="ts">
interface BlogPostSummary {
  slug: string
  title: string
  date: string | null
  tags: string[]
  excerpt: string
  sourceFilename: string
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

const searchQuery = ref('')
const selectedTag = ref('ALL')
const uploading = ref(false)
const uploadError = ref('')
const uploadSuccess = ref('')
const selectedPostFile = ref<File | null>(null)
const selectedImageFiles = ref<File[]>([])
const selectedImageCount = computed(() => selectedImageFiles.value.length)

const { data: response, pending, error } = await useFetch<ApiResponse<BlogPostSummary[]>>('/blog/posts', {
  baseURL: apiBase
})

const posts = computed(() => response.value?.data ?? [])

const tagSummaries = computed(() => {
  const tagMap = new Map<string, number>()
  for (const post of posts.value) {
    for (const tag of post.tags) {
      tagMap.set(tag, (tagMap.get(tag) ?? 0) + 1)
    }
  }

  return Array.from(tagMap.entries())
    .map(([tag, count]) => ({ tag, count }))
    .sort((left, right) => right.count - left.count || left.tag.localeCompare(right.tag, 'zh-CN'))
})

const filteredPosts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return posts.value.filter((post) => {
    const matchesTag = selectedTag.value === 'ALL' || post.tags.includes(selectedTag.value)
    const searchableText = `${post.title} ${post.excerpt} ${post.tags.join(' ')}`.toLowerCase()
    return matchesTag && (!query || searchableText.includes(query))
  })
})

function getPostRoute(post: BlogPostSummary) {
  return {
    name: 'blog-slug',
    params: {
      slug: post.slug
    }
  }
}

function formatDate(value: string | null) {
  if (!value) {
    return '未设置日期'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value))
}

function handlePostFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedPostFile.value = input.files?.[0] ?? null
}

function handleImageFilesChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedImageFiles.value = mergeSelectedImages(selectedImageFiles.value, Array.from(input.files ?? []))
}

function handleImageFolderChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedImageFiles.value = mergeSelectedImages(selectedImageFiles.value, Array.from(input.files ?? []))
}

function mergeSelectedImages(currentFiles: File[], nextFiles: File[]) {
  const fileMap = new Map<string, File>()
  for (const file of [...currentFiles, ...nextFiles]) {
    const relativePath = (file as File & { webkitRelativePath?: string }).webkitRelativePath ?? ''
    fileMap.set(`${relativePath || file.name}-${file.size}-${file.lastModified}`, file)
  }
  return Array.from(fileMap.values())
}

async function uploadBlogPost() {
  if (!selectedPostFile.value) {
    uploadError.value = '请先选择一篇 Markdown 博客文章。'
    return
  }

  uploading.value = true
  uploadError.value = ''
  uploadSuccess.value = ''

  try {
    const payload = new FormData()
    payload.append('post', selectedPostFile.value)
    for (const image of selectedImageFiles.value) {
      const relativePath = (image as File & { webkitRelativePath?: string }).webkitRelativePath
      payload.append('images', image, relativePath || image.name)
    }

    const uploaded = await $fetch<ApiResponse<BlogPostSummary>>('/blog/posts/upload', {
      baseURL: apiBase,
      method: 'POST',
      body: payload
    })

    const nextPosts = [uploaded.data, ...posts.value]
    if (response.value) {
      response.value = {
        ...response.value,
        data: nextPosts.sort((left, right) => {
          const leftTime = left.date ? new Date(left.date).getTime() : 0
          const rightTime = right.date ? new Date(right.date).getTime() : 0
          return rightTime - leftTime
        })
      }
    }

    selectedPostFile.value = null
    selectedImageFiles.value = []
    const postInput = document.getElementById('blog-post-file') as HTMLInputElement | null
    const imageInput = document.getElementById('blog-image-files') as HTMLInputElement | null
    const folderInput = document.getElementById('blog-image-folder') as HTMLInputElement | null
    if (postInput) {
      postInput.value = ''
    }
    if (imageInput) {
      imageInput.value = ''
    }
    if (folderInput) {
      folderInput.value = ''
    }

    uploadSuccess.value = '博客文章上传成功，列表已刷新。'
  } catch (uploadFailure: any) {
    uploadError.value = uploadFailure?.data?.message ?? '上传失败，请检查文件格式或稍后重试。'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="blog-page">
    <section class="blog-hero-band">
      <div class="blog-hero-copy">
        <p class="eyebrow">Blog</p>
        <h1>博客文章</h1>
        <p>学习笔记、日常随笔、科研训练和项目复盘会在这里汇总成更完整的公开内容。</p>
      </div>

      <form class="blog-submit-panel" @submit.prevent="uploadBlogPost">
        <p class="eyebrow">Markdown Entry</p>
        <h2>上传博客文章</h2>

        <label class="blog-upload-field">
          <span>Markdown 文件</span>
          <input
            id="blog-post-file"
            type="file"
            accept=".md,.markdown"
            required
            @change="handlePostFileChange"
          />
        </label>

        <label class="blog-upload-field">
          <span>文章图片（可选，可多选）</span>
          <input
            id="blog-image-files"
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            multiple
            @change="handleImageFilesChange"
          />
        </label>

        <label class="blog-upload-field">
          <span>图片文件夹（可选，适合原博客 assets/image 文件夹）</span>
          <input
            id="blog-image-folder"
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            multiple
            webkitdirectory
            directory
            @change="handleImageFolderChange"
          />
        </label>

        <p class="blog-upload-note">
          含图片的文章需要同时上传图片，已选择 <strong>{{ selectedImageCount }}</strong> 张。
          Markdown 里的 <code>/assets/demo.png</code>、<code>image/demo.png</code> 或
          <code>![图](demo.png)</code> 会按文件名自动匹配同步上传的图片。
        </p>

        <p v-if="uploadError" class="blog-upload-feedback error">{{ uploadError }}</p>
        <p v-else-if="uploadSuccess" class="blog-upload-feedback success">{{ uploadSuccess }}</p>

        <button class="button primary blog-upload-button" type="submit" :disabled="uploading">
          {{ uploading ? '上传中...' : '上传博客' }}
        </button>
      </form>
    </section>

    <section class="blog-toolbar" aria-label="博客筛选">
      <div class="blog-search-wrap">
        <span>搜索</span>
        <input v-model="searchQuery" type="search" placeholder="输入标题、摘要或标签" />
      </div>

      <div class="blog-tags">
        <button
          type="button"
          class="blog-tag-button"
          :class="{ active: selectedTag === 'ALL' }"
          @click="selectedTag = 'ALL'"
        >
          全部
          <span>{{ posts.length }}</span>
        </button>
        <button
          v-for="item in tagSummaries"
          :key="item.tag"
          type="button"
          class="blog-tag-button"
          :class="{ active: selectedTag === item.tag }"
          @click="selectedTag = item.tag"
        >
          {{ item.tag }}
          <span>{{ item.count }}</span>
        </button>
      </div>
    </section>

    <section class="blog-content-layout">
      <div class="blog-feed">
        <p v-if="pending" class="blog-state">正在读取博客文章...</p>
        <p v-else-if="error" class="blog-state">暂时无法读取博客文章，请确认后端服务已经启动。</p>
        <div v-else-if="!posts.length" class="blog-empty-state">
          <p class="eyebrow">Empty Library</p>
          <h2>还没有可展示的博客文章</h2>
          <p>
            把带 frontmatter 的 Markdown 文件放进
            <code>/opt/www/website/uploads/blog/posts</code> 后，刷新页面就会出现在这里。
          </p>
        </div>
        <p v-else-if="!filteredPosts.length" class="blog-state">当前筛选条件下没有文章。</p>

        <template v-else>
          <NuxtLink
            v-for="post in filteredPosts"
            :key="post.slug"
            class="blog-post-link"
            :to="getPostRoute(post)"
          >
            <article class="blog-post-card">
              <div class="blog-post-meta">
                <time>{{ formatDate(post.date) }}</time>
                <span>{{ post.sourceFilename }}</span>
              </div>
              <h2>{{ post.title }}</h2>
              <p>{{ post.excerpt }}</p>
              <div v-if="post.tags.length" class="blog-post-tags">
                <span v-for="tag in post.tags" :key="tag">#{{ tag }}</span>
              </div>
            </article>
          </NuxtLink>
        </template>
      </div>

      <aside class="blog-guide">
        <p class="eyebrow">Post Format</p>
        <h2>推荐 Markdown 头部</h2>
        <pre><code>---
title: "文章标题"
date: 2026-07-03
tags: [项目复盘, 学习]
---

这里写正文内容。

![图片说明](demo/image.png)</code></pre>
        <p>
          示例图片路径会读取：
          <code>/opt/www/website/uploads/blog/images/demo/image.png</code>
        </p>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.blog-page {
  display: grid;
  gap: 28px;
}

.blog-hero-band {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.46fr);
  gap: 24px;
  align-items: stretch;
  padding: 34px;
  border: 1px solid var(--border);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(108, 159, 255, 0.13), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03));
}

.blog-hero-copy h1 {
  margin: 14px 0 12px;
  font-size: 58px;
  line-height: 1;
}

.blog-hero-copy p:last-child,
.blog-submit-panel dd,
.blog-guide p,
.blog-state,
.blog-empty-state p,
.blog-post-card p {
  color: var(--muted);
  line-height: 1.8;
}

.blog-submit-panel,
.blog-guide,
.blog-empty-state {
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.blog-submit-panel h2,
.blog-guide h2,
.blog-empty-state h2 {
  margin: 12px 0;
  font-size: 26px;
}

.blog-submit-panel dl {
  display: grid;
  gap: 14px;
  margin: 0;
}

.blog-submit-panel dt {
  margin-bottom: 6px;
  color: var(--secondary);
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.blog-submit-panel {
  display: grid;
  gap: 14px;
}

.blog-upload-field {
  display: grid;
  gap: 8px;
}

.blog-upload-field span {
  color: var(--muted);
}

.blog-upload-field input {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
  font: inherit;
}

.blog-upload-note,
.blog-upload-feedback {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.blog-upload-feedback.error {
  color: #ffb3b3;
}

.blog-upload-feedback.success {
  color: #9af0c4;
}

.blog-upload-button {
  width: 100%;
}

code {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #dff6ff;
}

.blog-toolbar {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.035);
}

.blog-search-wrap {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.blog-search-wrap span {
  color: var(--secondary);
  font-weight: 700;
}

.blog-search-wrap input {
  width: 100%;
  padding: 13px 15px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
  font: inherit;
  outline: none;
}

.blog-search-wrap input:focus {
  border-color: rgba(107, 211, 255, 0.45);
}

.blog-tags,
.blog-post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.blog-tag-button,
.blog-post-tags span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--text);
  font: inherit;
}

.blog-tag-button {
  cursor: pointer;
}

.blog-tag-button span,
.blog-post-tags span {
  color: var(--muted);
}

.blog-tag-button.active {
  border-color: rgba(107, 211, 255, 0.38);
  background: rgba(107, 211, 255, 0.1);
}

.blog-content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.36fr);
  gap: 24px;
  align-items: start;
}

.blog-feed {
  display: grid;
  gap: 16px;
}

.blog-post-link {
  display: block;
}

.blog-post-card {
  display: grid;
  gap: 12px;
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.025));
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.18);
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.blog-post-link:hover .blog-post-card {
  transform: translateY(-3px);
  border-color: rgba(107, 211, 255, 0.42);
}

.blog-post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

.blog-post-card h2 {
  margin: 0;
  font-size: 28px;
}

.blog-post-card p {
  margin: 0;
}

.blog-guide {
  position: sticky;
  top: 104px;
}

.blog-guide pre {
  max-width: 100%;
  margin: 12px 0;
  padding: 14px;
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(5, 16, 30, 0.72);
}

.blog-guide pre code {
  padding: 0;
  background: transparent;
}

@media (max-width: 980px) {
  .blog-hero-band,
  .blog-content-layout {
    grid-template-columns: 1fr;
  }

  .blog-hero-copy h1 {
    font-size: 44px;
  }

  .blog-guide {
    position: static;
  }
}
</style>
