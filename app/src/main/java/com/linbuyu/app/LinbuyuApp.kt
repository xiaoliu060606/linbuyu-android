package com.linbuyu.app

import android.app.Application
import com.linbuyu.app.data.SettingsStore
import com.linbuyu.app.network.ApiClient
import com.linbuyu.app.network.ChatRepository
import com.linbuyu.app.network.SseClient
import com.linbuyu.app.service.ProactiveService
import com.linbuyu.app.service.ReminderPoller

/** 应用级单例容器：设置存储 + 网络仓库，Activity/Service 共用同一实例 */
class LinbuyuApp : Application() {

    lateinit var settings: SettingsStore
        private set
    lateinit var repository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        val api = ApiClient(settings)
        repository = ChatRepository(api, SseClient(api))
        ProactiveService.createChannel(this)
        ReminderPoller.createChannel(this)
    }
}