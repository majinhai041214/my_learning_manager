<template>
  <div class="site-shell">
    <div class="top-banner">
      <span>V1 版本开发中</span>
      <p>当前优先完成前端展示、内容结构和部署基础能力。</p>
    </div>

    <header class="site-header">
      <NuxtLink class="brand" to="/">
        <span class="brand-mark">M</span>
        <span class="brand-copy">
          <strong>Majinhai</strong>
          <small>Personal Web Lab</small>
        </span>
      </NuxtLink>
      <nav class="site-nav">
        <NuxtLink to="/">首页</NuxtLink>
        <NuxtLink to="/blog">博客</NuxtLink>
        <NuxtLink to="/algorithms">算法学习</NuxtLink>
        <NuxtLink to="/notes">学习笔记</NuxtLink>
        <NuxtLink to="/about">关于</NuxtLink>
      </nav>
      <div class="site-header-action">
        <a class="button secondary compact" href="mailto:your_email@example.com">联系我</a>
      </div>
    </header>

    <main class="site-main">
      <slot />
    </main>

    <footer class="site-footer">
      <div class="site-footer-grid">
        <div>
          <p class="footer-title">Majinhai Website</p>
          <p>个人博客、算法学习与长期知识沉淀的统一站点。</p>
        </div>
        <div>
          <p class="footer-title">当前建设方向</p>
          <ul class="footer-list">
            <li>内容展示与阅读体验</li>
            <li>算法学习与打卡工作流</li>
            <li>上传、批注与后端能力</li>
          </ul>
        </div>
        <div>
          <p class="footer-title">页面入口</p>
          <div class="footer-links">
            <NuxtLink to="/">首页</NuxtLink>
            <NuxtLink to="/blog">博客</NuxtLink>
            <NuxtLink to="/algorithms">算法学习</NuxtLink>
            <NuxtLink to="/notes">学习笔记</NuxtLink>
            <NuxtLink to="/about">关于</NuxtLink>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>
