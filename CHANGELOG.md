# Changelog

本文档记录当前 PR `#19: Splice old side screen into main` 相对 `main` 分支的主要更新内容。

更新时间：2026-04-27

## 当前版本

- 版本名：`beta1.3`
- 版本号：`13`
- 当前 PR：`#19`
- 当前分支：`codex/splice-old-side-screen`

## 总览

这次 PR 不是单点修复，而是一次较大规模的功能拼接与交互整理，核心目标是把旧版副一屏能力重新整合进现有主线，同时把通知中心、表盘、设置页和视觉细节一起补齐。

相对 `main`，当前 PR 累计涉及：

- 46 个文件变更
- 约 2900+ 行新增
- 约 1100+ 行删除

## 主要更新

### 1. 副一屏重新整合回主线

- 重新接入完整副一屏场景
- 增加快捷应用槽位
- 增加通知预览聚合展示
- 补齐副一屏与首页之间的过渡和状态同步
- 修复副一屏方向、堆叠和退出行为

相关文件：

- [`SmartStackLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/smartstack/SmartStackLayer.kt)
- [`LauncherActivity.kt`](app/src/main/kotlin/com/example/wlauncher/LauncherActivity.kt)
- [`GestureHost.kt`](app/src/main/kotlin/com/example/wlauncher/ui/navigation/GestureHost.kt)
- [`LauncherViewModel.kt`](app/src/main/kotlin/com/example/wlauncher/viewmodel/LauncherViewModel.kt)

### 2. 通知中心重做与通知监听增强

- 重构通知列表与通知分组模型
- 加入通知监听服务数据清洗与同步
- 支持副一屏预览到通知中心的连续操作
- 支持单条通知与整组通知滑动删除
- 优化通知卡片堆叠、展开、删除与进入动画
- 兼容部分 Android 版本的通知 API 差异

相关文件：

- [`WLauncherNotificationListener.kt`](app/src/main/kotlin/com/example/wlauncher/service/WLauncherNotificationListener.kt)
- [`NotificationLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationLayer.kt)
- [`NotificationModels.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationModels.kt)
- [`SwipeRevealDeleteContainer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/SwipeRevealDeleteContainer.kt)

### 3. 表盘能力增强

- 增加图片表盘缓存与降采样
- 优化内置图片表盘裁剪
- 修复表盘图片缓存导入问题
- 强化表盘设置与资源存储流程
- 保持内置表盘与扫描表盘切换体验一致

相关文件：

- [`InternalWatchFaceStorage.kt`](app/src/main/kotlin/com/example/wlauncher/watchface/InternalWatchFaceStorage.kt)
- [`WatchFaceLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/home/WatchFaceLayer.kt)
- [`LauncherViewModel.kt`](app/src/main/kotlin/com/example/wlauncher/viewmodel/LauncherViewModel.kt)

### 4. 设置页与配置项扩展

- 调整设置页文案
- 继续收敛设置项结构
- 增强图标包、隐藏应用、性能与表盘相关设置
- 优化关于页图标显示与相关资源
- 刷新启动器图标与设置入口图标

相关文件：

- [`SettingsActivity.kt`](app/src/main/kotlin/com/example/wlauncher/SettingsActivity.kt)
- [`WatchFaceSettingCard.kt`](app/src/main/kotlin/com/example/wlauncher/ui/settings/WatchFaceSettingCard.kt)
- [`app/src/main/res/mipmap-mdpi/ic_launcher.png`](app/src/main/res/mipmap-mdpi/ic_launcher.png)

### 5. 动画、回弹与返回手势优化

- 修复通知中心跟手动画
- 调整通知拖拽灵敏度与速度阈值
- 优化返回桌面、启动应用后的回切动画
- 统一副一屏与通知中心中的回弹表现
- 改善顶部橡皮筋体验

相关文件：

- [`LauncherActivity.kt`](app/src/main/kotlin/com/example/wlauncher/LauncherActivity.kt)
- [`NotificationLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationLayer.kt)
- [`SmartStackLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/smartstack/SmartStackLayer.kt)
- [`ScaleBlurModifier.kt`](app/src/main/kotlin/com/example/wlauncher/ui/anim/ScaleBlurModifier.kt)

## 本次最新补充修复

以下内容是 `beta1.3 (13)` 相对上一版补进去的行为修正与功能增强：

### 表盘与主题色增强

- 保留原 AnalogClock 并重命名为“拟真时钟”
- 新增 MD3E 时钟样式，支持官方 Material Shapes 表盘形状选择
- 新增表盘文字颜色设置：可使用表盘主题色，也可通过 R/G/B 滑块手动混色
- 拟真时钟会将手动颜色应用到整个钟表
- MD3E 时钟支持自动从表盘取色，或分别设置文字、表盘、时针、分针、秒针颜色
- 视频表盘自动取色会从第一帧采样
- 应用列表背景与应用项接入表盘主色调，并根据系统深色/浅色模式生成同色调深/浅背景

相关文件：

- [`WatchFaceLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/home/WatchFaceLayer.kt)
- [`InternalWatchFaceConfigActivity.kt`](app/src/main/kotlin/com/example/wlauncher/InternalWatchFaceConfigActivity.kt)
- [`LauncherActivity.kt`](app/src/main/kotlin/com/example/wlauncher/LauncherActivity.kt)
- [`AppListTheme.kt`](app/src/main/kotlin/com/example/wlauncher/ui/drawer/AppListTheme.kt)
- [`HoneycombScreen.kt`](app/src/main/kotlin/com/example/wlauncher/ui/drawer/HoneycombScreen.kt)
- [`ListDrawerScreen.kt`](app/src/main/kotlin/com/example/wlauncher/ui/drawer/ListDrawerScreen.kt)

