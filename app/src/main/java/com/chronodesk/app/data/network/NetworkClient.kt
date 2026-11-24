package com.chronodesk.app.data.network

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val TAG = "NetworkClient"

    /**
     * 智能代理选择器：
     * 针对 OpenAI 认证与用量查询端点 (auth.openai.com, chatgpt.com)，
     * 自动优先尝试本地 127.0.0.1:1082 (ADB 反向代理/Shadowrocket) 及 127.0.0.1:7890 (Clash)，
     * 随后自动回退至直连 (NO_PROXY)，确保网络在任何环境下无缝畅通。
     */
    private val smartProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> {
            val host = uri?.host ?: ""
            if (host.contains("openai.com") || host.contains("chatgpt.com") || host.contains("googleapis.com")) {
                return listOf(
                    Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 1082)),
                    Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 7890)),
                    Proxy.NO_PROXY
                )
            }
            return listOf(Proxy.NO_PROXY)
        }

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            Log.w(TAG, "Proxy route to $uri via $sa failed: ${ioe?.message}")
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(smartProxySelector)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthLoggingInterceptor())
            .build()
    }

    val authApi: CodexAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://auth.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CodexAuthApi::class.java)
    }

    val usageApi: CodexUsageApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://chatgpt.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CodexUsageApi::class.java)
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    val geocodingApi: GeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
    }

    /**
     * 用于 Antigravity OAuth 与额度查询的客户端（自动探测代理与 20s 超时）
     */
    val antigravityHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(smartProxySelector)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthLoggingInterceptor())
            .build()
    }

    /**
     * 用于天气备用接口与 Nominatim 地理编码的快速直连客户端（8 秒超时，仅 HTTPS）
     */
    val weatherFallbackHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(ProxySelector.getDefault())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    const val CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
    const val DEVICE_VERIFY_URL = "https://auth.openai.com/codex/device"
}
