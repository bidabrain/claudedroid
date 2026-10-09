<p align="center">
  <img src="app/src/main/res/drawable-nodpi/app_icon_full.png" width="128" alt="ClaudeDroid icon" />
</p>

# ClaudeDroid

ClaudeDroid 是一个原生 Android AI Agent 应用（Kotlin + Jetpack Compose + Material 3），由 **Claude** 驱动。
它在手机里内置一个 Linux 沙盒，Agent 可以执行命令、读写文件、浏览网页、调用工具、在授权后操作手机界面——
而且每一步操作都以可折叠的活动步骤显示在对话中，你随时可以查看、打断或调整方向。

界面支持 **简体中文** 和英文，跟随系统语言自动切换（Android 13+ 也可在系统设置里单独为本应用选择语言）。

## 模型与 API

本项目**只支持 Claude**，模型配置在编译时固定，应用内没有模型 / 服务商 / API Key 设置项。

- 接口：Anthropic 的 OpenAI 兼容接口 `https://api.anthropic.com/v1`（`/chat/completions`，支持流式输出和工具调用）
- 默认模型：`claude-sonnet-5-5`
- 配置来源：项目根目录的 `local.properties`（已加入 `.gitignore`，不会被提交）

## 快速开始

### 1. 环境要求

- **JDK 21**（注意：Android Studio 自带的 JBR 25 与当前 Gradle 8.14 不兼容）
- Android SDK（compileSdk 36）
- Android 8.0+（API 26+）的真机或模拟器，以及 ADB

### 2. 配置 `local.properties`

复制示例文件并填写：

```bash
cp local.properties.example local.properties
```

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
LLM_PROVIDER=custom
LLM_BASE_URL=https://api.anthropic.com/v1
LLM_MODEL=claude-sonnet-5-5
LLM_API_KEY=sk-ant-...        # 你的 Anthropic API Key
GOOGLE_CLIENT_SECRET=         # 可选：Google 集成用
```

可选的第三方集成密钥（不填则对应功能不可用）：

```properties
OPENAI_REALTIME_API_KEY=
GITHUB_OAUTH_CLIENT_ID=
GITHUB_OAUTH_CLIENT_SECRET=
GITHUB_OAUTH_TOKEN=
NOTION_OAUTH_CLIENT_ID=
NOTION_OAUTH_CLIENT_SECRET=
SPOTIFY_OAUTH_CLIENT_ID=
SPOTIFY_OAUTH_CLIENT_SECRET=
```

> ⚠️ API Key 会被编译进 APK，拿到 APK 的人可以提取出来。请勿分发你自己编译的 APK。

### 3. 编译与安装

```bash
./gradlew assembleDebug      # 生成 app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # 安装到已连接的设备
```

Windows PowerShell 下先指定 JDK 21：

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"; .\gradlew.bat installDebug
```

默认只打包 `arm64-v8a` 的 Sherpa 原生库（适合真机）；如需模拟器 ABI，请在 `app/build.gradle.kts` 中添加。

## 功能

- Claude 流式对话 + 工具调用，活动步骤可折叠查看
- 终端 / 进程工具：执行命令、后台进程、文件读写、网页工具
- 通过无障碍服务（AccessibilityService）读取界面并点击、滑动、输入；MediaProjection 截屏兜底
- MCP 客户端，可接入 stdio 外部工具服务器
- Google、GitHub、Notion、Spotify 等 OAuth 集成
- WhatsApp、短信、通知监听等渠道（需审批的回复）
- Agent 工作区文件：`AGENTS.md`、`SOUL.md`、`TOOLS.md`、`USER.md`、`IDENTITY.md`、`HEARTBEAT.md`
- Skills：内置起始技能，可启用 / 禁用 / 编辑 / 新建
- 语音：Android 系统 TTS，可选下载 Sherpa-ONNX 离线语音模型

## 权限

部分功能需要你在应用设置或系统设置中授权：

| 权限 | 用途 |
|------|------|
| 无障碍服务 | 读取界面树、点击、滑动、输入、全局导航 |
| 屏幕录制 | 无障碍树为空时的截屏兜底（每次会话需授权） |
| 麦克风 | 语音输入 |
| 通知 / 通知使用权 | 渠道与审批流程 |
| 悬浮窗 | 悬浮控件 |
| 共享文件夹 | 通过 `Documents/ClaudeDroid` 导入导出文件 |
| 短信 | 仅在启用短信渠道时需要 |

## 目录结构

```text
app/src/main/java/com/clawdroid/app/
├── core/
│   ├── automation/   # WorkManager 定时任务
│   ├── bootstrap/    # 内置 Linux 环境
│   ├── channels/     # WhatsApp / 短信 / 通知渠道
│   ├── config/       # 应用配置（LLM 配置来自 BuildConfig）
│   ├── control/      # 无障碍与截屏控制
│   ├── engine/       # Agent 循环、工具执行、MCP、上下文压缩、循环检测
│   ├── service/      # OAuth 与前台服务
│   ├── skills/       # 技能加载
│   ├── terminal/     # 进程执行
│   ├── tools/        # 暴露给模型的工具实现
│   ├── voice/        # TTS 引擎
│   └── workspace/    # Agent 工作区文件
├── data/
│   ├── api/          # LLM 客户端、消息与工具 schema
│   └── db/           # Room 数据库
└── ui/               # Compose 界面：chat / settings / sidebar / splash / voice ...

app/src/main/res/
├── values/           # 英文文案（strings_*.xml）
└── values-zh/        # 简体中文文案
```

新增界面文案时，请同时在 `values/` 和 `values-zh/` 中添加对应字符串。

## 调试

```bash
adb devices -l
adb logcat -d | grep -iE "LlmApiClient|AgentEngine|FATAL EXCEPTION|AndroidRuntime"
```

## 已知限制

- 只支持 Claude（通过 OpenAI 兼容接口）；更换模型或 Key 需修改 `local.properties` 后重新编译
- `targetSdk = 28` 是为了内置 Linux 环境，适合侧载安装，不符合 Play 商店要求
- 不支持 root 能力和本地模型推理
- 屏幕录制授权按会话计算，需要用户手动同意

## 更多文档

- 产品与架构方向：`AGENTS.md`
- 项目上下文：`context.md`
