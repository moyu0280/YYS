package com.example.onmyojiagent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/** 运行报告：日志 + 通知 + 截图落库（供回放排查） */
object Reporter {
    var screen: ScreenCapture? = null        // 截图能力（MediaProjection）
    var appContext: Context? = null          // 由 App 注入，用于通知
    var logListener: ((String) -> Unit)? = null

    private const val CHANNEL_ID = "onmyoji_agent"

    fun reportDone(task: TaskConfig) =
        emit("[${task.id}] 完成", "任务完成", "${task.name} 已完成")

    fun reportFailure(task: TaskConfig, step: TaskStep) {
        emit("[${task.id}] 失败于 $step", "任务失败", "${task.name} 失败，已暂停，请人工处理")
        screen?.capture()?.let { saveScreenshot("${task.id}_fail") }
    }

    fun reportVersionMismatch(task: TaskConfig) =
        emit("[${task.id}] 版本不一致，已暂停", "版本更新", "检测到阴阳师新版本，正在更新知识库")

    fun reportBlocked(a: Action) =
        emit("危险操作已拦截: $a", "操作被拦截", "检测到可能消耗资源/资金的操作，已停止交人工")

    private fun emit(log: String, title: String, text: String) {
        Log.i("OnmyojiAgent", log)
        logListener?.invoke(log)
        notifyUser(title, text)
    }

    private fun notifyUser(title: String, text: String) {
        val ctx = appContext ?: return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "运行状态", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(ctx, CHANNEL_ID) else Notification.Builder(ctx)
        val n = builder
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }

    private fun saveScreenshot(name: String) {
        // TODO: 把 Bitmap 存到 filesDir/回放/name_时间戳.png
    }
}
