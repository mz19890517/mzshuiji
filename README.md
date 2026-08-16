# 我的记事本 (Notebook)

一个简单轻量的 Android 记事本应用。

## 功能

- 笔记列表,按更新时间倒序排列
- 新建 / 编辑 / 删除笔记
- 笔记包含标题、正文与更新时间
- 数据通过 SQLite 本地持久化,无需网络

## 技术

- Kotlin + Material 3,单 Activity + RecyclerView 列表
- 原生 SQLite (SQLiteOpenHelper),无额外数据库依赖
- minSdk 24 (Android 7.0),targetSdk / compileSdk 35

## 构建

仓库配置了 GitHub Actions,推送到 `main` 分支后自动构建 APK:

- 在仓库的 **Actions** 页面查看构建状态
- 构建完成后到对应运行记录里下载 `notebook-debug` / `notebook-release` 构件

release 版本使用 debug 签名,可直接安装测试。
