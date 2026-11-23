package com.chronodesk.app.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.chronodesk.app.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.Locale

/**
 * 写实 3D Tesla 车辆外观渲染加载与永久本地缓存管理器。
 * 通过 HTTPS 设计室合成端点按车型与漆面配色实时获取透明背景 3D 车模，
 * 自动裁切多余透明留白后永久缓存至应用私有目录，兼顾超低体积与离线可用性。
 */
object TeslaVehicleRenderLoader {

    private const val TAG = "TeslaRenderLoader"
    const val VIEW_STUD_3QTR = "STUD_3QTR"
    const val VIEW_SIDE = "SIDE"

    private val memoryCache = LruCache<String, ImageBitmap>(12)

    private fun normalizeModelKey(normalizedModel: String): String {
        return when (normalizedModel.uppercase(Locale.US).trim()) {
            "MODEL Y", "MODELY", "Y" -> "my"
            "MODEL S", "MODELS", "S" -> "ms"
            "MODEL X", "MODELX", "X" -> "mx"
            else -> "m3"
        }
    }

    private fun resolveOptionsString(modelKey: String, resolvedColor: String): String {
        val colorKey = resolvedColor.uppercase(Locale.US).trim()
        return when (modelKey) {
            "m3" -> when (colorKey) {
                "RED_CLASSIC" -> "\$MT322,\$PPMR,\$W40B,\$IBB1"
                "WHITE" -> "\$MT351,\$PPSW,\$W38A,\$IBB2"
                "BLACK" -> "\$MT351,\$PBSB,\$W38A,\$IBB2"
                "SILVER" -> "\$MT351,\$PN01,\$W38A,\$IBB2"
                "BLUE" -> "\$MT351,\$PPSB,\$W38A,\$IBB2"
                else -> "\$MT351,\$PR01,\$W38A,\$IBB2" // 烈焰红 Model 3 (Ultra Red)
            }
            "my" -> {
                val paint = when (colorKey) {
                    "RED_CLASSIC" -> "\$PPMR"
                    "WHITE" -> "\$PPSW"
                    "BLACK" -> "\$PBSB"
                    "SILVER" -> "\$PN01"
                    "BLUE" -> "\$PPSB"
                    else -> "\$PR01"
                }
                "\$MTY14,$paint,\$WY19B,\$INPB0"
            }
            "ms" -> {
                val paint = when (colorKey) {
                    "WHITE" -> "\$PPSW"
                    "BLACK" -> "\$PBSB"
                    "SILVER" -> "\$PMNG"
                    "BLUE" -> "\$PPSB"
                    else -> "\$PR01"
                }
                "\$MTS18,$paint,\$WS90,\$IBE00"
            }
            "mx" -> {
                val paint = when (colorKey) {
                    "WHITE" -> "\$PPSW"
                    "BLACK" -> "\$PBSB"
                    "SILVER" -> "\$PMNG"
                    "BLUE" -> "\$PPSB"
                    else -> "\$PR01"
                }
                "\$MTX14,$paint,\$WX01,\$IBE00"
            }
            else -> "\$MT351,\$PR01,\$W38A,\$IBB2"
        }
    }

    /**
     * 生成按优先级排列的纯 HTTPS 实车模型渲染链接（国内 CDN 优先，海外全球节点兜底）。
     */
    fun buildCompositorUrls(
        normalizedModel: String,
        resolvedColor: String,
        view: String = VIEW_STUD_3QTR
    ): List<String> {
        val modelKey = normalizeModelKey(normalizedModel)
        val safeView = if (view == VIEW_SIDE) VIEW_SIDE else VIEW_STUD_3QTR
        val options = resolveOptionsString(modelKey, resolvedColor)
        val encodedOpts = URLEncoder.encode(options, "UTF-8")
        val query = "context=design_studio_2&options=$encodedOpts&view=$safeView&model=$modelKey&size=960&bkba_opt=1"
        return listOf(
            "https://static-assets.tesla.cn/configurator/compositor?$query",
            "https://static-assets.tesla.com/configurator/compositor?$query"
        )
    }

    private fun buildCacheKey(normalizedModel: String, resolvedColor: String, view: String): String {
        val modelKey = normalizeModelKey(normalizedModel)
        val colorKey = resolvedColor.uppercase(Locale.US).trim().replace(Regex("[^A-Z0-9_]"), "_")
        val safeView = if (view == VIEW_SIDE) VIEW_SIDE else VIEW_STUD_3QTR
        return "${modelKey}_${colorKey}_${safeView}"
    }

    private fun getCacheFile(context: Context, key: String): File {
        val dir = File(context.filesDir, "tesla_renders")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$key.png")
    }

    /**
     * 同步读取内存或本地磁盘缓存的实车渲染图（零网络阻塞，供界面首帧立即呈现）
     */
    fun loadCachedBitmap(
        context: Context,
        normalizedModel: String,
        resolvedColor: String,
        view: String = VIEW_STUD_3QTR
    ): ImageBitmap? {
        val key = buildCacheKey(normalizedModel, resolvedColor, view)
        memoryCache.get(key)?.let { return it }

        return try {
            val file = getCacheFile(context, key)
            if (!file.exists() || file.length() == 0L) return null
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val imageBmp = bmp.asImageBitmap()
            memoryCache.put(key, imageBmp)
            imageBmp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 优先命中本地缓存，未命中时从 HTTPS 端点下载透明棚拍车模、裁切留白并持久化至磁盘缓存
     */
    suspend fun fetchOrLoadBitmap(
        context: Context,
        normalizedModel: String,
        resolvedColor: String,
        view: String = VIEW_STUD_3QTR,
        httpClient: OkHttpClient = NetworkClient.weatherFallbackHttpClient
    ): ImageBitmap? {
        loadCachedBitmap(context, normalizedModel, resolvedColor, view)?.let { return it }

        return withContext(Dispatchers.IO) {
            val key = buildCacheKey(normalizedModel, resolvedColor, view)
            val urls = buildCompositorUrls(normalizedModel, resolvedColor, view)
            for (url in urls) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "ChronoDesk-Android/1.0")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use
                        val bytes = resp.body?.bytes() ?: return@use
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val rawBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return@use
                        if (rawBmp.width < 100 || rawBmp.height < 60) return@use

                        val trimmed = trimTransparentPadding(rawBmp, paddingPx = 8)
                        val file = getCacheFile(context, key)
                        file.outputStream().use { out ->
                            trimmed.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val img = trimmed.asImageBitmap()
                        memoryCache.put(key, img)
                        return@withContext img
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Fetch render failed ($url): ${e.message}")
                }
            }
            null
        }
    }

    /**
     * 裁切透明 PNG 四周空白边距，使实车主体在展示卡片中充分放大居中
     */
    fun trimTransparentPadding(src: Bitmap, paddingPx: Int = 8): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
                if (alpha > 12) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return src

        val left = (minX - paddingPx).coerceAtLeast(0)
        val top = (minY - paddingPx).coerceAtLeast(0)
        val right = (maxX + paddingPx).coerceAtMost(w - 1)
        val bottom = (maxY + paddingPx).coerceAtMost(h - 1)
        val cropW = (right - left + 1).coerceAtLeast(1)
        val cropH = (bottom - top + 1).coerceAtLeast(1)

        if (cropW >= w - 4 && cropH >= h - 4) return src
        return Bitmap.createBitmap(src, left, top, cropW, cropH)
    }
}
