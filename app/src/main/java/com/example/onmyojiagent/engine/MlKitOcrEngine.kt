package com.example.onmyojiagent.engine

import android.graphics.Bitmap
import com.example.onmyojiagent.OcrEngine
import com.example.onmyojiagent.OcrText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** ML Kit 中文 OCR 实现（识别版本号、图片文字等） */
class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override fun recognize(bitmap: Bitmap): List<OcrText> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val latch = CountDownLatch(1)
        val out = AtomicReference<List<OcrText>>(emptyList())
        recognizer.process(image)
            .addOnSuccessListener { v ->
                out.set(v.textBlocks.flatMap { b ->
                    b.lines.map { OcrText(it.text, it.boundingBox?.toString()) }
                })
            }
            .addOnFailureListener { out.set(emptyList()) }
            .addOnCompleteListener { latch.countDown() }
        latch.await(10, TimeUnit.SECONDS)
        return out.get()
    }
}
