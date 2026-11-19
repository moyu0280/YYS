package com.example.onmyojiagent

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务：本 App 的"手"与"眼"唯一入口。
 * 硬约束：本服务永不调用 performGlobalAction(GLOBAL_ACTION_BACK / HOME)。
 */
class AgentAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 窗口内容变化时，由 TaskEngine 主动读屏即可，此处留空
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
    }

    /** 读当前屏幕根节点（可能为 null，需等待重试） */
    fun rootNode(): AccessibilityNodeInfo? = rootInActiveWindow

    /**
     * 系统返回键 —— 禁用。
     * 用 @Deprecated(ERROR) 在编译期拦截任何误用；本方法永远不实现。
     */
    @Deprecated("禁止使用系统返回键，请使用 ActionExecutor.requestBack()", level = DeprecationLevel.ERROR)
    fun systemBack() {
        error("系统返回键被禁用")
    }
}
