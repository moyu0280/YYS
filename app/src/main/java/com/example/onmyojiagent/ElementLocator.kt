package com.example.onmyojiagent

/** 元素定位：优先走词典 ui_element（resource_id → text → region），命中即执行 */
object ElementLocator {
    fun click(elementId: String): Boolean {
        val el = KnowledgeRepo.element(elementId) ?: return false
        if (el.resourceId != null && ActionExecutor.clickById(el.resourceId)) return true
        if (el.text != null && ActionExecutor.clickByText(el.text)) return true
        el.region?.let { r ->
            val p = r.split(",")
            if (p.size == 2) {
                return ActionExecutor.clickAtRegion(p[0].trim().toFloat(), p[1].trim().toFloat())
            }
        }
        return false
    }
}
