package com.example.onmyojiagent

import org.yaml.snakeyaml.Yaml

/**
 * 任务 DSL 解析器：把 assets/tasks/*.yaml 映射为 TaskConfig。
 * 依赖：org.yaml:snakeyaml。
 */
object TaskConfigParser {

    fun parse(yamlText: String): TaskConfig {
        val root = Yaml().load<Map<String, Any?>>(yamlText)
        val task = root["task"] as Map<String, Any?>
        val id = task["id"] as String
        val name = task["name"] as String
        val steps = (task["steps"] as List<Map<String, Any?>>).map(::parseStep)
        return TaskConfig(id, name, steps)
    }

    private fun parseStep(raw: Map<String, Any?>): TaskStep {
        val enterPage = raw["enterPage"] as? String
        val action = raw["action"] as Map<String, Any?>
        val a = when {
            action.containsKey("clickText") -> Action.ClickText(action["clickText"] as String)
            action.containsKey("clickId") -> Action.ClickId(action["clickId"] as String)
            action.containsKey("clickElement") -> Action.ClickElement(action["clickElement"] as String)
            action.containsKey("inputText") -> Action.InputText(action["inputText"] as String)
            action.containsKey("clickRegion") -> {
                val r = action["clickRegion"] as Map<String, Any?>
                Action.ClickRegion((r["x"] as Number).toFloat(), (r["y"] as Number).toFloat())
            }
            action.containsKey("wait") -> Action.Wait((action["wait"] as Number).toLong())
            action.containsKey("back") -> Action.Back   // 走游戏内返回，不用系统返回
            else -> error("未知 action: $action")
        }
        return TaskStep(enterPage, a)
    }
}
