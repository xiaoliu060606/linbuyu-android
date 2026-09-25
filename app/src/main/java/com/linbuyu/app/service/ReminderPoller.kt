package com.linbuyu.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.linbuyu.app.LinbuyuApp
import com.linbuyu.app.MainActivity
import com.linbuyu.app.R
import com.linbuyu.app.network.ReminderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 到期提醒轮询：App 打开期间每 30s 拉一次 /api/reminders。
 * 新出现的 due 提醒 → 本地通知 + 回调（聊天页塞一条系统气泡），并立即 ack，
 * 避免同一个提醒每 30s 重复弹。已投递 id 记在内存里，本次会话内不重复。
 */
class ReminderPoller(
    context: Context,
    private val onReminder: (ReminderItem) -> Unit,
) {

    private val app = context.applicationContext as LinbuyuApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val delivered = mutableSetOf<Long>()

    companion object {
        const val CHANNEL_ID = "linbuyu_reminders"
        const val NOTIFY_ID = 2001
        private const val POLL_INTERVAL_MS = 30_000L

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "到期提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "林不语的到期提醒（带文件、待办等）"
                }
            )
        }
    }

    fun start() {
        scope.launch {
            while (isActive) {
                runCatching {
                    val due = app.repository.fetchReminders()
                    for (r in due) {
                        if (delivered.add(r.id)) {
                            onReminder(r)
                            showNotification(r)
                            runCatching { app.repository.ackReminder(r.id) }
                        }
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun showNotification(r: ReminderItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(app, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            app, r.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ 提醒")
            .setContentText(r.text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(app).notify(NOTIFY_ID, notification)
    }
}
