<script setup lang="ts">
import { renderMarkdownToHtml } from '~/utils/notePreview'

interface BlogPostDetail {
  slug: string
  title: string
  date: string | null
  tags: string[]
  excerpt: string
  content: string
  sourceFilename: string
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

const route = useRoute()
const runtimeConfig = useRuntimeConfig()
const apiBase = runtimeConfig.public.apiBase
const slug = computed(() => {
  const value = route.params.slug
  return Array.isArray(value) ? value.join('/') : String(value ?? '')
})

const { data: response, pending, error } = await useFetch<ApiResponse<BlogPostDetail>>(
  () => `/blog/posts/${slug.value}`,
  {
    baseURL: apiBase
  }
)

const post = computed(() => response.value?.data ?? null)
const renderedContent = computed(() => post.value ? renderMarkdownToHtml(post.value.content) : '')

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '未设置日期'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(new Date(value))
}
</script>

<template>
  <div class="blog-detail-page">
    <NuxtLink class="blog-back-link" to="/blog">返回博客列表</NuxtLink>

    <article v-if="post" class="blog-article-shell">
      <header class="blog-article-header">
        <p class="eyebrow">Blog Post</p>
        <h1>{{ post.title }}</h1>
        <div class="blog-article-meta">
          <time>{{ formatDate(post.date) }}</time>
          <span>{{ post.sourceFilename }}</span>
        </div>
        <div v-if="post.tags.length" class="blog-article-tags">
          <span v-for="tag in post.tags" :key="tag">#{{ tag }}</span>
        </div>
      </header>

      <div class="blog-article-content" v-html="renderedContent"></div>
    </article>

    <section v-else class="blog-state-panel">
      <p class="eyebrow">{{ pending ? 'Loading' : 'Unavailable' }}</p>
      <h1>{{ pending ? '正在读取文章' : '暂时无法读取这篇文章' }}</h1>
      <p>
        {{ error ? '请确认后端服务已经启动，或者这篇文章仍存在于博客 Markdown 目录。' : '文章内容马上就来。' }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.blog-detail-page {
  display: grid;
  gap: 22px;
  width: min(980px, 100%);
  margin: 0 auto;
}

.blog-back-link {
  width: fit-content;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--muted);
}

.blog-article-shell,
.blog-state-panel {
  padding: 30px;
  border: 1px solid var(--border);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.065), rgba(255, 255, 255, 0.025));
}

.blog-article-header {
  display: grid;
  gap: 14px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}

.blog-article-header h1,
.blog-state-panel h1 {
  margin: 0;
  font-size: 46px;
  line-height: 1.12;
}

.blog-article-meta,
.blog-article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.blog-article-meta {
  color: var(--muted);
  font-size: 14px;
}

.blog-article-tags span {
  display: inline-flex;
  padding: 7px 11px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(107, 211, 255, 0.08);
  color: #dff6ff;
  font-size: 13px;
}

.blog-article-content {
  padding-top: 26px;
  color: var(--text);
}

.blog-article-content :deep(h2),
.blog-article-content :deep(h3),
.blog-article-content :deep(h4),
.blog-article-content :deep(h5) {
  margin: 32px 0 14px;
  line-height: 1.35;
}

.blog-article-content :deep(h2) {
  font-size: 30px;
}

.blog-article-content :deep(h3) {
  font-size: 24px;
}

.blog-article-content :deep(p),
.blog-article-content :deep(li),
.blog-article-content :deep(blockquote),
.blog-state-panel p {
  color: var(--muted);
  line-height: 1.9;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.blog-article-content :deep(p),
.blog-article-content :deep(ul),
.blog-article-content :deep(blockquote),
.blog-article-content :deep(pre),
.blog-article-content :deep(.markdown-table-wrap),
.blog-article-content :deep(.markdown-math-block) {
  margin: 0 0 18px;
}

.blog-article-content :deep(ul) {
  padding-left: 22px;
}

.blog-article-content :deep(blockquote) {
  padding-left: 16px;
  border-left: 3px solid rgba(107, 211, 255, 0.42);
}

.blog-article-content :deep(a) {
  color: var(--primary);
}

.blog-article-content :deep(code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
  font-family: Consolas, "Courier New", monospace;
}

.blog-article-content :deep(.markdown-code-block) {
  max-width: 100%;
  overflow-x: auto;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(5, 16, 30, 0.75);
}

.blog-article-content :deep(.markdown-code-block code) {
  display: block;
  padding: 0;
  background: transparent;
  line-height: 1.7;
  white-space: pre;
}

.blog-article-content :deep(.markdown-image) {
  display: block;
  max-width: min(100%, 760px);
  max-height: 720px;
  margin: 22px auto;
  border: 1px solid var(--border);
  border-radius: 18px;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.04);
}

.blog-article-content :deep(.markdown-table-wrap) {
  max-width: 100%;
  overflow-x: auto;
}

.blog-article-content :deep(.markdown-table) {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
}

.blog-article-content :deep(.markdown-table th),
.blog-article-content :deep(.markdown-table td) {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  line-height: 1.75;
}

.blog-article-content :deep(.markdown-table th) {
  background: rgba(107, 211, 255, 0.08);
}

@media (max-width: 720px) {
  .blog-article-shell,
  .blog-state-panel {
    padding: 22px;
  }

  .blog-article-header h1,
  .blog-state-panel h1 {
    font-size: 34px;
  }
}
</style>
