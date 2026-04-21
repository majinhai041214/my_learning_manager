<template>
  <div class="page-stack">
    <AlgoCheckinCalendar />
    <AlgorithmCheckinPanel />

    <section class="page-hero">
      <p class="eyebrow">Algorithms</p>
      <h1>算法学习模块会从“内容展示”进化成“学习工作台”</h1>
      <p>
        这里不只是一个专题页，而是你后续打卡、上传笔记、上传代码、
        整理专题路线和写批注的核心区域。现在已经接入了第一版真实打卡能力，后面继续把笔记和代码管理补齐。
      </p>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <p class="eyebrow">Learning Structure</p>
        <h2>算法学习区的主结构</h2>
      </div>

      <div class="feature-grid">
        <article class="feature-card">
          <span class="feature-index">01</span>
          <h3>学习路线图</h3>
          <p>按基础数据结构、搜索、动态规划、图论等方向逐步组织你的算法学习顺序。</p>
          <div class="hero-actions">
            <NuxtLink class="button primary compact" to="/algorithms/roadmap">查看路线图</NuxtLink>
          </div>
        </article>
        <article class="feature-card">
          <span class="feature-index">02</span>
          <h3>每日打卡</h3>
          <p>记录每天学了什么、刷了哪些题、遇到了什么问题，并保留连续学习痕迹。</p>
        </article>
        <article class="feature-card">
          <span class="feature-index">03</span>
          <h3>笔记与代码沉淀</h3>
          <p>把 Markdown 笔记和题解代码归档到统一结构下，而不是散落在不同目录里。</p>
        </article>
      </div>
    </section>

    <section class="section-block section-grid two-columns">
      <article class="content-card">
        <p class="eyebrow">Planned Workflow</p>
        <h2>你后续在这里的使用方式</h2>
        <ol class="roadmap-list">
          <li>先完成每日打卡，记录主题和摘要</li>
          <li>从学习笔记页上传 Markdown、PDF 和常见代码文件</li>
          <li>进入独立的学习笔记页查看、整理和后续批注</li>
        </ol>
      </article>

      <article class="content-card">
        <p class="eyebrow">Mind Map</p>
        <h2>路线图会切到独立的思维导图页面</h2>
        <p>
          算法学习页继续聚焦在学习工作流和打卡能力，专题路线图则单独展开成更适合浏览的导图页面，
          让你能像看 XMind 一样按主题、子主题和刷题方向快速定位。
        </p>
        <div class="hero-actions">
          <NuxtLink class="button primary" to="/algorithms/roadmap">进入学习路线图</NuxtLink>
        </div>
      </article>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <p class="eyebrow">Topic Map</p>
        <h2>算法学习区会优先组织这些专题</h2>
      </div>

      <div class="topic-map-grid">
        <article class="topic-pill-card">
          <strong>基础数据结构</strong>
          <span>数组、链表、栈、队列、哈希</span>
        </article>
        <article class="topic-pill-card">
          <strong>搜索与回溯</strong>
          <span>DFS、BFS、递归搜索、剪枝</span>
        </article>
        <article class="topic-pill-card">
          <strong>动态规划</strong>
          <span>状态设计、转移优化、经典模型</span>
        </article>
        <article class="topic-pill-card">
          <strong>图论</strong>
          <span>最短路、拓扑排序、连通性问题</span>
        </article>
        <article class="topic-pill-card">
          <strong>字符串</strong>
          <span>KMP、Trie、哈希与模式匹配</span>
        </article>
        <article class="topic-pill-card">
          <strong>数学与技巧</strong>
          <span>前缀和、差分、位运算、构造</span>
        </article>
      </div>
    </section>
  </div>
</template>
