package com.example.onmyojiagent

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.onmyojiagent.engine.MediaProjectionCapture

/** 主界面：授权引导 + 任务选择 + 开始/停止 + 日志 */
class MainActivity : AppCompatActivity() {

    private data class TaskItem(val id: String, val name: String)

    // 任务列表（id 对应 assets/tasks/<id>.yaml）
    private val taskItems = listOf(
        TaskItem("daily_signin", "每日签到"),
        TaskItem("soul_run", "御魂副本")
    )

    private lateinit var txtLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtLog = findViewById(R.id.txtLog)
        Reporter.logListener = { runOnUiThread { txtLog.append("$it\n") } }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnNotify).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), 200)
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener { startSelectedTask() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, MediaProjectionCapture::class.java))
            runOnUiThread { txtLog.append("已停止\n") }
        }

        val spinner = findViewById<Spinner>(R.id.spinnerTask)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, taskItems.map { it.name }
        )
    }

    private fun startSelectedTask() {
        val spinner = findViewById<Spinner>(R.id.spinnerTask)
        val item = taskItems[spinner.selectedItemPosition]
        Thread {
            try {
                val yaml = assets.open("tasks/${item.id}.yaml").bufferedReader().readText()
                val task = TaskConfigParser.parse(yaml)
                TaskEngine.run(task)
            } catch (e: Exception) {
                runOnUiThread { txtLog.append("执行出错: ${e.message}\n") }
            }
        }.start()
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, MediaProjectionCapture::class.java)
                .putExtra(MediaProjectionCapture.EXTRA_RESULT_CODE, resultCode)
                .putExtra(MediaProjectionCapture.EXTRA_RESULT_DATA, data)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent)
            else startService(intent)
        }
    }
}
