package com.linbuyu.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "linbuyu_settings")

/** 本地偏好：服务器地址、访问令牌、显示名。聊天记录等业务数据以服务器为真相源。 */
class SettingsStore(private val context: Context) {
    companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_TOKEN = stringPreferencesKey("app_token")
        val KEY_AI_NAME = stringPreferencesKey("ai_name")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_FINGERPRINT = stringPreferencesKey("fingerprint")
        // 模拟器访问宿主机用 10.0.2.2；真机连局域网/云端改为实际地址
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    val aiName: Flow<String> = context.dataStore.data.map { it[KEY_AI_NAME] ?: "林不语" }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "我" }

    suspend fun getBaseUrl(): String = baseUrl.first()
    suspend fun getToken(): String = token.first()
    suspend fun setBaseUrl(v: String) = context.dataStore.edit { it[KEY_BASE_URL] = v }
    suspend fun setToken(v: String) = context.dataStore.edit { it[KEY_TOKEN] = v }
    suspend fun setAiName(v: String) = context.dataStore.edit { it[KEY_AI_NAME] = v }
    suspend fun setUserName(v: String) = context.dataStore.edit { it[KEY_USER_NAME] = v }

    /** 设备指纹：首次访问时生成并持久化一个随机 UUID，用于 /api/loc、/api/weather 的身份头 */
    suspend fun getFingerprint(): String {
        val existing = context.dataStore.data.first()[KEY_FINGERPRINT]
        if (!existing.isNullOrBlank()) return existing
        val generated = "android-" + UUID.randomUUID()
        context.dataStore.edit { it[KEY_FINGERPRINT] = generated }
        return generated
    }

    // 网络层在 IO 线程同步读取当前值（DataStore 有缓存，毫秒级）
    fun getBaseUrlSync(): String = runBlocking { getBaseUrl() }
    fun getTokenSync(): String = runBlocking { getToken() }
    fun getFingerprintSync(): String = runBlocking { getFingerprint() }
}
