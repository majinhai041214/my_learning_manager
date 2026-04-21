export default defineNuxtConfig({
  compatibilityDate: '2026-04-20',
  devtools: { enabled: true },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://127.0.0.1:8080'
    }
  },
  app: {
    head: {
      title: 'Majinhai Website',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Personal blog and algorithm study website.' }
      ]
    }
  },
  css: ['~/assets/css/main.css', 'katex/dist/katex.min.css', 'pdfjs-dist/legacy/web/pdf_viewer.css']
})
