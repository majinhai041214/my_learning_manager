import katex from 'katex'

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function renderPlainInline(value: string) {
  return escapeHtml(value)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}

function renderMathExpression(value: string, displayMode = false) {
  const expression = value.trim()

  if (!expression) {
    return ''
  }

  try {
    return katex.renderToString(expression, {
      displayMode,
      throwOnError: false,
      strict: 'ignore',
      output: 'html'
    })
  } catch {
    return `<span class="math-expression">${escapeHtml(expression)}</span>`
  }
}

function renderInlineMarkdown(value: string) {
  const pattern = /(`[^`]+`|\$\$[\s\S]+?\$\$|\$[^$\n]+\$|\[[^\]]+\]\(([^)]+)\))/g
  let result = ''
  let cursor = 0
  let match: RegExpExecArray | null

  while ((match = pattern.exec(value)) !== null) {
    result += renderPlainInline(value.slice(cursor, match.index))

    const token = match[0]
    if (token.startsWith('`')) {
      result += `<code>${escapeHtml(token.slice(1, -1))}</code>`
    } else if (token.startsWith('$$')) {
      result += `<span class="math-inline">${renderMathExpression(token.slice(2, -2), false)}</span>`
    } else if (token.startsWith('$')) {
      result += `<span class="math-inline">${renderMathExpression(token.slice(1, -1), false)}</span>`
    } else if (token.startsWith('[')) {
      const linkMatch = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/)
      if (linkMatch) {
        result += `<a href="${escapeHtml(linkMatch[2])}" target="_blank" rel="noreferrer">${renderPlainInline(linkMatch[1])}</a>`
      } else {
        result += renderPlainInline(token)
      }
    }

    cursor = match.index + token.length
  }

  result += renderPlainInline(value.slice(cursor))
  return result
}

function wrapToken(content: string, className: string) {
  return `<span class="${className}">${escapeHtml(content)}</span>`
}

