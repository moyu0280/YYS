# 阴阳师自动化助手（OnmyojiAgent）

Android 无障碍 + AI 的阴阳师日常/活动自动化 App。**设计文档见上级目录**（阴阳师自动化APP设计方案.md 及详细设计各文件）。

## 构建

1. 用 Android Studio 打开本目录 `onmyoji-agent/`；
2. 同步 Gradle（AGP 8.3.2 / Kotlin 1.9.24，需 JDK 17）；
3. 连真机运行（需要 Android 8.0+，minSdk 26）。

## 云端构建（GitHub Actions，无需电脑）

1. 建一个 GitHub 仓库，把本目录（`onmyoji-agent/`）里的内容作为**仓库根**推上去（`settings.gradle.kts` 在根目录）。
2. 推代码即自动触发构建；也可在仓库 Actions 页手动 `Run workflow`。
3. 构建完成后，在该次运行页面下载产物 `onmyoji-agent-debug`（含 `app-debug.apk`）。
4. 把 APK 传到手机安装。

命令参考（在 `onmyoji-agent/` 目录里）：

```bash
git init && git add -A && git commit -m "init"
git branch -M main
git remote add origin <你的仓库地址>
git push -u origin main
```

## 使用步骤

1. 打开 App → 点「开启无障碍服务」（跳到系统设置，勾选本 App）；
2. 点「授权屏幕截图」（弹系统投屏授权，允许）；
3. 点「授权通知」；
4. 选任务 →「开始执行」。

## 关键说明

- **游戏包名**：在 `res/xml/accessibility_service_config.xml` 的 `packageNames`，默认 `com.netease.onmyoji`；渠道服（B 站等）需改。
- **AI 决策默认关闭**（`Brain.llm` 为 null，规则驱动也能跑）；在 `App.onCreate` 注入 `OllamaClient`（本地）或 `OpenAiCompatClient`（云端）。
- **知识库种子**在 `KnowledgeRepo.seedDefaults()`，正式版由「领域知识层」填充真实页面/元素；版本不一致会自动暂停。
- **三条硬约束（代码层保证）**：不读内存（只用 UI 树 + 截图）、禁系统返回键（`requestBack` 只点游戏内箭头/X）、危险操作护栏拦截（`RiskGuard`）。

## 目录

```
app/src/main/java/com/example/onmyojiagent/
├── App.kt / MainActivity.kt         # 入口 + 主界面
├── AgentAccessibilityService.kt     # 无障碍服务（禁系统返回）
├── ScreenState.kt / ActionExecutor.kt
├── BackRecognizer.kt / ElementLocator.kt
├── TaskConfig.kt / TaskConfigParser.kt / TaskEngine.kt
├── Brain.kt / LlmClients.kt / PerceptionEngines.kt
├── VersionDetector.kt / KnowledgeRepo.kt / Reporter.kt
├── risk/RiskGuard.kt                # 危险操作护栏
└── engine/
    ├── MlKitOcrEngine.kt            # OCR 实现
    ├── MediaProjectionCapture.kt    # 截图实现
    └── TemplateMatcher.kt           # 形状识别
```

## 尚未完成（引擎/实机部分）

1. 版本号读取需在登录页/关于页实际调 OCR 验证；
2. `BackRecognizer` 的箭头/X 模板需采集真实截图放到 `assets/templates/`；
3. 通知/截图落盘的完整实现；
4. 游戏内输入框的粘贴式输入兜底（部分输入框不响应 ACTION_SET_TEXT）。
