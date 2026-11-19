package com.example.onmyojiagent

import android.graphics.Bitmap

data class OcrText(val text: String, val bounds: String?)

/** OCR 引擎接口：ML Kit / PaddleOCR 实现 */
interface OcrEngine {
    fun recognize(bitmap: Bitmap): List<OcrText>
}

/** 多模态视觉模型接口：Qwen-VL / GPT-4o 等 */
interface VlmEngine {
    fun ask(bitmap: Bitmap, question: String): String
}

/** 截图能力：MediaProjection 实现（无障碍服务本身不能截图） */
interface ScreenCapture {
    fun capture(): Bitmap?
}