function highlightCode(code: string, extension: string) {
  const language = extension.toLowerCase()

  const keywordGroups: Record<string, string[]> = {
    python: ['def', 'class', 'return', 'if', 'elif', 'else', 'for', 'while', 'import', 'from', 'as', 'try', 'except', 'with', 'lambda', 'yield', 'None', 'True', 'False'],
    java: ['public', 'private', 'protected', 'class', 'interface', 'enum', 'return', 'if', 'else', 'for', 'while', 'switch', 'case', 'break', 'new', 'static', 'void', 'extends', 'implements', 'null', 'true', 'false'],
    cpp: ['int', 'long', 'double', 'float', 'bool', 'void', 'auto', 'class', 'struct', 'return', 'if', 'else', 'for', 'while', 'switch', 'case', 'break', 'continue', 'const', 'using', 'namespace', 'include', 'nullptr', 'true', 'false'],
    javascript: ['const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'switch', 'case', 'break', 'new', 'class', 'extends', 'import', 'from', 'export', 'async', 'await', 'true', 'false', 'null', 'undefined'],
    typescript: ['const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'switch', 'case', 'break', 'new', 'class', 'extends', 'import', 'from', 'export', 'async', 'await', 'true', 'false', 'null', 'undefined', 'type', 'interface', 'implements']
  }

  const languageKey = (
    ['py'].includes(language) ? 'python'
      : ['cpp', 'cc', 'cxx', 'c', 'h', 'hpp'].includes(language) ? 'cpp'
        : ['js'].includes(language) ? 'javascript'
          : ['ts'].includes(language) ? 'typescript'
            : language
  )

  const keywords = keywordGroups[languageKey] ?? []
  const keywordSet = new Set(keywords)

  let index = 0
  let html = ''

  while (index < code.length) {
    const current = code[index]
    const next = code[index + 1] ?? ''

    if (languageKey === 'python' && current === '#') {
      let end = index
      while (end < code.length && code[end] !== '\n') {
        end += 1
      }
      html += wrapToken(code.slice(index, end), 'token-comment')
      index = end
      continue
    }

    if (languageKey !== 'python' && current === '/' && next === '/') {
      let end = index
      while (end < code.length && code[end] !== '\n') {
        end += 1
      }
      html += wrapToken(code.slice(index, end), 'token-comment')
      index = end
      continue
    }

    if (languageKey !== 'python' && current === '/' && next === '*') {
      let end = index + 2
      while (end < code.length - 1 && !(code[end] === '*' && code[end + 1] === '/')) {
        end += 1
      }
      end = Math.min(code.length, end + 2)
      html += wrapToken(code.slice(index, end), 'token-comment')
      index = end
      continue
    }

    if (current === '"' || current === "'" || current === '`') {
      const quote = current
      let end = index + 1
      while (end < code.length) {
        if (code[end] === '\\') {
          end += 2
          continue
        }
        if (code[end] === quote) {
          end += 1
          break
        }
        end += 1
      }
      html += wrapToken(code.slice(index, end), 'token-string')
      index = end
      continue
    }

    if (/\d/.test(current)) {
      let end = index + 1
      while (end < code.length && /[\d.]/.test(code[end])) {
        end += 1
      }
      html += wrapToken(code.slice(index, end), 'token-number')
      index = end
      continue
    }

    if (/[A-Za-z_]/.test(current)) {
      let end = index + 1
      while (end < code.length && /[A-Za-z0-9_]/.test(code[end])) {
        end += 1
      }
      const word = code.slice(index, end)
      html += keywordSet.has(word) ? wrapToken(word, 'token-keyword') : escapeHtml(word)
      index = end
      continue
    }

    html += escapeHtml(current)
    index += 1
  }

  return html
}

export function renderMarkdownToHtml(markdown: string) {
  const lines = markdown.replaceAll('\r\n', '\n').split('\n')
  const blocks: string[] = []
  let index = 0
  let inCodeBlock = false
  let inMathBlock = false
  let codeLanguage = ''
  let codeBuffer: string[] = []
  let mathBuffer: string[] = []
  let listBuffer: string[] = []

  function flushList() {
    if (!listBuffer.length) {
      return
    }
    blocks.push(`<ul>${listBuffer.map(item => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ul>`)
    listBuffer = []
  }

  function flushCodeBlock() {
    if (!codeBuffer.length && !inCodeBlock) {
      return
    }
    const languageLabel = codeLanguage || 'text'
    blocks.push(
      `<pre class="markdown-code-block"><code class="language-${languageLabel}">${highlightCode(codeBuffer.join('\n'), languageLabel)}</code></pre>`
    )
    codeBuffer = []
    codeLanguage = ''
  }

  function flushMathBlock() {
    if (!mathBuffer.length && !inMathBlock) {
      return
    }
    blocks.push(
      `<div class="markdown-math-block">${renderMathExpression(mathBuffer.join('\n'), true)}</div>`
    )
    mathBuffer = []
  }

  while (index < lines.length) {
    const line = lines[index]

    if (line.trimStart().startsWith('```')) {
      if (inCodeBlock) {
        flushCodeBlock()
        inCodeBlock = false
      } else {
        flushList()
        inCodeBlock = true
        codeLanguage = line.trimStart().slice(3).trim().toLowerCase()
        codeBuffer = []
      }
      index += 1
      continue
    }

    if (line.trim() === '$$') {
      if (inMathBlock) {
        flushMathBlock()
        inMathBlock = false
      } else {
        flushList()
        inMathBlock = true
        mathBuffer = []
      }
      index += 1
      continue
    }

    if (inCodeBlock) {
      codeBuffer.push(line)
      index += 1
      continue
    }

    if (inMathBlock) {
      mathBuffer.push(line)
      index += 1
      continue
    }

    if (!line.trim()) {
      flushList()
      index += 1
      continue
    }

    if (/^[-*]\s+/.test(line)) {
      listBuffer.push(line.replace(/^[-*]\s+/, ''))
      index += 1
      continue
    }

    flushList()

    const headingMatch = line.match(/^(#{1,4})\s+(.*)$/)
    if (headingMatch) {
      const level = headingMatch[1].length
      blocks.push(`<h${level + 1}>${renderInlineMarkdown(headingMatch[2])}</h${level + 1}>`)
      index += 1
      continue
    }

    if (line.startsWith('> ')) {
      blocks.push(`<blockquote>${renderInlineMarkdown(line.slice(2))}</blockquote>`)
      index += 1
      continue
    }

    blocks.push(`<p>${renderInlineMarkdown(line)}</p>`)
    index += 1
  }

  flushList()
  if (inCodeBlock) {
    flushCodeBlock()
  }
  if (inMathBlock) {
    flushMathBlock()
  }

  return blocks.join('')
}

export function renderCodeToHtml(code: string, extension: string) {
  return `<pre class="markdown-code-block"><code class="language-${extension.toLowerCase()}">${highlightCode(code, extension)}</code></pre>`
}

export function escapePlainText(text: string) {
  return escapeHtml(text)
}
