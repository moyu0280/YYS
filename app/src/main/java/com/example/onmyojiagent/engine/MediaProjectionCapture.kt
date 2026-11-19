package com.example.onmyojiagent.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import com.example.onmyojiagent.ScreenCapture

/**
 * 截图服务：MediaProjection 前台服务，持续把屏幕帧读进 latest。
 * 无障碍服务本身不能截图，必须靠这个授权方式。
 */
class MediaProjectionCapture : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        @Volatile var latest: Bitmap? = null
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        if (code != 0 && data != null) startCapture(code, data)
        return START_STICKY
    }

    private fun startCapture(code: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(code, data)
        val w = resources.displayMetrics.widthPixels
        val h = resources.displayMetrics.heightPixels
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val img = reader.acquireLatestImage()
                if (img != null) {
                    val plane = img.planes[0]
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    // 简化：多数机型 pixelStride=4、rowStride=w*4，可直接 copyPixelsFromBuffer
                    bmp.copyPixelsFromBuffer(plane.buffer)
                    latest = bmp
                    img.close()
                }
            }, null)
        }
        virtualDisplay = projection?.createVirtualDisplay(
            "onmyoji_capture", w, h, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun buildNotification(): Notification {
        val ch = NotificationChannel("capture", "截图服务", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        return Notification.Builder(this, "capture")
            .setContentTitle("阴阳师自动化运行中")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        projection?.stop()
        imageReader?.close()
        super.onDestroy()
    }
}

/** 实现 ScreenCapture 接口：读取 MediaProjectionCapture.latest */
object ScreenCaptureImpl : ScreenCapture {
    override fun capture(): Bitmap? = MediaProjectionCapture.latest
}
