package com.linbuyu.app.service

import com.linbuyu.app.network.ProactiveMessage
import kotlinx.coroutines.flow.MutableSharedFlow

/** App 是否处于前台（MainActivity 维护），供服务决定"后台才发通知" */
object AppForeground {
    @Volatile
    var isForeground = false
}

/** 前台服务轮询到的主动消息广播给聊天页（App 内唯一轮询者，避免多端竞争读取即清空的队列） */
object ProactiveBus {
    val messages: MutableSharedFlow<ProactiveMessage> = MutableSharedFlow(extraBufferCapacity = 16)
}
