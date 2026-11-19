package com.example.onmyojiagent

/** 任务 DSL 数据模型（对应 YAML） */
data class TaskConfig(
    val id: String,
    val name: String,
    val steps: List<TaskStep>
)

data class TaskStep(
    val enterPage: String?,   // 前置页面指纹（null = 不校验）
    val action: Action
)

sealed class Action {
    data class ClickText(val text: String) : Action()
    data class ClickId(val id: String) : Action()
    data class ClickElement(val elementId: String) : Action()   // 走元素词典
    data class ClickRegion(val x: Float, val y: Float) : Action()
    data class InputText(val text: String) : Action()
    data class Wait(val ms: Long) : Action()
    object Back : Action()   // 永远走 ActionExecutor.requestBack()，点游戏内 UI
}
