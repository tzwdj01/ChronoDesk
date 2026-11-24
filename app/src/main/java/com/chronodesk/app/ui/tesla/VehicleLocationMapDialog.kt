package com.chronodesk.app.ui.tesla

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.model.LastDriveInfo
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.utils.CoordinateConverter
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.resolveChronoPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private object MapTileMemoryCache {
    private val cache = LruCache<String, Bitmap>(36)
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bmp: Bitmap) {
        cache.put(key, bmp)
    }
}

/**
 * 车辆定位详情与迷你地图预览弹窗：
 * - 左侧（60%）：3×3 HTTPS 地图瓦片预览 + 纠偏中心标记（离线或失败自动降级经纬网格）
 * - 右侧（40%）：匹配地名、原始坐标、国内纠偏说明及高德/百度/系统/网页地图一键跳转
 */
@Composable
fun VehicleLocationMapDialog(
    location: VehicleLocation,
    vehicleName: String,
    vehicleState: VehicleState,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    lastDrive: LastDriveInfo? = null
) {
    val context = LocalContext.current
    val lat = location.latitude ?: return
    val lng = location.longitude ?: return
    if (!location.isValid) return

    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val bg = theme.surface
    val cardBg = theme.surfaceElevated
    val border = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val accent = theme.accent

    val inChina = remember(lat, lng) { CoordinateConverter.isInChinaMainland(lat, lng) }
    val uris = remember(lat, lng, location.primaryPlaceName, vehicleName) {
        val label = if (location.geofenceName != null) {
            "$vehicleName · ${location.geofenceName}"
        } else {
            location.resolvedAddress ?: vehicleName
        }
        CoordinateConverter.buildMapLaunchUris(lat, lng, label)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 弹窗顶部栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$vehicleName · 车辆定位与地图",
                            color = primaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { contentDescription = "关闭地图预览" }
                            .clickable(role = Role.Button, onClickLabel = "关闭") { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(cardBg)
                                .border(1.dp, border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧 60%：瓦片迷你地图预览
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, border, RoundedCornerShape(12.dp))
                    ) {
                        VehicleMiniMapPreview(
                            lat = lat,
                            lng = lng,
                            inChina = inChina,
                            isNightMode = isNightMode,
                            accent = accent,
                            cardBg = cardBg,
                            border = border,
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                    }

                    // 右侧 40%：地点详情与外链地图启动器
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 状态与地点名称卡片
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(1.dp, border, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val statusText = when {
                                    vehicleState.isDriving -> "行驶中"
                                    vehicleState.isAsleep -> "休眠停靠"
                                    vehicleState.isCharging -> "充电中"
                                    else -> "最后停泊点"
                                }
                                Text(
                                    text = statusText,
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = location.primaryPlaceName,
                                    color = primaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!location.geofenceName.isNullOrBlank() && !location.resolvedAddress.isNullOrBlank()) {
                                    Text(
                                        text = location.resolvedAddress,
                                        color = secondaryText,
                                        fontSize = 11.5.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 经纬度与采样时间信息
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(1.dp, border, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "WGS-84: %.5f, %.5f", lat, lng),
                                    color = primaryText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (inChina) {
                                        "已自动适配 GCJ-02 / BD-09 国内地图纠偏"
                                    } else {
                                        "使用标准 WGS-84 国际坐标系"
                                    },
                                    color = mutedText,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (location.formattedSampledTime != null) {
                                        "位置采样时间：${location.formattedSampledTime}"
                                    } else {
                                        "位置采样时间：未知"
                                    },
                                    color = secondaryText,
                                    fontSize = 10.5.sp
                                )
                            }

                            if (lastDrive != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cardBg)
                                        .border(1.dp, border, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "最近行程 · ${lastDrive.formattedDistanceKm} km${lastDrive.formattedDuration?.let { " · $it" } ?: ""}",
                                        color = accent,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "起：${lastDrive.resolveStartPlace()}",
                                        color = primaryText,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "终：${lastDrive.resolveEndPlace(location)}",
                                        color = primaryText,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 外部地图跳转按钮区
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "在地图应用中查看",
                                color = secondaryText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            MapActionButton(
                                title = "高德地图",
                                subtitle = "GCJ-02 纠偏 · 未安装自动打开网页版",
                                accent = accent,
                                cardBg = cardBg,
                                border = border,
                                primaryText = primaryText,
                                secondaryText = mutedText,
                                onClick = {
                                    launchMapUriWithFallback(
                                        context = context,
                                        primaryUri = uris.amapAppUri,
                                        fallbackUri = uris.webFallbackUri,
                                        fallbackToast = "未检测到高德地图 App，已切换网页版"
                                    )
                                }
                            )
                            MapActionButton(
                                title = "百度地图",
                                subtitle = "BD-09 纠偏 · 未安装自动切换网页版",
                                accent = if (isPureInk) accent else if (isNightMode) TechEmerald else Color(0xFF16A34A),
                                cardBg = cardBg,
                                border = border,
                                primaryText = primaryText,
                                secondaryText = mutedText,
                                onClick = {
                                    launchMapUriWithFallback(
                                        context = context,
                                        primaryUri = uris.baiduAppUri,
                                        fallbackUri = uris.webFallbackUri,
                                        fallbackToast = "未检测到百度地图 App，已切换网页地图"
                                    )
                                }
                            )
                            MapActionButton(
                                title = "系统地图 / 网页地图",
                                subtitle = "通用 geo 协议或浏览器打开",
                                accent = secondaryText,
                                cardBg = cardBg,
                                border = border,
                                primaryText = primaryText,
                                secondaryText = mutedText,
                                onClick = {
                                    launchMapUriWithFallback(
                                        context = context,
                                        primaryUri = uris.geoUri,
                                        fallbackUri = uris.webFallbackUri,
                                        fallbackToast = "已使用网页地图打开"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleMiniMapPreview(
    lat: Double,
    lng: Double,
    inChina: Boolean,
    isNightMode: Boolean,
    accent: Color,
    cardBg: Color,
    border: Color,
    primaryText: Color,
    secondaryText: Color
) {
    var zoom by remember { mutableIntStateOf(15) }
    var tiles by remember { mutableStateOf<Map<Pair<Int, Int>, ImageBitmap>>(emptyMap()) }
    var isLoadingTiles by remember { mutableStateOf(true) }

    val (tileLat, tileLng) = remember(lat, lng, inChina) {
        if (inChina) CoordinateConverter.wgs84ToGcj02(lat, lng) else lat to lng
    }
    val centerTile = remember(tileLat, tileLng, zoom) {
        CoordinateConverter.latLngToSlippyTile(tileLat, tileLng, zoom)
    }

    LaunchedEffect(centerTile.tileX, centerTile.tileY, zoom, inChina) {
        isLoadingTiles = true
        val n = 1 shl zoom
        val loaded = withContext(Dispatchers.IO) {
            coroutineScope {
                val tasks = (-1..1).flatMap { dx ->
                    (-1..1).map { dy ->
                        async {
                            val tx = ((centerTile.tileX + dx) % n + n) % n
                            val ty = centerTile.tileY + dy
                            if (ty < 0 || ty >= n) return@async null
                            val bmp = fetchOrGetTileBitmap(tx, ty, zoom, inChina)
                            if (bmp != null) (dx to dy) to bmp.asImageBitmap() else null
                        }
                    }
                }
                tasks.awaitAll().filterNotNull().toMap()
            }
        }
        tiles = loaded
        isLoadingTiles = false
    }

    val mapBg = if (isNightMode) DarkBackground else InkBackground
    val gridColor = if (isNightMode) DarkBorder.copy(alpha = 0.45f) else InkBorder.copy(alpha = 0.6f)
    val nightTileFilter = remember(isNightMode) {
        if (isNightMode) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.45f, 0f, 0f, 0f, 5f,
                        0f, 0.48f, 0f, 0f, 8f,
                        0f, 0f, 0.58f, 0f, 16f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mapBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // 底图兜底经纬网格（离线或加载中始终可见）
            val step = 48f
            var gx = cx % step
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += step
            }
            var gy = cy % step
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += step
            }

            // 绘制 3×3 地图瓦片并精确对齐中心偏移
            if (tiles.isNotEmpty()) {
                val tileSizePx = (maxOf(w, h) / 2.0f).coerceAtLeast(180f)
                for ((offsetPair, img) in tiles) {
                    val (dx, dy) = offsetPair
                    val left = cx + (dx - centerTile.offsetX) * tileSizePx
                    val top = cy + (dy - centerTile.offsetY) * tileSizePx
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(tileSizePx.roundToInt() + 1, tileSizePx.roundToInt() + 1),
                        colorFilter = nightTileFilter
                    )
                }
            }

            // 绘制中心车辆定位光晕与标记点
            drawCircle(
                color = accent.copy(alpha = 0.20f),
                radius = 30f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accent,
                radius = 12f,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accent,
                radius = 6.5f,
                center = Offset(cx, cy)
            )
        }

        // 左上角来源标签
        val sourceLabel = when {
            isLoadingTiles && tiles.isEmpty() -> "正在加载地图切片…"
            tiles.isNotEmpty() && inChina -> "高德地图预览 · Z$zoom"
            tiles.isNotEmpty() -> "OSM 地图预览 · Z$zoom"
            else -> "离线坐标雷达视图 · Z$zoom"
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(cardBg.copy(alpha = 0.88f))
                .border(0.8.dp, border, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = sourceLabel,
                color = primaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 右下角缩放按钮（>=48dp 触控热区）
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ZoomButton(
                icon = Icons.Default.Add,
                contentDesc = "放大地图",
                enabled = zoom < 17,
                cardBg = cardBg,
                border = border,
                tint = primaryText,
                mutedTint = secondaryText,
                onClick = { if (zoom < 17) zoom++ }
            )
            ZoomButton(
                icon = Icons.Default.Remove,
                contentDesc = "缩小地图",
                enabled = zoom > 13,
                cardBg = cardBg,
                border = border,
                tint = primaryText,
                mutedTint = secondaryText,
                onClick = { if (zoom > 13) zoom-- }
            )
        }
    }
}

@Composable
private fun ZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    enabled: Boolean,
    cardBg: Color,
    border: Color,
    tint: Color,
    mutedTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = contentDesc }
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDesc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cardBg.copy(alpha = 0.92f))
                .border(1.dp, border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else mutedTint.copy(alpha = 0.35f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun MapActionButton(
    title: String,
    subtitle: String,
    accent: Color,
    cardBg: Color,
    border: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = "打开$title", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = primaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = secondaryText,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(15.dp)
        )
    }
}

private fun fetchOrGetTileBitmap(tx: Int, ty: Int, zoom: Int, inChina: Boolean): Bitmap? {
    val providerKey = if (inChina) "amap" else "osm"
    val cacheKey = "${providerKey}_${zoom}_${tx}_${ty}"
    MapTileMemoryCache.get(cacheKey)?.let { return it }

    val url = if (inChina) {
        "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=$tx&y=$ty&z=$zoom"
    } else {
        "https://tile.openstreetmap.org/$zoom/$tx/$ty.png"
    }
    return try {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "ChronoDesk-Android/1.0")
            .build()
        MapTileMemoryCache.httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bytes = resp.body?.bytes() ?: return null
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            MapTileMemoryCache.put(cacheKey, bmp)
            bmp
        }
    } catch (_: Exception) {
        null
    }
}

private fun launchMapUriWithFallback(
    context: Context,
    primaryUri: String,
    fallbackUri: String,
    fallbackToast: String
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(primaryUri)))
    } catch (_: Exception) {
        try {
            Toast.makeText(context, fallbackToast, Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)))
        } catch (_: Exception) {
            Toast.makeText(context, "未找到可用的地图或浏览器应用", Toast.LENGTH_SHORT).show()
        }
    }
}
