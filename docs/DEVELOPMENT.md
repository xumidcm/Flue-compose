# Development

## 环境要求

- Android Studio
- JDK 17
- Android SDK
- Gradle Wrapper

现代分支目前已经在本地验证过 `assembleRelease`：

- `main`
- `youzipi/fix-1.4`

`classic/beta1.2` 作为历史快照，推荐优先用于参考、回看和做轻量修复。

## 本地构建

常用命令：

```powershell
.\gradlew.bat assembleRelease --no-daemon --console=plain
```

如需调试：

```powershell
.\gradlew.bat installDebug
```

## 签名说明

公开仓库不包含真实签名文件。

如果你需要自行打包签名版，请在本地提供：

- `keystore.properties`
- 或以下环境变量 / Gradle 属性
  - `FLUE_SIGNING_STORE_FILE`
  - `FLUE_SIGNING_STORE_PASSWORD`
  - `FLUE_SIGNING_KEY_ALIAS`
  - `FLUE_SIGNING_KEY_PASSWORD`

没有签名信息时，仓库仍可用于本地构建和开发验证。

## 目录概览

```text
app/
  src/main/kotlin/com/example/wlauncher/
    LauncherActivity.kt
    SettingsActivity.kt
    viewmodel/LauncherViewModel.kt
    ui/
      drawer/
      home/
      notification/
      smartstack/
      controlcenter/
  src/main/java/com/flue/launcher/watchface/
    LunchWatchFace*.kt
```

## 分支定位

- `main`
  - 当前公开最新版。
  - 适合继续整理结构、文档和公开可维护能力。

- `classic/beta1.2`
  - 历史经典线。
  - 适合查旧行为、旧布局和旧版本实现。

- `youzipi/fix-1.4`
  - `beta1.4` 时期的专项修复线。
  - 适合参考那一阶段的性能和快捷交互实现。

## 公开版约束

- 不提交真实签名文件
- 不提交 `.claude`、`.codex`、本地日志和临时备份
- 现代公开分支不再保留旧版 `DingDingCat / 叮叮猫` 表盘能力
