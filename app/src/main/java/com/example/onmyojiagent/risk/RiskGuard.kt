package com.example.onmyojiagent.risk

import com.example.onmyojiagent.Action
import com.example.onmyojiagent.KnowledgeRepo

/** 危险操作护栏：消耗/购买/分解/删除等一律拦截，交人工确认 */
object RiskGuard {

    private val riskyWords = listOf(
        "勾玉", "御魂", "金币", "购买", "充值", "分解", "返还", "献祭",
        "删除", "退寮", "出售", "消耗", "合成", "兑换"
    )

    fun isRiskyText(text: String?): Boolean {
        text ?: return false
        return riskyWords.any { text.contains(it) }
    }

    /** 判断 Action 是否危险（点击/输入文本、或词典元素文本命中危险词） */
    fun isRisky(action: Action): Boolean = when (action) {
        is Action.ClickText -> isRiskyText(action.text)
        is Action.InputText -> isRiskyText(action.text)
        is Action.ClickElement -> isRiskyText(KnowledgeRepo.element(action.elementId)?.text)
        else -> false
    }
}
