package com.chronodesk.app.data.network

import com.google.gson.annotations.SerializedName

data class GrafanaQueryResponse(
    @SerializedName("results")
    val results: Map<String, GrafanaQueryResult>? = null
)

data class GrafanaQueryResult(
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("frames")
    val frames: List<GrafanaDataFrame>? = null,
    @SerializedName("error")
    val error: String? = null
)

data class GrafanaDataFrame(
    @SerializedName("schema")
    val schema: GrafanaSchema? = null,
    @SerializedName("data")
    val data: GrafanaData? = null
)

data class GrafanaSchema(
    @SerializedName("fields")
    val fields: List<GrafanaField>? = null
)

data class GrafanaField(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String? = null
)

data class GrafanaData(
    @SerializedName("values")
    val values: List<List<Any?>>? = null
)

object GrafanaFrameParser {

    /**
     * 将 Grafana 列式 DataFrame (schema.fields + data.values) 转换为行式 Map 集合
     */
    fun parseFramesToRows(frames: List<GrafanaDataFrame>?): List<Map<String, Any?>> {
        if (frames.isNullOrEmpty()) return emptyList()
        val allRows = mutableListOf<Map<String, Any?>>()

        for (frame in frames) {
            val fields = frame.schema?.fields ?: continue
            val columns = frame.data?.values ?: continue
            if (fields.size != columns.size) {
                // 列结构不匹配则跳过该 frame
                continue
            }

            val rowCount = columns.firstOrNull()?.size ?: 0
            for (r in 0 until rowCount) {
                val row = mutableMapOf<String, Any?>()
                for (c in fields.indices) {
                    val name = fields[c].name
                    val colValues = columns[c]
                    val value = if (r < colValues.size) colValues[r] else null
                    row[name] = value
                }
                allRows.add(row)
            }
        }
        return allRows
    }

    /**
     * 从结果集的 RefId (默认 "A") 中解析所有行
     */
    fun parseResultToRows(response: GrafanaQueryResponse?, refId: String = "A"): List<Map<String, Any?>> {
        val result = response?.results?.get(refId) ?: return emptyList()
        if (!result.error.isNullOrBlank()) {
            throw IllegalArgumentException("SQL 查询失败: ${result.error}")
        }
        if (result.status != null && result.status >= 400) {
            throw IllegalArgumentException("SQL 查询失败 (状态码 ${result.status})")
        }
        return parseFramesToRows(result.frames)
    }

    // --- 安全类型转换扩展 ---

    fun Map<String, Any?>.getString(key: String): String? {
        val v = this[key] ?: return null
        return v.toString()
    }

    fun Map<String, Any?>.getDouble(key: String): Double? {
        val v = this[key] ?: return null
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    fun Map<String, Any?>.getInt(key: String): Int? {
        val v = this[key] ?: return null
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    fun Map<String, Any?>.getLong(key: String): Long? {
        val v = this[key] ?: return null
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toDoubleOrNull()?.toLong()
            else -> null
        }
    }
}
