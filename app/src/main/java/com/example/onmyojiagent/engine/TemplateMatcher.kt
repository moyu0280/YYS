package com.example.onmyojiagent.engine

import android.graphics.Bitmap
import kotlin.math.sqrt

/** 简单灰度模板匹配（归一化互相关），用于返回箭头/X 形状识别 */
object TemplateMatcher {

    /**
     * 返回最佳匹配的左上角坐标 [x, y]；低于阈值返回 null。
     * 用于小模板 + 小区域（左上/右上角），复杂度可接受。
     */
    fun match(scene: Bitmap, template: Bitmap, threshold: Float = 0.6f): IntArray? {
        val sw = scene.width; val sh = scene.height
        val tw = template.width; val th = template.height
        if (tw > sw || th > sh) return null
        val s = gray(scene); val t = gray(template)
        val tMean = t.sum() / (tw * th)
        var best = -2f; var bx = 0; var by = 0
        for (y in 0..sh - th) for (x in 0..sw - tw) {
            var wMean = 0f
            for (j in 0 until th) for (i in 0 until tw) wMean += s[(y + j) * sw + (x + i)]
            wMean /= (tw * th)
            var num = 0f; var dS = 0f; var dT = 0f
            for (j in 0 until th) for (i in 0 until tw) {
                val sv = s[(y + j) * sw + (x + i)]
                val tv = t[j * tw + i]
                num += (sv - wMean) * (tv - tMean)
                dS += (sv - wMean) * (sv - wMean)
                dT += (tv - tMean) * (tv - tMean)
            }
            val score = if (dS == 0f || dT == 0f) 0f else num / sqrt(dS * dT)
            if (score > best) { best = score; bx = x; by = y }
        }
        return if (best >= threshold) intArrayOf(bx, by) else null
    }

    private fun gray(b: Bitmap): FloatArray {
        val w = b.width; val h = b.height
        val px = IntArray(w * h)
        b.getPixels(px, 0, w, 0, 0, w, h)
        return FloatArray(w * h) { i ->
            val c = px[i]
            0.299f * ((c shr 16) and 0xff) + 0.587f * ((c shr 8) and 0xff) + 0.114f * (c and 0xff)
        }
    }
}
