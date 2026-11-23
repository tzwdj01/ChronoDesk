package com.chronodesk.app.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.Dns
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
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 内网 DNS 解析锁定器：确保主机名地址校验与 OkHttp 实际连接使用同一组解析结果
 */
object IntranetDns : Dns {
    private val pinnedAddresses = ConcurrentHashMap<String, List<InetAddress>>()

    fun pin(host: String, addresses: List<InetAddress>) {
        pinnedAddresses[host.lowercase()] = addresses
    }

    fun clearPin(host: String) {
        pinnedAddresses.remove(host.lowercase())
    }

    fun clearAll() {
        pinnedAddresses.clear()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        return pinnedAddresses[hostname.lowercase()] ?: Dns.SYSTEM.lookup(hostname)
    }
}

/**
 * 校验 IP 地址是否属于私有局域网/回环/链路本地范围
 */
fun isPrivateAddress(address: InetAddress): Boolean {
    if (address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress) {
        return true
    }
    val bytes = address.address
    if (bytes != null && bytes.size == 16) {
        // IPv6 Unique Local Address (ULA) fc00::/7
        val firstByte = bytes[0].toInt() and 0xFF
        if ((firstByte and 0xFE) == 0xFC) {
            return true
        }
    }
    return false
}

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
class GrafanaApiClient(
    val dns: Dns = IntranetDns
) {

    private val serviceCache = ConcurrentHashMap<String, GrafanaRetrofitService>()

    /**
     * 重定向安全拦截器：保持自动重定向关闭，任何重定向均拦截并终止，以防凭据跨协议/跨域泄露
     */
    private val redirectProtectionInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        if (response.isRedirect) {
            val location = response.header("Location")
            throw IOException("自动重定向已禁用以保护凭据安全 (HTTP ${response.code} -> $location)")
        }
        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false) // 保持自动重定向关闭
            .followSslRedirects(false)
            .dns(dns)
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

        val host = uri.host
        if (host.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 地址缺少有效的主机名")
        }

        if (!uri.userInfo.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 地址严禁包含用户名或密码")
        }

        if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {
            throw IllegalArgumentException("Grafana 根地址不可包含查询参数或片段标识 (#)")
        }

        if (scheme == "http") {
            if (!allowHttp) {
                throw IllegalArgumentException("HTTP 协议会明文传递 Token；仅可信内网环境允许勾选开启 HTTP")
            }
            // 解析主机名，验证是否为内网地址，并锁定解析结果以防 DNS 重绑定
            val addresses = try {
                dns.lookup(host)
            } catch (e: Exception) {
                throw IllegalArgumentException("无法解析内网主机地址: $host (${e.message})")
            }
            if (addresses.isEmpty()) {
                throw IllegalArgumentException("无法解析主机名: $host")
            }
            for (addr in addresses) {
                if (!isPrivateAddress(addr)) {
                    throw IllegalArgumentException("HTTP 仅支持私有内网地址或主机名，禁止连接公网地址: $host (${addr.hostAddress})")
                }
            }
            IntranetDns.pin(host, addresses)
        }

        return "$trimmed/"
    }

    companion object {
        val instance = GrafanaApiClient()
    }
}
