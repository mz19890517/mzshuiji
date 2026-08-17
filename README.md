# MZ瞬记

基于 [Quillpad](https://github.com/nicholasgasior/quillpad) 开源笔记应用深度定制的图文混排记事本。

## 功能特性

### 图文混排
- 笔记内容支持文字与附件混排显示
- 编辑态显示 Markdown marker，阅读态渲染为可视化卡片

### 附件交互（参考小米笔记）
| 操作 | 功能 |
|------|------|
| 单击 | 弹出菜单（编辑描述/删除/分享/上移/下移） |
| 双击 | 预览/打开附件 |
| 长按拖拽 | 拖动附件调整位置 |

### 其他功能
- 音频播放条（播放/暂停 + 进度条 + 文件名）
- 任意格式文件附件（图片/视频/音频/PDF/文档等）
- 外部分享支持（新建笔记或插入已有笔记）
- Markdown 支持、标签系统、笔记颜色标记
- 固定 Debug 签名，覆盖安装无需卸载

## 安装

前往 [Releases](https://github.com/mz19890517/mzshuiji/releases) 下载最新 APK。

## 构建

推送到 `main` 分支后 GitHub Actions 自动构建 APK。

### 本地构建

签名文件：`mzshunji-keystore.zip`（密码私有）

## 技术信息

| 项目 | 说明 |
|------|------|
| 包名 | `com.mz.shunji` |
| 版本 | `1.1.0` |
| 最低SDK | Android 7.0 (API 24) |
| 基座 | Quillpad (MIT License) |

## 许可

上游代码采用 GPL-3.0 许可（见 LICENSE）
