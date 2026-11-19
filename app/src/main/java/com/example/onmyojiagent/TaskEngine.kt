package com.example.onmyojiagent

import com.example.onmyojiagent.risk.RiskGuard

/**
 * 任务引擎（规则驱动状态机 + AI 兜底 + 危险操作护栏）。
 * 循环：版本对齐 → 逐步骤执行 → 失败交给 Brain 兜底一次 → 仍失败暂停交人工。
 */
object TaskEngine {

    fun run(task: TaskConfig) {
        if (!VersionDetector.ensureAligned()) {
            Reporter.reportVersionMismatch(task)
            return
        }
        for (step in task.steps) {
            if (!runStep(step)) {
                if (!aiFallback(task, step)) {
                    Reporter.reportFailure(task, step)
                    return
                }
            }
        }
        Reporter.reportDone(task)
    }

    private fun runStep(step: TaskStep): Boolean {
        if (!waitForPage(step.enterPage)) return false
        return executeAction(step.action)
    }

    /** 规则失败 → 交给 AI 决策一次 */
    private fun aiFallback(task: TaskConfig, step: TaskStep): Boolean {
        val action = Brain.decide(
            taskId = task.id,
            stepDesc = step.toString(),
            screen = ScreenState.capture(),
            version = VersionDetector.detect() ?: "unknown"
        ) ?: return false
        return executeAction(action)
    }

    private fun executeAction(a: Action): Boolean {
        if (RiskGuard.isRisky(a)) {
            Reporter.reportBlocked(a)
            return false   // 危险操作：拦截，交人工
        }
        return when (a) {
            is Action.ClickText -> ActionExecutor.clickByText(a.text)
            is Action.ClickId -> ActionExecutor.clickById(a.id)
            is Action.ClickElement -> ElementLocator.click(a.elementId)
            is Action.ClickRegion -> ActionExecutor.clickAtRegion(a.x, a.y)
            is Action.InputText -> ActionExecutor.inputText(a.text)
            is Action.Wait -> { ActionExecutor.wait(a.ms); true }
            is Action.Back -> ActionExecutor.requestBack()   // 走游戏内返回，绝不用系统返回
        }
    }

    /** 等待页面指纹命中（读屏循环 + 超时） */
    private fun waitForPage(pageId: String?): Boolean {
        if (pageId == null) return true
        val fp = KnowledgeRepo.pageFingerprint(pageId) ?: return true
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (fp.matches(ScreenState.capture().allText)) return true
            ActionExecutor.wait(500)
        }
        return false
    }
}
