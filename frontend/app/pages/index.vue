<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">Personal Website</p>
        <h1>把博客、算法学习和长期内容管理放进同一个清晰的站点里</h1>
        <p class="hero-text">
          这个新版网站不只是博客重做，而是一次完整的个人内容系统升级。
          它会同时承载你的文章、算法学习过程、每日记录、笔记与代码沉淀，
          并逐步演进成真正可持续维护的个人平台。
        </p>
        <div class="hero-actions">
          <NuxtLink class="button primary" to="/algorithms">进入算法学习区</NuxtLink>
          <NuxtLink class="button secondary" to="/blog">查看博客模块</NuxtLink>
        </div>
      </div>

      <div class="hero-panel">
        <article class="hero-card spotlight-card">
          <p>当前阶段</p>
          <strong>前端基础界面搭建中</strong>
          <ul>
            <li>首页叙事与导航结构</li>
            <li>博客模块展示页</li>
            <li>算法学习页结构化呈现</li>
          </ul>
        </article>

        <article class="hero-card status-card">
          <span class="mini-label">当前站点方向</span>
          <h2>内容站 + 学习工作台</h2>
          <p>
            先做一版可上线、可展示、可继续扩展的前端界面，
            后续再逐步接入上传、笔记和批注能力。
          </p>
        </article>
      </div>
    </section>

    <section class="section-block section-grid two-columns">
      <article class="content-card">
        <p class="eyebrow">Why Rebuild</p>
        <h2>为什么要新建这一版网站</h2>
        <p>
          旧博客更偏静态展示，而现在你需要的是一个能承载长期积累的系统。
          除了文章内容，你还希望把算法学习、打卡记录、笔记、代码和批注统一起来，
          这决定了新版网站需要从结构上就考虑扩展性。
        </p>
      </article>

      <article class="content-card metric-card">
        <div class="metric-row">
          <div>
            <strong>3</strong>
            <span>核心模块</span>
          </div>
          <div>
            <strong>1</strong>
            <span>统一工作区</span>
          </div>
          <div>
            <strong>V1</strong>
            <span>正在搭建</span>
          </div>
        </div>
      </article>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <p class="eyebrow">Core Modules</p>
        <h2>新版站点会由三部分组成</h2>
      </div>

      <div class="feature-grid">
        <article class="feature-card">
          <span class="feature-index">01</span>
          <h3>个人博客</h3>
          <p>发布研究记录、学习文章、项目总结和长期写作内容，形成稳定的个人表达出口。</p>
        </article>
        <article class="feature-card">
          <span class="feature-index">02</span>
          <h3>算法学习区</h3>
          <p>承载路线图、刷题记录、专题笔记与代码沉淀，让算法学习过程具备长期可复用性。</p>
        </article>
        <article class="feature-card">
          <span class="feature-index">03</span>
          <h3>内容管理能力</h3>
          <p>后续会逐步加入上传、笔记、批注与简单后台能力，把个人站从展示站升级成工作台。</p>
        </article>
      </div>
    </section>

    <section class="section-block section-grid two-columns">
      <article class="content-card roadmap-card">
        <p class="eyebrow">Roadmap</p>
        <h2>当前开发顺序</h2>
        <ol class="roadmap-list">
          <li>先把前端页面搭成可以展示的第一版</li>
          <li>再补后端基础接口与上传能力</li>
          <li>最后完成服务器部署、备案与正式上线</li>
        </ol>
      </article>

      <article class="content-card">
        <p class="eyebrow">Current Stack</p>
        <h2>当前技术栈</h2>
        <ul class="stack-list">
          <li>Frontend: Nuxt</li>
          <li>Backend: Spring Boot</li>
          <li>Deploy: Caddy + 部署脚本</li>
        </ul>
      </article>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <p class="eyebrow">Highlighted Areas</p>
        <h2>第一版前端会优先把这几块打磨出来</h2>
      </div>

      <div class="split-showcase">
        <article class="showcase-card">
          <div class="showcase-copy">
            <p class="eyebrow">Blog Area</p>
            <h3>博客区负责稳定输出</h3>
            <p>
              它会承载更完整的文章、研究记录和项目复盘，是网站里长期价值最高的公开内容层。
            </p>
            <NuxtLink class="button secondary" to="/blog">查看博客模块</NuxtLink>
          </div>
        </article>

        <article class="showcase-card accent-card">
          <div class="showcase-copy">
            <p class="eyebrow">Algorithm Area</p>
            <h3>算法学习区负责过程沉淀</h3>
            <p>
              这里会把打卡、专题路线、笔记与代码管理整合起来，逐步形成你自己的学习工作台。
            </p>
            <NuxtLink class="button primary" to="/algorithms">进入算法学习区</NuxtLink>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