### 应用列表与图标增强

- 新增鱼眼模式、快速流逝动画、双色图标相关选项
- 快速流逝动画改为只按屏幕 Y 位置计算，并优化中间慢区逻辑以降低抖动
- 列表布局补齐快速流逝动画与无回弹 overscroll
- 双色图标对非 AdaptiveIcon 路径进行灰度后二值化，并保持黑底白前景倾向
- 应用栏恢复可排序的内置设置入口，且不依赖系统扫描出的 SettingsActivity

相关文件：

- [`AppRepository.kt`](app/src/main/kotlin/com/example/wlauncher/data/repository/AppRepository.kt)
- [`LauncherViewModel.kt`](app/src/main/kotlin/com/example/wlauncher/viewmodel/LauncherViewModel.kt)
- [`HoneycombScreen.kt`](app/src/main/kotlin/com/example/wlauncher/ui/drawer/HoneycombScreen.kt)
- [`ListDrawerScreen.kt`](app/src/main/kotlin/com/example/wlauncher/ui/drawer/ListDrawerScreen.kt)

### 副一屏电池与步数

- 副一屏底部电池胶囊放大
- 新增传感器计步显示与“显示步数”开关
- 步数增加图标，电池与步数组合整体保持居中
- 权限拒绝或传感器不可用时使用受控占位，不影响界面稳定

相关文件：

- [`SmartStackLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/smartstack/SmartStackLayer.kt)
- [`LauncherViewModel.kt`](app/src/main/kotlin/com/example/wlauncher/viewmodel/LauncherViewModel.kt)
- [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml)

### Warning 与 Deprecation 清理

- 清理 Kotlin 编译 warning 和多处 Android/Compose deprecation
- 修复通知层时间格式空安全问题
- 更新步数图标为 AutoMirrored Material 图标，消除 deprecation warning

相关文件：

- [`NotificationLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationLayer.kt)
- [`WLauncherNotificationListener.kt`](app/src/main/kotlin/com/example/wlauncher/service/WLauncherNotificationListener.kt)
- [`SettingsActivity.kt`](app/src/main/kotlin/com/example/wlauncher/SettingsActivity.kt)
- [`SmartStackLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/smartstack/SmartStackLayer.kt)

以下内容是此前补进去的行为修正：

### 电池胶囊统一

- 把通知中心和副一屏底部电池胶囊抽成统一组件
- 修正电量百分比文字垂直居中偏下的问题
- 统一两处胶囊宽度、内层尺寸与圆角
- 充电闪电图标移到胶囊外侧显示，不再撑宽背景

相关文件：

- [`WatchBatteryPill.kt`](app/src/main/kotlin/com/example/wlauncher/ui/common/WatchBatteryPill.kt)
- [`NotificationLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationLayer.kt)
- [`SmartStackLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/smartstack/SmartStackLayer.kt)

### 通知下拉返回逻辑修正

- 修复通知列表在“先上滑再下拉”时仍可能直接返回副一屏的问题
- 现在只有列表已经回到顶部且静止后，新的一次下拉手势才会触发返回
- 当列表只是刚回到顶部时，会优先触发顶部橡皮筋效果

相关文件：

- [`NotificationLayer.kt`](app/src/main/kotlin/com/example/wlauncher/ui/notification/NotificationLayer.kt)

## CI 状态

当前这轮修复提交对应的 GitHub Actions 已完成并成功产出 Release APK：

- [PR 构建运行 24598848670](https://github.com/xumidcm/Flue/actions/runs/24598848670)
- [Push 构建运行 24598847621](https://github.com/xumidcm/Flue/actions/runs/24598847621)

## 备注

- 本文档描述的是“当前 PR 相对 `main` 的累计变化”，不是历史 release 的逐版本 changelog
- 如果后续继续在这个 PR 上追加功能，建议直接在本文件追加新小节，或在 PR 合并后再拆成正式版本日志
