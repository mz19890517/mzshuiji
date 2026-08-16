# 我的记事本 (Notebook)

基于 [Quillpad](https://github.com/quillpad/quillpad) 的开源 Markdown 记事本应用,供个人修改使用。

## 功能

- 支持 Markdown 的笔记
- 任务清单 / 置顶 / 隐藏笔记
- 提醒、语音录音与文件附件
- 笔记本分组、标签、归档、搜索
- 文件同步、Nextcloud 同步(实验性)、ZIP 备份还原
- 明暗模式与多种配色方案

## 构建

仓库配置了 GitHub Actions,推送到 `main` 分支后自动构建 APK:

- 在仓库的 **Actions** 页面查看构建状态
- 构建完成后到对应运行记录里下载 `quillpad-debug` 构件

debug 版本使用调试签名,可直接安装测试。

## 数据与许可

- 数据通过本地数据库存储,可导出备份
- 上游代码采用 GPL-3.0 许可(见 LICENSE)
