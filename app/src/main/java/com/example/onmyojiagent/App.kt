package com.example.onmyojiagent

import android.app.Application
import com.example.onmyojiagent.engine.MlKitOcrEngine
import com.example.onmyojiagent.engine.ScreenCaptureImpl

/** 应用入口：初始化知识库、注入引擎实现 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Reporter.appContext = this

        // 1) 知识库初始化 + 冷启动种子
        KnowledgeRepo.init("$filesDir/knowledge.db")
        KnowledgeRepo.seedDefaults()

        // 2) 注入引擎实现（OCR / 截图）
        VersionDetector.ocr = MlKitOcrEngine()
        VersionDetector.screen = ScreenCaptureImpl
        Reporter.screen = ScreenCaptureImpl
        BackRecognizer.screen = ScreenCaptureImpl

        // 3) AI 客户端（可选，按需注入其一）：
        // Brain.llm = OllamaClient()                                   // 本地离线
        // Brain.llm = OpenAiCompatClient(baseUrl, apiKey, model)       // 云端
    }
}
