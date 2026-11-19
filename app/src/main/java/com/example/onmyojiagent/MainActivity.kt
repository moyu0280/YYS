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

    private lateinit var txtLog: TextView
    private lateinit var taskItems: List<TaskItem>

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

        taskItems = loadTasks()
        val spinner = findViewById<Spinner>(R.id.spinnerTask)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, taskItems.map { it.name }
        )
    }

    /** 从 assets/tasks 读取所有任务，取 YAML 里的中文名显示 */
    private fun loadTasks(): List<TaskItem> {
        return try {
            assets.list("tasks")
                ?.filter { it.endsWith(".yaml") }
                ?.mapNotNull { file ->
                    val yaml = assets.open("tasks/$file").bufferedReader().readText()
                    val task = TaskConfigParser.parse(yaml)
                    TaskItem(file.removeSuffix(".yaml"), task.name)
                }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun startSelectedTask() {
        if (taskItems.isEmpty()) {
            runOnUiThread { txtLog.append("没有可用任务\n") }
            return
        }
        val spinner = findViewById<Spinner>(R.id.spinnerTask)
        val item = taskItems[spinner.selectedItemPosition]
        Thread {
            val yaml = assets.open("tasks/${item.id}.yaml").bufferedReader().readText()
            val task = TaskConfigParser.parse(yaml)
            TaskEngine.run(task)
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
