package com.chronodesk.app.data

import com.chronodesk.app.data.network.GrafanaData
import com.chronodesk.app.data.network.GrafanaDataFrame
import com.chronodesk.app.data.network.GrafanaField
import com.chronodesk.app.data.network.GrafanaFrameParser
import com.chronodesk.app.data.network.GrafanaFrameParser.getDouble
import com.chronodesk.app.data.network.GrafanaFrameParser.getInt
import com.chronodesk.app.data.network.GrafanaFrameParser.getString
import com.chronodesk.app.data.network.GrafanaQueryResponse
import com.chronodesk.app.data.network.GrafanaQueryResult
import com.chronodesk.app.data.network.GrafanaSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrafanaFrameParserTest {

    @Test
    fun testParseStandardDataFrame() {
        val schema = GrafanaSchema(
            fields = listOf(
                GrafanaField(name = "id", type = "number"),
                GrafanaField(name = "name", type = "string"),
                GrafanaField(name = "efficiency", type = "number")
            )
        )
        val data = GrafanaData(
            values = listOf(
                listOf(1, 2),
                listOf("Model Y", "Model 3"),
                listOf(0.152, 0.145)
            )
        )
        val frame = GrafanaDataFrame(schema = schema, data = data)

        val rows = GrafanaFrameParser.parseFramesToRows(listOf(frame))
        assertEquals(2, rows.size)

        val row0 = rows[0]
        assertEquals(1, row0.getInt("id"))
        assertEquals("Model Y", row0.getString("name"))
        assertEquals(0.152, row0.getDouble("efficiency") ?: 0.0, 0.0001)

        val row1 = rows[1]
        assertEquals(2, row1.getInt("id"))
        assertEquals("Model 3", row1.getString("name"))
        assertEquals(0.145, row1.getDouble("efficiency") ?: 0.0, 0.0001)
    }

    @Test
    fun testEmptyFramesAndNullData() {
        assertTrue(GrafanaFrameParser.parseFramesToRows(null).isEmpty())
        assertTrue(GrafanaFrameParser.parseFramesToRows(emptyList()).isEmpty())

        val frameWithoutData = GrafanaDataFrame(
            schema = GrafanaSchema(fields = listOf(GrafanaField("col1"))),
            data = null
        )
        assertTrue(GrafanaFrameParser.parseFramesToRows(listOf(frameWithoutData)).isEmpty())

        val frameWithMismatchedCols = GrafanaDataFrame(
            schema = GrafanaSchema(fields = listOf(GrafanaField("c1"), GrafanaField("c2"))),
            data = GrafanaData(values = listOf(listOf("val1"))) // 仅1列但有2个字段
        )
        assertTrue(GrafanaFrameParser.parseFramesToRows(listOf(frameWithMismatchedCols)).isEmpty())
    }

    @Test
    fun testTypeConversions() {
        val row = mapOf<String, Any?>(
            "str_num" to "123.45",
            "int_val" to 42,
            "null_val" to null,
            "bool_val" to true
        )

        assertEquals("123.45", row.getString("str_num"))
        assertEquals(123.45, row.getDouble("str_num") ?: 0.0, 0.001)
        assertEquals(123, row.getInt("str_num"))

        assertEquals(42, row.getInt("int_val"))
        assertEquals(42.0, row.getDouble("int_val") ?: 0.0, 0.001)

        assertNull(row.getString("null_val"))
        assertNull(row.getDouble("null_val"))
        assertNull(row.getInt("null_val"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testQueryErrorThrowsException() {
        val response = GrafanaQueryResponse(
            results = mapOf(
                "A" to GrafanaQueryResult(
                    status = 500,
                    error = "pq: relation \"cars\" does not exist"
                )
            )
        )
        GrafanaFrameParser.parseResultToRows(response, "A")
    }
}
