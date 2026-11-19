package com.example.onmyojiagent

/**
 * 版本检测：读游戏当前版本号（登录页 / 设置-关于）。
 * 引擎（OcrEngine/VlmEngine/ScreenCapture）由 App 启动时注入。
 */
object VersionDetector {
    var ocr: OcrEngine? = null
    var vlm: VlmEngine? = null
    var screen: ScreenCapture? = null

    /** 返回版本号字符串，如 "1.0.86"；读不到返回 null */
    fun detect(): String? {
        val bmp = screen?.capture() ?: return null
        // 1) OCR 直接找版本号文本
        ocr?.recognize(bmp)?.forEach { t ->
            Regex("""v?(\d+\.\d+\.\d+)""").find(t.text)?.let {
                return it.groupValues[1]
            }
        }
        // 2) VLM 兜底询问版本号
        vlm?.ask(bmp, "这是阴阳师登录页或关于页，请只回答客户端版本号，格式 x.x.x")?.let {
            Regex("""(\d+\.\d+\.\d+)""").find(it)?.let { m ->
                return m.groupValues[1]
            }
        }
        return null
    }

    /**
     * 版本对齐检查：读版本号 → 查知识库。
     * 读不到或不对齐都返回 false，由上层暂停并提示"检测到新版本，正在更新知识库"。
     */
    fun ensureAligned(): Boolean {
        val v = detect() ?: return false
        return KnowledgeRepo.isVersionAligned(v)
    }
}
