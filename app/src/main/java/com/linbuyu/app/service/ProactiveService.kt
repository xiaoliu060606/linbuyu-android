package com.linbuyu.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.MainActivity
import com.linbuyu.app.R
import com.linbuyu.app.network.ProactiveMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台服务：App 内唯一轮询者。
 *  - 每 5s 拉取 /api/proactive（读即清空），前台广播给 UI，后台发通知提醒
 *  - 每 60s 心跳 /api/user-active，告诉后端"用户在线"以调度主动消息
 *  - START_STICKY：被系统回收后自动重启
 */
class ProactiveService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val app: LinbuyuApp get() = application as LinbuyuApp

    companion object {
        const val CHANNEL_ID = "linbuyu_proactive"
        const val NOTIFY_ID = 1001
        const val PROACTIVE_NOTIFY_ID = 1002

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "林不语消息",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "收到林不语主动消息时提醒"
                }
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        startForeground(NOTIFY_ID, buildForegroundNotification())
        scope.launch {
            var lastHeartbeat = 0L
            while (isActive) {
                runCatching {
                    val messages = app.repository.fetchProactive()
                    if (messages.isNotEmpty()) {
                        for (msg in messages) {
                            ProactiveBus.messages.emit(msg)
                            if (!AppForeground.isForeground) {
                                showProactiveNotification(msg)
                            }
                        }
                    }
                }
                val now = System.currentTimeMillis()
                if (now - lastHeartbeat >= 60_000) {
                    lastHeartbeat = now
                    runCatching { app.repository.userActive() }
                }
                delay(5_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("林不语")
            .setContentText("守护中，随时回应你")
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showProactiveNotification(msg: ProactiveMessage) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("林不语")
            .setContentText(msg.content.take(60))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).notify(PROACTIVE_NOTIFY_ID, notification)
    }
}
