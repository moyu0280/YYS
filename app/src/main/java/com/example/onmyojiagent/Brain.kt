package com.example.onmyojiagent

import org.json.JSONObject

/**
 * AI 决策（Brain）：规则走不通时，把"屏幕 + 任务 + 知识"交给 LLM 决定下一步。
 * 客户端可插拔（本地小模型 / 云端大模型），决策结果统一解析成 Action。
 */

/** LLM 客户端接口：本地 / 云端实现二选一注入 */
interface LlmClient {
    fun complete(prompt: String): String
}

object Brain {
    var llm: LlmClient? = null   // 由外部注入（OllamaClient / OpenAiCompatClient）

    /** 请求 AI 兜底：返回下一步动作，或 null（AI 也无解 → 交人工） */
    fun decide(
        taskId: String,
        stepDesc: String,
        screen: ScreenState,
        version: String
    ): Action? {
        val client = llm ?: return null
        val prompt = buildPrompt(taskId, stepDesc, screen, version)
        return parseAction(client.complete(prompt))
    }

    private fun buildPrompt(
        taskId: String, stepDesc: String, screen: ScreenState, version: String
    ): String {
        val els = screen.elements.take(40).joinToString("\n") {
            "id=${it.resourceId} text=${it.text} desc=${it.desc} clickable=${it.clickable} bounds=${it.bounds}"
        }
        return """
            |你是《阴阳师》自动化助手，当前版本 $version。
            |任务：$taskId；当前步骤：$stepDesc。
            |仅根据下面的屏幕元素决策，不要臆测。返回 JSON：
            |{"action":{"type":"clickText|clickId|clickElement|clickRegion|inputText|back|wait|none","text":"","id":"","elementId":"","x":0.0,"y":0.0,"ms":0},"reason":"..."}
            |铁律：返回/关闭只点游戏内箭头或 X，禁止系统返回键；消耗勾玉/御魂/金币、购买、分解、删除一律返回 none；不确定就返回 none。
            |屏幕元素：
            |$els
        """.trimMargin()
    }

    /** 解析 LLM 返回的 action JSON → Action */
    private fun parseAction(json: String): Action? {
        return try {
            val obj = JSONObject(json).getJSONObject("action")
            when (obj.getString("type")) {
                "clickText" -> Action.ClickText(obj.getString("text"))
                "clickId" -> Action.ClickId(obj.getString("id"))
                "clickElement" -> Action.ClickElement(obj.getString("elementId"))
                "clickRegion" ->
                    Action.ClickRegion(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat())
                "inputText" -> Action.InputText(obj.getString("text"))
                "back" -> Action.Back
                "wait" -> Action.Wait(obj.getLong("ms"))
                else -> null   // 含 "none"：交人工
            }
        } catch (e: Exception) {
            null
        }
    }
}
