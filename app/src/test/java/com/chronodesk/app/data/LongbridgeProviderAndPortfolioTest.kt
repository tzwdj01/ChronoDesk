package com.chronodesk.app.data

import com.chronodesk.app.data.model.LongbridgeCashItem
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeRatePair
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.provider.LongbridgeAuthResult
import com.chronodesk.app.data.provider.LongbridgeDataProvider
import com.chronodesk.app.data.provider.LongbridgeFetchResult
import com.chronodesk.app.data.security.TokenRedactor
import com.chronodesk.app.ui.longbridge.resolveLongbridgePnlColor
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class LongbridgeProviderAndPortfolioTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: LongbridgeDataProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        provider = LongbridgeDataProvider(
            httpClient = OkHttpClient.Builder().build(),
            mcpBaseUrlOverride = baseUrl,
            oauthBaseUrlOverride = "$baseUrl/oauth2"
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `authenticate tool parses SSE response and extracts Bearer token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/event-stream")
                .setBody(
                    "event: message\n" +
                        "data: " + JSONObject().apply {
                            put("jsonrpc", "2.0")
                            put("id", 1)
                            put("result", JSONObject().apply {
                                put("isError", false)
                                put("content", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put(
                                            "text",
                                            "Authentication succeeded! Add header `Authorization: Bearer lb_mcp_secret_token_987654` to connect."
                                        )
                                    })
                                })
                            })
                        }.toString() + "\n\n"
                )
        )

        val result = provider.exchangeOneTimeAuthCode("CN", "valid_code_123")
        assertTrue(result is LongbridgeAuthResult.Success)
        val success = result as LongbridgeAuthResult.Success
        assertEquals("lb_mcp_secret_token_987654", success.accessToken)

        val req = server.takeRequest()
        assertEquals("/agent", req.path)
        val reqJson = JSONObject(req.body.readUtf8())
        assertEquals("tools/call", reqJson.getString("method"))
        val params = reqJson.getJSONObject("params")
        assertEquals("authenticate", params.getString("name"))
        assertEquals("valid_code_123", params.getJSONObject("arguments").getString("auth_code"))
    }

    @Test
    fun `fetchSnapshot aggregates MCP tools, currency rates, and intraday trade-adjusted PnL`() = runTest {
        installStandardMcpDispatcher(
            executionsResponseProvider = {
                okJsonEnvelope(
                    JSONObject().put(
                        "trades",
                        JSONArray().apply {
                            put(JSONObject().apply {
                                put("symbol", "0700.HK")
                                put("side", "Buy")
                                put("quantity", "50")
                                put("price", "405.00")
                            })
                            put(JSONObject().apply {
                                put("symbol", "0700.HK")
                                put("side", "Sell")
                                put("quantity", "20")
                                put("price", "415.00")
                            })
                        }
                    ).toString()
                )
            },
            includeUsPostMarket = false
        )

        val res = provider.fetchSnapshot(
            accessToken = "tok_test_abc",
            settings = LongbridgeSettings(
                enabled = true,
                displayCurrency = "HKD",
                includeExtendedHours = true
            )
        )

        assertTrue("Expected Success but got $res", res is LongbridgeFetchResult.Success)
        val snap = (res as LongbridgeFetchResult.Success).snapshot

        assertEquals("ACC-8801", snap.accountId)
        assertEquals("港美主账户", snap.accountName)
        assertEquals("HKD", snap.baseCurrency)
        assertEquals(200_000.0, snap.netAssets ?: 0.0, 0.01)
        assertEquals(45_000.0, snap.totalCash ?: 0.0, 0.01)
        assertEquals(120_000.0, snap.buyPower ?: 0.0, 0.01)
        assertEquals(32_500.0, snap.cumulativePl ?: 0.0, 0.01)

        // 0700.HK: qty=130, init=100, prevClose=400, lastDone=412, buy=50@405, sell=20@415
        // todayPl = 130*412 - 100*400 + 8300 - 20250 = 53560 - 40000 + 8300 - 20250 = 1610.0 HKD
        val tencent = snap.positions.first { it.symbol == "0700.HK" }
        assertEquals(1610.0, tencent.todayPl ?: 0.0, 0.01)
        // MarketValue = 130 * 412 = 53560.0
        assertEquals(53_560.0, tencent.marketValue ?: 0.0, 0.01)

        // AAPL.US: qty=10, init=10, prevClose=220, lastDone=225 -> +50 USD = +389 HKD (@7.78)
        val aapl = snap.positions.first { it.symbol == "AAPL.US" }
        assertEquals(50.0, aapl.todayPl ?: 0.0, 0.01)

        // Total Today P&L in HKD = 1610 + 50 * 7.78 = 1999.0
        assertEquals(1_999.0, snap.todayPl ?: 0.0, 0.05)
        // Prev Equity = 200000 - 1999 = 198001 -> rate ~= 1.00959%
        assertEquals((1_999.0 / 198_001.0) * 100.0, snap.todayPlRatePercent ?: 0.0, 0.02)
    }

    @Test
    fun `US extended hours quote is selected only when includeExtendedHours is true`() = runTest {
        installStandardMcpDispatcher(
            executionsResponseProvider = { okJsonEnvelope("{\"trades\":[]}") },
            includeUsPostMarket = true
        )

        val withExt = provider.fetchSnapshot(
            accessToken = "tok_test_abc",
            settings = LongbridgeSettings(enabled = true, includeExtendedHours = true)
        ) as LongbridgeFetchResult.Success
        val aaplExt = withExt.snapshot.positions.first { it.symbol == "AAPL.US" }
        assertEquals(231.0, aaplExt.lastDone ?: 0.0, 0.01)
        assertEquals("盘后", aaplExt.sessionTag)
        assertEquals(110.0, aaplExt.todayPl ?: 0.0, 0.01) // (231 - 220) * 10

        val regOnly = provider.fetchSnapshot(
            accessToken = "tok_test_abc",
            settings = LongbridgeSettings(enabled = true, includeExtendedHours = false)
        ) as LongbridgeFetchResult.Success
        val aaplReg = regOnly.snapshot.positions.first { it.symbol == "AAPL.US" }
        assertEquals(225.0, aaplReg.lastDone ?: 0.0, 0.01)
        assertNull(aaplReg.sessionTag)
        assertEquals(50.0, aaplReg.todayPl ?: 0.0, 0.01) // (225 - 220) * 10
    }

    @Test
    fun `missing scope 403308 on today_executions degrades gracefully without failing snapshot`() = runTest {
        installStandardMcpDispatcher(
            executionsResponseProvider = {
                errorJsonEnvelope("{\"code\":403308,\"message\":\"missing trade.read scope\"}")
            },
            includeUsPostMarket = false
        )

        val res = provider.fetchSnapshot(
            accessToken = "tok_test_abc",
            settings = LongbridgeSettings(enabled = true, includeExtendedHours = false)
        )
        assertTrue("Expected graceful Success on 403308, got $res", res is LongbridgeFetchResult.Success)
        val snap = (res as LongbridgeFetchResult.Success).snapshot
        // Falls back to simple quantity * (lastDone - prevClose) = 130 * (412 - 400) = 1560.0
        val tencent = snap.positions.first { it.symbol == "0700.HK" }
        assertEquals(1_560.0, tencent.todayPl ?: 0.0, 0.01)
    }

    @Test
    fun `HTTP 401 returns AuthExpired and OAuth refresh token flow succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_token\"}"))
        val fetchRes = provider.fetchSnapshot("expired_token", LongbridgeSettings(enabled = true))
        assertTrue(fetchRes is LongbridgeFetchResult.AuthExpired)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    JSONObject().apply {
                        put("access_token", "new_access_tok_555")
                        put("refresh_token", "new_refresh_tok_666")
                        put("expires_in", 3600)
                    }.toString()
                )
        )
        val refreshRes = provider.refreshAccessToken("CN", "client_abc", "old_refresh_tok")
        assertTrue(refreshRes is LongbridgeAuthResult.Success)
        val refreshed = refreshRes as LongbridgeAuthResult.Success
        assertEquals("new_access_tok_555", refreshed.accessToken)
        assertEquals("new_refresh_tok_666", refreshed.refreshToken)
    }

    @Test
    fun `PKCE S256 generation, callback state check, credential JSON parser, and secret redaction`() {
        val pkce = provider.generatePkcePair()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(pkce.codeVerifier.toByteArray(Charsets.US_ASCII))
        val expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        assertEquals(expectedChallenge, pkce.codeChallenge)

        val validCb = provider.extractCodeFromCallbackInput(
            "http://127.0.0.1:41234/callback?code=auth_code_99&state=${pkce.state}",
            expectedState = pkce.state
        )
        assertEquals("auth_code_99", validCb.getOrNull())

        val mismatchCb = provider.extractCodeFromCallbackInput(
            "http://127.0.0.1:41234/callback?code=auth_code_99&state=evil_state",
            expectedState = pkce.state
        )
        assertTrue(mismatchCb.isFailure)

        val cred = LongbridgeDataProvider.parseCredentialInput(
            """{"access_token":"tok_a","refresh_token":"tok_r","client_id":"cid_1","expires_in":7200}"""
        )
        assertNotNull(cred)
        assertEquals("tok_a", cred!!.accessToken)
        assertEquals("tok_r", cred.refreshToken)
        assertEquals("cid_1", cred.clientId)

        val camelCred = LongbridgeDataProvider.parseCredentialInput(
            """{"accessToken":"tok_camel","refreshToken":"rt_camel","clientId":"cid_camel"}"""
        )
        assertEquals("tok_camel", camelCred?.accessToken)
        assertEquals("rt_camel", camelCred?.refreshToken)
        assertEquals("cid_camel", camelCred?.clientId)

        val directDataCred = LongbridgeDataProvider.parseCredentialInput("""{"data":"tok_direct_string_987"}""")
        assertEquals("tok_direct_string_987", directDataCred?.accessToken)

        assertNull(LongbridgeDataProvider.extractTokenFromAuthenticateText("""{"error":"invalid_params"}"""))
        assertNull(LongbridgeDataProvider.extractTokenFromAuthenticateText("""[{"msg":"bad"}]"""))

        val rawLog = """{"auth_code":"secret_1234","registration_access_token":"reg_tok_abcd"}"""
        val redacted = TokenRedactor.redactJsonString(rawLog)
        assertFalse(redacted.contains("secret_1234"))
        assertFalse(redacted.contains("reg_tok_abcd"))
    }

    @Test
    fun `reprojectToCurrency, privacy masking, capsule formatters, and Red-Up Green-Down color scheme switch`() {
        val rates = listOf(
            LongbridgeRatePair("HKD", "USD", 7.80),
            LongbridgeRatePair("HKD", "CNY", 1.08)
        )
        val snapHkd = LongbridgeFullSnapshot(
            baseCurrency = "HKD",
            displayCurrency = "HKD",
            netAssets = 78_000.0,
            totalCash = 15_600.0,
            todayPl = 780.0,
            todayPlRatePercent = 1.01,
            rawNetAssetsInBase = 78_000.0,
            rawTotalCashInBase = 15_600.0,
            rawTodayPlInBase = 780.0,
            exchangeRates = rates,
            cashDetails = listOf(LongbridgeCashItem("HKD", availableCash = 15_600.0))
        )

        val usdView = snapHkd.reprojectToCurrency("USD")
        assertEquals("USD", usdView.displayCurrency)
        assertEquals(10_000.0, usdView.netAssets ?: 0.0, 0.01)
        assertEquals(2_000.0, usdView.totalCash ?: 0.0, 0.01)
        assertEquals(100.0, usdView.todayPl ?: 0.0, 0.01)

        // Masking hides monetary amounts while preserving rate percent
        assertEquals("HK$ ****", snapHkd.formattedNetAssets(masked = true))
        assertEquals("HK$ ****", snapHkd.formattedTodayPl(masked = true))
        assertEquals("+1.01%", snapHkd.formattedTodayPlRate())
        assertEquals("HK$78,000.00", snapHkd.formattedNetAssets(masked = false))

        // Capsule Today P&L formatting (normal, compact >= 100 & >= 100_000 & < 100, masked)
        assertEquals("+HK$780.00", snapHkd.formattedCapsuleTodayPl(masked = false, compact = false))
        assertEquals("+HK$780", snapHkd.formattedCapsuleTodayPl(masked = false, compact = true))
        assertEquals("+HK$12.35", snapHkd.copy(todayPl = 12.35).formattedCapsuleTodayPl(masked = false, compact = true))
        assertEquals("HK$****", snapHkd.formattedCapsuleTodayPl(masked = true, compact = true))
        val largeSnap = snapHkd.copy(todayPl = -123_456.0)
        assertEquals("-HK$123,456.00", largeSnap.formattedCapsuleTodayPl(masked = false, compact = false))
        assertEquals("-HK$12.3万", largeSnap.formattedCapsuleTodayPl(masked = false, compact = true))

        // Compact secondary metric formatters
        assertEquals("HK$37.8万", LongbridgeFullSnapshot.formatUnsignedCompactMetric(378_178.96, "HKD"))
        assertEquals("+HK$4.3万", LongbridgeFullSnapshot.formatSignedCompactMoney(42_680.50, "HKD"))

        // Color scheme switching: Red-Up/Green-Down vs Green-Up/Red-Down
        assertEquals(InkWarningRed, resolveLongbridgePnlColor(100.0, isRedUpGreenDown = true, isNightMode = false))
        assertEquals(InkStatusGreen, resolveLongbridgePnlColor(-100.0, isRedUpGreenDown = true, isNightMode = false))
        assertEquals(InkStatusGreen, resolveLongbridgePnlColor(100.0, isRedUpGreenDown = false, isNightMode = false))
        assertEquals(InkWarningRed, resolveLongbridgePnlColor(-100.0, isRedUpGreenDown = false, isNightMode = false))
        assertEquals(TechRose, resolveLongbridgePnlColor(50.0, isRedUpGreenDown = true, isNightMode = true))
        assertEquals(TechEmerald, resolveLongbridgePnlColor(50.0, isRedUpGreenDown = false, isNightMode = true))
    }

    @Test
    fun `fetchSnapshot parses US HK CN benchmark indices and market temperatures with graceful partial fallback`() = runTest {
        installStandardMcpDispatcher(
            executionsResponseProvider = { okJsonEnvelope("{\"trades\":[]}") },
            includeUsPostMarket = false,
            marketTemperatureProvider = { mkt ->
                when (mkt) {
                    "US" -> okJsonEnvelope("""{"data":{"temperature":70,"description":"Warm","valuation":59,"sentiment":82,"updated_at":"1732435200"}}""")
                    "HK" -> okJsonEnvelope("""{"temperature":46,"description":"Moderate","valuation":38,"sentiment":52}""")
                    else -> errorJsonEnvelope("""{"code":403308,"message":"missing quote scope"}""")
                }
            }
        )

        val res = provider.fetchSnapshot("tok_test_abc", LongbridgeSettings(enabled = true))
        assertTrue("Expected Success even when CN market_temperature fails: $res", res is LongbridgeFetchResult.Success)
        val snap = (res as LongbridgeFetchResult.Success).snapshot

        assertEquals(7, snap.safeMarketIndices.size)
        val nasdaq = snap.safeMarketIndices.first { it.symbol == ".IXIC.US" }
        assertEquals("纳指", nasdaq.shortName)
        assertEquals("US", nasdaq.market)
        assertEquals(1.0, nasdaq.changeRatePercent ?: 0.0, 0.01)

        val hsi = snap.safeMarketIndices.first { it.symbol == "HSI.HK" }
        assertEquals(-0.50, hsi.changeRatePercent ?: 0.0, 0.01)

        assertEquals(2, snap.safeMarketTemperatures.size)
        val usTemp = snap.safeMarketTemperatures.first { it.market == "US" }
        assertEquals(70, usTemp.temperature)
        assertEquals("偏热", usTemp.statusLabel)
        assertEquals(59, usTemp.valuation)
        assertEquals(82, usTemp.sentiment)
    }

    private fun installStandardMcpDispatcher(
        executionsResponseProvider: () -> String,
        includeUsPostMarket: Boolean,
        marketTemperatureProvider: (String) -> String = { mkt ->
            when (mkt) {
                "US" -> okJsonEnvelope("""{"temperature":70,"valuation":59,"sentiment":82}""")
                "HK" -> okJsonEnvelope("""{"temperature":46,"valuation":38,"sentiment":52}""")
                else -> okJsonEnvelope("""{"temperature":58,"valuation":49,"sentiment":66}""")
            }
        }
    ) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = request.body.readUtf8()
                val reqJson = JSONObject(body)
                val toolName = reqJson.getJSONObject("params").getString("name")
                val respBody = when (toolName) {
                    "account_balance" -> okJsonEnvelope(
                        JSONObject().put(
                            "list",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("currency", "HKD")
                                    put("net_assets", "200000.00")
                                    put("total_cash", "45000.00")
                                    put("buy_power", "120000.00")
                                    put("risk_level", "0")
                                    put(
                                        "cash_infos",
                                        JSONArray().put(
                                            JSONObject().apply {
                                                put("currency", "HKD")
                                                put("available_cash", "45000.00")
                                            }
                                        )
                                    )
                                }
                            )
                        ).toString()
                    )

                    "stock_positions" -> okJsonEnvelope(
                        JSONObject().put(
                            "list",
                            JSONArray().put(
                                JSONObject().put(
                                    "stock_info",
                                    JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("symbol", "0700.HK")
                                            put("symbol_name", "腾讯控股")
                                            put("market", "HK")
                                            put("currency", "HKD")
                                            put("quantity", "130")
                                            put("init_quantity", "100")
                                            put("cost_price", "360.0")
                                        })
                                        put(JSONObject().apply {
                                            put("symbol", "AAPL.US")
                                            put("symbol_name", "Apple")
                                            put("market", "US")
                                            put("currency", "USD")
                                            put("quantity", "10")
                                            put("init_quantity", "10")
                                            put("cost_price", "200.0")
                                        })
                                    }
                                )
                            )
                        ).toString()
                    )

                    "exchange_rate" -> okJsonEnvelope(
                        JSONObject().put(
                            "exchanges",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("base_currency", "HKD")
                                    put("other_currency", "USD")
                                    put("average_rate", "7.78")
                                }
                            )
                        ).toString()
                    )

                    "get_me" -> okJsonEnvelope(
                        JSONObject().apply {
                            put("account_no", "ACC-8801")
                            put("nickname", "港美主账户")
                            put("dc_region", "CN")
                        }.toString()
                    )

                    "quote" -> {
                        val reqSyms = reqJson.getJSONObject("params").optJSONObject("arguments")
                            ?.optJSONArray("symbols")?.let { arr ->
                                (0 until arr.length()).map { idx -> arr.getString(idx) }
                            }.orEmpty()
                        val listArr = JSONArray()
                        if (reqSyms.any { it == "0700.HK" || it == "AAPL.US" }) {
                            listArr.put(JSONObject().apply {
                                put("symbol", "0700.HK")
                                put("last_done", "412.00")
                                put("prev_close", "400.00")
                                put("timestamp", "1732435200")
                            })
                            listArr.put(JSONObject().apply {
                                put("symbol", "AAPL.US")
                                put("last_done", "225.00")
                                put("prev_close", "220.00")
                                put("timestamp", "1732435200")
                                if (includeUsPostMarket) {
                                    put("post_market", JSONObject().apply {
                                        put("last_done", "231.00")
                                        put("prev_close", "220.00")
                                        put("timestamp", "1732442400")
                                    })
                                }
                            })
                        }
                        if (reqSyms.any { it == ".IXIC.US" || it == "HSI.HK" || it == "000001.SH" }) {
                            listOf(
                                Triple(".IXIC.US", "19190.00", "19000.00"),
                                Triple(".SPX.US", "6000.00", "5970.00"),
                                Triple(".DJI.US", "44400.00", "44000.00"),
                                Triple("HSI.HK", "19900.00", "20000.00"),
                                Triple("HSTECH.HK", "4200.00", "4250.00"),
                                Triple("000001.SH", "3300.00", "3250.00"),
                                Triple("000300.SH", "3900.00", "3860.00")
                            ).forEach { (sym, last, prev) ->
                                listArr.put(JSONObject().apply {
                                    put("symbol", sym)
                                    put("last_done", last)
                                    put("prev_close", prev)
                                })
                            }
                        }
                        okJsonEnvelope(JSONObject().put("list", listArr).toString())
                    }

                    "market_temperature" -> {
                        val mkt = reqJson.getJSONObject("params").optJSONObject("arguments")
                            ?.optString("market").orEmpty().ifBlank { "US" }
                        marketTemperatureProvider(mkt)
                    }

                    "today_executions" -> executionsResponseProvider()
                    "fund_position_overview" -> okJsonEnvelope(JSONObject().put("list", JSONArray()).toString())
                    "profit_analysis" -> okJsonEnvelope(
                        JSONObject().put(
                            "summary",
                            JSONObject().apply {
                                put("sum_profit", "32500.00")
                                put("currency", "HKD")
                            }
                        ).toString()
                    )

                    else -> errorJsonEnvelope("unknown tool $toolName")
                }
                return MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(respBody)
            }
        }
    }

    private fun okJsonEnvelope(payloadText: String): String {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("result", JSONObject().apply {
                put("isError", false)
                put("content", JSONArray().put(
                    JSONObject().apply {
                        put("type", "text")
                        put("text", payloadText)
                    }
                ))
            })
        }.toString()
    }

    private fun errorJsonEnvelope(errText: String): String {
        return JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("result", JSONObject().apply {
                put("isError", true)
                put("content", JSONArray().put(
                    JSONObject().apply {
                        put("type", "text")
                        put("text", errText)
                    }
                ))
            })
        }.toString()
    }
}
