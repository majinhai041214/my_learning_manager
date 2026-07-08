# 博客内容维护说明

新版网站的博客内容由服务器运行数据目录驱动，不需要每次新增文章都重新打包前端。

博客页已经提供上传入口，可以直接上传一篇 Markdown 文章，也可以同时选择多张图片一起上传。

## 服务器目录

如果后端服务从 `/opt/www/website/backend` 启动，并使用默认配置，那么博客内容目录为：

```bash
/opt/www/website/uploads/blog/posts
/opt/www/website/uploads/blog/images
```

本地开发时，对应目录通常是：

```text
F:\website\uploads\blog\posts
F:\website\uploads\blog\images
```

## Markdown 文件格式

每篇文章建议使用一个 `.md` 文件，并放到 `blog/posts` 目录。

```md
---
title: "文章标题"
date: 2026-07-03
tags: [项目复盘, 学习]
---

这里写正文内容。

![图片说明](demo/image.png)
```

字段说明：

- `title`：文章标题。
- `date`：文章日期，建议使用 `YYYY-MM-DD`。
- `tags`：文章标签，使用 `[标签1, 标签2]`。
- `slug`：可选。如果不写，系统会使用文件名去掉日期前缀后的部分作为文章地址。

## 图片存储

图片统一放在 `blog/images` 目录。推荐按文章建子目录：

```bash
/opt/www/website/uploads/blog/images/openclaw/step-1.png
```

文章中引用：

```md
![树莓派配置截图](openclaw/step-1.png)
```

也支持旧博客常见写法：

```md
![树莓派配置截图](image/openclaw/step-1.png)
![树莓派配置截图](images/openclaw/step-1.png)
```

后端会统一映射到 `/api/blog/images/...`，部署后不需要在 Markdown 里写完整域名。

## 网页上传图片规则

在博客页上传时：

- 只上传 Markdown：适合纯文字文章，或图片已经提前放到了 `uploads/blog/images`。
- Markdown + 图片一起上传：适合新文章含本地图片的情况。

如果同时上传图片，后端会为这篇文章自动创建一个图片子目录，并尝试把 Markdown 中这些写法改到新目录下：

```md
![图片](demo.png)
![图片](image/demo.png)
![图片](images/demo.png)
```

因此文章里引用的图片文件名需要和上传的图片文件名一致。

## 上线注意

`uploads` 是运行数据目录，更新代码前要备份：

```bash
cp -a /opt/www/website/uploads /opt/www/backups/website-$(date +%F)/uploads
```

新增或修改博客 Markdown 后，一般只需要刷新页面；如果后端服务没有权限读取新文件，再检查目录权限。
