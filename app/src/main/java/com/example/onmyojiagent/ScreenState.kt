package com.example.onmyojiagent

import android.view.accessibility.AccessibilityNodeInfo

/** 结构化屏幕：把无障碍节点树压成"可操作元素列表 + 文本指纹" */
data class ClickableElement(
    val resourceId: String?,
    val text: String?,
    val desc: String?,
    val clickable: Boolean,
    val bounds: String?
)

data class ScreenState(
    val elements: List<ClickableElement>,
    val allText: Set<String>
) {
    companion object {
        fun capture(): ScreenState {
            val root = AgentAccessibilityService.instance?.rootNode()
            val els = mutableListOf<ClickableElement>()
            val texts = mutableSetOf<String>()

            fun walk(n: AccessibilityNodeInfo?) {
                if (n == null) return
                n.text?.toString()?.let { texts.add(it) }
                n.contentDescription?.toString()?.let { texts.add(it) }
                if (n.isClickable || n.text != null || n.viewIdResourceName != null) {
                    els.add(
                        ClickableElement(
                            n.viewIdResourceName,
                            n.text?.toString(),
                            n.contentDescription?.toString(),
                            n.isClickable,
                            n.boundsInScreen?.toString()
                        )
                    )
                }
                for (i in 0 until n.childCount) walk(n.getChild(i))
            }
            walk(root)
            return ScreenState(els, texts)
        }
    }
}
