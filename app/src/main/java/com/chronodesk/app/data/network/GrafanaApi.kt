package com.chronodesk.app.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class GrafanaDatasourceItem(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("isDefault") val isDefault: Boolean? = null
)

data class GrafanaQueryBody(
    @SerializedName("from") val from: String = (System.currentTimeMillis() - 86_400_000L).toString(),
    @SerializedName("to") val to: String = System.currentTimeMillis().toString(),
    @SerializedName("queries") val queries: List<GrafanaQueryEntry>
)

data class GrafanaQueryEntry(
    @SerializedName("refId") val refId: String = "A",
    @SerializedName("datasource") val datasource: GrafanaDatasourceRef,
    @SerializedName("rawSql") val rawSql: String,
    @SerializedName("format") val format: String = "table",
    @SerializedName("rawQuery") val rawQuery: Boolean = true,
    @SerializedName("editorMode") val editorMode: String = "code",
    @SerializedName("intervalMs") val intervalMs: Long = 60000L,
    @SerializedName("maxDataPoints") val maxDataPoints: Int = 1000
)

data class GrafanaDatasourceRef(
    @SerializedName("uid") val uid: String,
    @SerializedName("type") val type: String = "postgres"
)

interface GrafanaRetrofitService {

    @GET("api/datasources")
    suspend fun getDatasources(
        @Header("Authorization") authorization: String
    ): List<GrafanaDatasourceItem>

    @POST("api/ds/query")
    suspend fun queryDataSource(
        @Header("Authorization") authorization: String,
        @Body body: GrafanaQueryBody
    ): GrafanaQueryResponse
}

/**
 * 专用于 Grafana / TeslaMate 的独立网络客户端
 * 严格执行安全契约：
 * 1. 独立主机与连接池，绝不携带 OpenAI / DeepSeek 等外部 Token；
 * 2. 禁止跨域或跨协议重定向携带 Authorization 凭据；
 * 3. 严格校验 URL 规范（无 query、无 userinfo，https 优先）。
 */
class GrafanaApiClient {

    private val serviceCache = ConcurrentHashMap<String, GrafanaRetrofitService>()

    /**
     * 重定向安全拦截器：若发生重定向且目标主机或协议与原请求不符，严禁转发凭据
     */
    private val redirectProtectionInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalHost = originalRequest.url.host
        val originalScheme = originalRequest.url.scheme

        val response = chain.proceed(originalRequest)
        if (response.isRedirect) {
            val location = response.header("Location")
            if (!location.isNullOrBlank()) {
                val targetUri = try { URI.create(location) } catch (_: Exception) { null }
                if (targetUri != null) {
                    val targetHost = targetUri.host
                    val targetScheme = targetUri.scheme
                    if (targetHost != null && !targetHost.equals(originalHost, ignoreCase = true)) {
                        // 目标主机不一致，抛出异常阻断携带凭据泄漏
                        throw IOException("禁止跨主机重定向以保护安全凭证 ($originalHost -> $targetHost)")
                    }
                    if (originalScheme == "https" && targetScheme == "http") {
                        throw IOException("禁止从 HTTPS 重定向至不安全的 HTTP 协议")
                    }
                }
            }
        }
        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true) // 允许同源重定向，由 redirectProtectionInterceptor 阻断跨主机/跨协议泄密
            .followSslRedirects(true)
            .addInterceptor(redirectProtectionInterceptor)
            .build()
    }

    /**
     * 获取或构建 Retrofit 服务端点
     */
    fun getService(baseUrl: String, allowHttp: Boolean = false): GrafanaRetrofitService {
        val sanitizedBaseUrl = sanitizeAndValidateUrl(baseUrl, allowHttp)
        return serviceCache.computeIfAbsent(sanitizedBaseUrl) { url ->
            Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GrafanaRetrofitService::class.java)
        }
    }

    /**
     * 校验并标准化 Grafana Base URL
     */
    fun sanitizeAndValidateUrl(rawUrl: String, allowHttp: Boolean): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Grafana 地址不能为空")
        }

        val uri = try {
            URI.create(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Grafana 地址格式无效: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw IllegalArgumentException("Grafana 地址协议必须为 http 或 https")
        }

        if (scheme == "http" && !allowHttp) {
            throw IllegalArgumentException("HTTP 协议会明文传递 Token；仅可信内网环境允许勾选开启 HTTP")
        }

        if (uri.host.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 地址缺少有效的主机名")
        }

        if (!uri.userInfo.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 地址严禁包含用户名或密码")
        }

        if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 根地址不可包含查询参数或片段标识 (#)")
        }

        return "$trimmed/"
    }

    companion object {
        val instance = GrafanaApiClient()
    }
}
