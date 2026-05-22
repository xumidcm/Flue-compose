# Flue-compose

Flue-compose 项目已停止维护，中考结束后将重构 Flue-next。

`Flue-compose` 是一个面向 Android 手表的 Compose 启动器公开版仓库，目标是保留 Flue 的主要桌面、应用列表、通知和表盘体验，同时把适合公开发布的代码、文档和构建配置整理出来。

## 仓库说明

- 本仓库是从私有开发仓库整理出的公开版本。
- 不保留原私有仓库 Git 历史。
- 不包含真实签名文件、私有 Agent 配置、本地调试备份和日志。
- 公开版中的现代分支已移除旧版 `DingDingCat / 叮叮猫` 表盘支持。

## 分支说明

- `main`
  - 当前公开最新版。
  - 基于私有仓库较新的 `beta1.5` 方向整理。
  - 保留现代应用抽屉、通知中心、Smart Stack、内置图片/视频表盘和 `jb_watch` 支持。

- `classic/beta1.2`
  - `main` 主线旧版本的经典快照。
  - 对应历史 `beta1.2` 公开整理版，偏向更早期的主线形态。
  - 主要用于保留旧版结构和旧时期实现参考。

- `youzipi/fix-1.4`
  - 基于 `youzipi/fix-beta1.4-performance-and-shortcuts` 整理出的公开分支。
  - 偏向 `beta1.4` 时期的性能与快捷交互修复线。
  - 同样移除了旧版 `DingDingCat / 叮叮猫` 表盘支持。

## 主要能力

- Android 手表桌面 / 启动器入口
- 蜂窝与列表两种应用抽屉
- Smart Stack / 副一屏
- 通知监听与通知中心
- 内置图片 / 视频表盘
- 外部 `jb_watch` 表盘导入
- 主题、动画、图标与显示适配设置

## 开发文档

- 开发环境、构建方式和目录说明见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)

## 开源协议

本仓库遵循 `GNU General Public License v3.0`。

- 完整协议见 [LICENSE](LICENSE)
- 若你基于本仓库修改并分发，请遵守 GPLv3 的相应要求

## 构建说明

默认使用 Android Studio 或 Gradle 构建：

```powershell
.\gradlew.bat assembleRelease --no-daemon --console=plain
```

若需要发布签名，请自行提供本地签名配置。公开仓库不会包含真实 `keystore` 或密码配置。
