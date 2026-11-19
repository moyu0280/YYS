package com.example.onmyojiagent

import android.graphics.Bitmap
import com.example.onmyojiagent.engine.TemplateMatcher

/**
 * 返回元素识别器：只识别游戏内左上角箭头 / 右上角 X / 文字"返回·关闭"。
 * 绝不降级到系统返回键。
 */
object BackRecognizer {

    var screen: ScreenCapture? = null       // 截图能力（MediaProjection）
    var arrowTemplate: Bitmap? = null       // 由 App 从 assets 加载
    var closeTemplate: Bitmap? = null

    // 相对坐标区域（机型无关）
    private const val ARROW_X1 = 0.02f; private const val ARROW_Y1 = 0.03f
    private const val ARROW_X2 = 0.10f; private const val ARROW_Y2 = 0.12f
    private const val CLOSE_X1 = 0.88f; private const val CLOSE_Y1 = 0.03f
    private const val CLOSE_X2 = 0.98f; private const val CLOSE_Y2 = 0.12f

    fun back(): Boolean {
        // 1) 词典/常见 id（返回按钮）
        if (ActionExecutor.clickById("btn_back")) return true
        if (ActionExecutor.clickById("iv_back")) return true
        // 2) 文字"返回"
        if (ActionExecutor.clickByText("返回")) return true
        // 3) 左上角箭头区域（模板匹配）
        if (clickArrowTopLeft()) return true
        // 4) 右上角 X 区域（模板匹配）
        if (clickCloseTopRight()) return true
        // 5) 文字"关闭/取消"
        if (ActionExecutor.clickByText("关闭")) return true
        if (ActionExecutor.clickByText("取消")) return true
        // 6) 全失败 → 上层暂停交人工
        return false
    }

    private fun clickArrowTopLeft(): Boolean =
        clickByTemplate(arrowTemplate, ARROW_X1, ARROW_Y1, ARROW_X2, ARROW_Y2)

    private fun clickCloseTopRight(): Boolean =
        clickByTemplate(closeTemplate, CLOSE_X1, CLOSE_Y1, CLOSE_X2, CLOSE_Y2)

    /** 在指定相对区域里做模板匹配；无模板/无截图则回退到点区域中心 */
    private fun clickByTemplate(tpl: Bitmap?, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val centerX = (x1 + x2) / 2f
        val centerY = (y1 + y2) / 2f
        val bmp = screen?.capture() ?: return false
        if (tpl == null) return ActionExecutor.clickAtRegion(centerX, centerY)
        val region = cropRegion(bmp, x1, y1, x2, y2)
        val hit = TemplateMatcher.match(region, tpl) ?: return false
        val relX = x1 + hit[0].toFloat() / bmp.width
        val relY = y1 + hit[1].toFloat() / bmp.height
        return ActionExecutor.clickAtRegion(relX, relY)
    }

    private fun cropRegion(bmp: Bitmap, x1: Float, y1: Float, x2: Float, y2: Float): Bitmap {
        val w = (bmp.width * (x2 - x1)).toInt().coerceAtLeast(1)
        val h = (bmp.height * (y2 - y1)).toInt().coerceAtLeast(1)
        val x = (bmp.width * x1).toInt()
        val y = (bmp.height * y1).toInt()
        return Bitmap.createBitmap(bmp, x, y, w, h)
    }
}
