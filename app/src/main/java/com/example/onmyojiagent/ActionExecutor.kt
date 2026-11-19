package com.example.onmyojiagent

import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 动作执行器：所有"点/滑/输入"的统一出口。
 * 返回/关闭一律走 requestBack()，内部转译成点击游戏内 UI，绝不碰系统返回键。
 */
object ActionExecutor {

    private val service get() = AgentAccessibilityService.instance

    fun clickByText(text: String): Boolean =
        service?.rootNode()
            ?.findAccessibilityNodeInfosByText(text)
            ?.firstOrNull { it.isClickable }
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun clickById(resourceId: String): Boolean =
        service?.rootNode()
            ?.findAccessibilityNodeInfosByViewId(resourceId)
            ?.firstOrNull { it.isClickable }
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    /** 按相对坐标点击（机型无关）：相对坐标 → 绝对像素 → dispatchGesture */
    fun clickAtRegion(relX: Float, relY: Float): Boolean {
        val s = service ?: return false
        val root = s.rootNode() ?: return false
        val b = Rect()
        root.getBoundsInScreen(b)
        val absX = b.left + (relX * b.width()).toInt()
        val absY = b.top + (relY * b.height()).toInt()
        return dispatchTap(absX.toFloat(), absY.toFloat())
    }

    /** 文本输入：对可编辑节点用 ACTION_SET_TEXT（部分游戏输入框需粘贴兜底） */
    fun inputText(text: String): Boolean {
        val root = service?.rootNode() ?: return false
        val field = findEditable(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findEditable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (n.isEditable) return n
        for (i in 0 until n.childCount) {
            findEditable(n.getChild(i))?.let { return it }
        }
        return null
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 300): Boolean {
        val s = service ?: return false
        val root = s.rootNode() ?: return false
        val b = Rect()
        root.getBoundsInScreen(b)
        val path = Path().apply {
            moveTo(b.left + x1 * b.width(), b.top + y1 * b.height())
            lineTo(b.left + x2 * b.width(), b.top + y2 * b.height())
        }
        val g = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, ms))
            .build()
        return s.dispatchGesture(g, null, null)
    }

    private fun dispatchTap(x: Float, y: Float): Boolean {
        val s = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        val g = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return s.dispatchGesture(g, null, null)
    }

    fun sleep(ms: Long) = Thread.sleep(ms)

    /** 统一"返回/关闭"入口 —— 只点游戏内 UI，找不到返回 false（上层暂停交人工） */
    fun requestBack(): Boolean = BackRecognizer.back()
}
