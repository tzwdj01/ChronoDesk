package com.chronodesk.app.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class LongbridgeColorScheme {
    RED_UP_GREEN_DOWN,
    GREEN_UP_RED_DOWN
}

data class LongbridgeSettings(
    val enabled: Boolean = false,
    val showOnHome: Boolean = true,
    val displayCurrency: String = "HKD", // HKD, USD, CNY, ACCOUNT
    val colorScheme: String = "RED_UP_GREEN_DOWN", // RED_UP_GREEN_DOWN, GREEN_UP_RED_DOWN
    val includeExtendedHours: Boolean = true,
    val privacyMaskAmounts: Boolean = false,
    val region: String = "CN", // CN, GLOBAL
    val refreshIntervalSec: Int = 60,
    val accountAlias: String = "长桥证券",
    val clientId: String = "",
    val callbackPort: Int = 0
) {
    val isRedUpGreenDown: Boolean
        get() = colorScheme != "GREEN_UP_RED_DOWN"

    val normalizedDisplayCurrency: String
        get() = when (displayCurrency.trim().uppercase(Locale.US)) {
            "USD", "HKD", "CNY", "ACCOUNT" -> displayCurrency.trim().uppercase(Locale.US)
            "CNH" -> "CNY"
            else -> "HKD"
        }

    val normalizedRegion: String
        get() = if (region.equals("GLOBAL", ignoreCase = true)) "GLOBAL" else "CN"
}

data class LongbridgeRatePair(
    val baseCurrency: String,
    val otherCurrency: String,
    val averageRate: Double
)

data class LongbridgeCashItem(
    val currency: String,
    val availableCash: Double? = null,
    val withdrawCash: Double? = null,
    val frozenCash: Double? = null,
    val settlingCash: Double? = null
)

data class LongbridgePositionItem(
    val symbol: String,
    val symbolName: String,
    val market: String,
    val currency: String,
    val quantity: Double,
    val initQuantity: Double? = null,
    val costPrice: Double? = null,
    val lastDone: Double? = null,
    val prevClose: Double? = null,
    val marketValue: Double? = null,
    val todayPl: Double? = null,
    val todayPlRatePercent: Double? = null,
    val totalPl: Double? = null,
    val sessionTag: String? = null
)

data class LongbridgeConnectionState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val isAuthExpired: Boolean = false,
    val lastSuccessTime: Long? = null,
    val errorMessage: String? = null
)

data class LongbridgeIndexQuote(
    val symbol: String,
    val shortName: String,
    val market: String, // "US", "HK", "CN"
    val lastDone: Double,
    val prevClose: Double? = null,
    val changeVal: Double? = null,
    val changeRatePercent: Double? = null
) {
    fun formattedLevel(): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = if (abs(lastDone) >= 10_000.0) {
            DecimalFormat("#,##0.0", symbols)
        } else {
            DecimalFormat("#,##0.00", symbols)
        }
        return df.format(lastDone)
    }

    fun formattedChangeRate(): String = LongbridgeFullSnapshot.formatSignedPercent(changeRatePercent)
}

data class LongbridgeMarketTemp(
    val market: String, // "US", "HK", "CN"
    val temperature: Int, // 0..100
    val description: String = "",
    val valuation: Int? = null,
    val sentiment: Int? = null,
    val updatedAt: Long? = null
) {
    val statusLabel: String
        get() = when {
            temperature >= 70 -> "偏热"
            temperature >= 55 -> "偏暖"
            temperature >= 40 -> "温和"
            temperature >= 25 -> "偏冷"
            else -> "冰点"
        }
}

data class LongbridgeFullSnapshot(
    val accountId: String? = null,
    val accountName: String? = null,
    val dcRegion: String? = null,
    val baseCurrency: String = "HKD",
    val displayCurrency: String = "HKD",
    val netAssets: Double? = null,
    val totalCash: Double? = null,
    val buyPower: Double? = null,
    val marketValueTotal: Double? = null,
    val todayPl: Double? = null,
    val todayPlRatePercent: Double? = null,
    val cumulativePl: Double? = null,
    val riskLevel: Int? = null,
    val marginCall: Double? = null,
    val cashDetails: List<LongbridgeCashItem> = emptyList(),
    val positions: List<LongbridgePositionItem> = emptyList(),
    val marketIndices: List<LongbridgeIndexQuote> = emptyList(),
    val marketTemperatures: List<LongbridgeMarketTemp> = emptyList(),
    val rawNetAssetsInBase: Double? = null,
    val rawTotalCashInBase: Double? = null,
    val rawBuyPowerInBase: Double? = null,
    val rawMarketValueInBase: Double? = null,
    val rawTodayPlInBase: Double? = null,
    val rawCumulativePlInBase: Double? = null,
    val rawCumulativeCurrency: String? = null,
    val exchangeRates: List<LongbridgeRatePair> = emptyList(),
    val isMock: Boolean = false,
    val connectionState: LongbridgeConnectionState = LongbridgeConnectionState(),
    val queriedAt: Long = 0L
) {
    @Suppress("UNCHECKED_CAST")
    val safeMarketIndices: List<LongbridgeIndexQuote>
        get() = (marketIndices as List<LongbridgeIndexQuote>?) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    val safeMarketTemperatures: List<LongbridgeMarketTemp>
        get() = (marketTemperatures as List<LongbridgeMarketTemp>?) ?: emptyList()

    val hasValidAssetData: Boolean
        get() = netAssets != null || totalCash != null || positions.isNotEmpty()

    val riskLevelLabel: String
        get() = when (riskLevel) {
            0 -> "安全"
            1 -> "稳健"
            2 -> "预警"
            3 -> "危险"
            else -> "正常"
        }

    val formattedSyncTime: String
        get() {
            val ts = connectionState.lastSuccessTime ?: queriedAt
            if (ts <= 0L) return "未同步"
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        }

    fun reprojectToCurrency(targetCurrencyMode: String): LongbridgeFullSnapshot {
        val rawBase = normalizeCurrency(baseCurrency.ifBlank { displayCurrency.ifBlank { "HKD" } })
        val effectiveTarget = if (targetCurrencyMode.equals("ACCOUNT", ignoreCase = true) || targetCurrencyMode.isBlank()) {
            rawBase
        } else {
            normalizeCurrency(targetCurrencyMode)
        }

        val srcNet = rawNetAssetsInBase ?: netAssets
        val srcCash = rawTotalCashInBase ?: totalCash
        val srcBuy = rawBuyPowerInBase ?: buyPower
        val srcMv = rawMarketValueInBase ?: marketValueTotal
        val srcPl = rawTodayPlInBase ?: todayPl
        val srcCum = rawCumulativePlInBase ?: cumulativePl
        val srcCumCurr = normalizeCurrency(rawCumulativeCurrency ?: rawBase)

        return copy(
            displayCurrency = effectiveTarget,
            netAssets = srcNet?.let { convertAmount(it, rawBase, effectiveTarget, exchangeRates) },
            totalCash = srcCash?.let { convertAmount(it, rawBase, effectiveTarget, exchangeRates) },
            buyPower = srcBuy?.let { convertAmount(it, rawBase, effectiveTarget, exchangeRates) },
            marketValueTotal = srcMv?.let { convertAmount(it, rawBase, effectiveTarget, exchangeRates) },
            todayPl = srcPl?.let { convertAmount(it, rawBase, effectiveTarget, exchangeRates) },
            cumulativePl = srcCum?.let { convertAmount(it, srcCumCurr, effectiveTarget, exchangeRates) }
        )
    }

    fun formattedNetAssets(masked: Boolean): String = formatMoney(netAssets, displayCurrency, masked)

    fun formattedTotalCash(masked: Boolean): String = formatMoney(totalCash, displayCurrency, masked)

    fun formattedBuyPower(masked: Boolean): String = formatMoney(buyPower, displayCurrency, masked)

    fun formattedMarketValue(masked: Boolean): String = formatMoney(marketValueTotal, displayCurrency, masked)

    fun formattedTodayPl(masked: Boolean): String = formatSignedMoney(todayPl, displayCurrency, masked)

    fun formattedCapsuleTodayPl(masked: Boolean, compact: Boolean = false): String {
        val sym = currencySymbol(displayCurrency)
        if (masked) return "$sym****"
        val value = todayPl ?: return "—"
        if (!value.isFinite()) return "—"
        if (compact) {
            val absVal = abs(value)
            val sign = when {
                value > 0.004 -> "+"
                value < -0.004 -> "-"
                else -> ""
            }
            if (absVal >= 100_000.0) {
                val df1 = DecimalFormat("#,##0.0", DecimalFormatSymbols(Locale.US))
                return "$sign$sym${df1.format(absVal / 10_000.0)}万"
            }
            if (absVal >= 100.0) {
                val df0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
                return "$sign$sym${df0.format(absVal)}"
            }
        }
        return formatSignedMoney(value, displayCurrency, masked = false)
    }

    fun formattedCumulativePl(masked: Boolean): String = formatSignedMoney(cumulativePl, displayCurrency, masked)

    fun formattedTodayPlRate(): String = formatSignedPercent(todayPlRatePercent)

    fun positionTodayPlInDisplay(pos: LongbridgePositionItem): Double? {
        val raw = pos.todayPl ?: return null
        return convertAmount(raw, pos.currency, displayCurrency, exchangeRates)
    }

    fun positionMarketValueInDisplay(pos: LongbridgePositionItem): Double? {
        val raw = pos.marketValue ?: return null
        return convertAmount(raw, pos.currency, displayCurrency, exchangeRates)
    }

    companion object {
        private val MONEY_SYMBOLS = mapOf(
            "HKD" to "HK$",
            "USD" to "US$",
            "CNY" to "¥",
            "CNH" to "¥",
            "SGD" to "S$"
        )

        fun normalizeCurrency(code: String?): String {
            val upper = code?.trim()?.uppercase(Locale.US).orEmpty()
            return when {
                upper.isBlank() -> "HKD"
                upper == "CNH" || upper == "RMB" -> "CNY"
                else -> upper
            }
        }

        fun currencySymbol(code: String?): String {
            val norm = normalizeCurrency(code)
            return MONEY_SYMBOLS[norm] ?: "$norm "
        }

        fun formatMoney(amount: Double?, currency: String?, masked: Boolean = false): String {
            val sym = currencySymbol(currency)
            if (masked) return "$sym ****"
            if (amount == null || !amount.isFinite()) return "—"
            val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            return if (amount < -0.004) "-$sym${df.format(abs(amount))}" else "$sym${df.format(abs(amount))}"
        }

        fun formatSignedMoney(amount: Double?, currency: String?, masked: Boolean = false): String {
            val sym = currencySymbol(currency)
            if (masked) return "$sym ****"
            if (amount == null || !amount.isFinite()) return "—"
            val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            return when {
                amount > 0.004 -> "+$sym${df.format(amount)}"
                amount < -0.004 -> "-$sym${df.format(abs(amount))}"
                else -> "${sym}0.00"
            }
        }

        fun formatCompactMoney(amount: Double?, currency: String?, masked: Boolean = false): String {
            val sym = currencySymbol(currency)
            if (masked) return "$sym****"
            if (amount == null || !amount.isFinite()) return "—"
            val absVal = abs(amount)
            val sign = if (amount < -0.004) "-" else ""
            val df2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            val df1 = DecimalFormat("#,##0.0", DecimalFormatSymbols(Locale.US))
            return when {
                absVal >= 100_000_000 -> "$sign$sym${df2.format(absVal / 100_000_000.0)}亿"
                absVal >= 1_000_000 -> "$sign$sym${df1.format(absVal / 10_000.0)}万"
                else -> "$sign$sym${df2.format(absVal)}"
            }
        }

        fun formatUnsignedCompactMetric(amount: Double?, currency: String?, masked: Boolean = false): String {
            val sym = currencySymbol(currency)
            if (masked) return "$sym****"
            if (amount == null || !amount.isFinite()) return "—"
            val absVal = abs(amount)
            val sign = if (amount < -0.004) "-" else ""
            val df2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            val df1 = DecimalFormat("#,##0.0", DecimalFormatSymbols(Locale.US))
            val df0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
            return when {
                absVal >= 100_000_000.0 -> "$sign$sym${df2.format(absVal / 100_000_000.0)}亿"
                absVal >= 10_000.0 -> "$sign$sym${df1.format(absVal / 10_000.0)}万"
                absVal >= 100.0 -> "$sign$sym${df0.format(absVal)}"
                else -> "$sign$sym${df2.format(absVal)}"
            }
        }

        fun formatSignedCompactMoney(amount: Double?, currency: String?, masked: Boolean = false): String {
            val sym = currencySymbol(currency)
            if (masked) return "$sym****"
            if (amount == null || !amount.isFinite()) return "—"
            val absVal = abs(amount)
            if (absVal <= 0.004) return "${sym}0.00"
            val sign = if (amount > 0.004) "+" else "-"
            val df2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            val df1 = DecimalFormat("#,##0.0", DecimalFormatSymbols(Locale.US))
            val df0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
            return when {
                absVal >= 100_000_000.0 -> "$sign$sym${df2.format(absVal / 100_000_000.0)}亿"
                absVal >= 10_000.0 -> "$sign$sym${df1.format(absVal / 10_000.0)}万"
                absVal >= 100.0 -> "$sign$sym${df0.format(absVal)}"
                else -> "$sign$sym${df2.format(absVal)}"
            }
        }

        fun formatSignedPercent(ratePercent: Double?): String {
            if (ratePercent == null || !ratePercent.isFinite()) return "—"
            return when {
                ratePercent > 0.004 -> String.format(Locale.US, "+%.2f%%", ratePercent)
                ratePercent < -0.004 -> String.format(Locale.US, "-%.2f%%", abs(ratePercent))
                else -> "0.00%"
            }
        }

        fun formatPrice(price: Double?, currency: String?): String {
            if (price == null || !price.isFinite()) return "—"
            val sym = currencySymbol(currency)
            val pattern = if (abs(price) < 10.0) "#,##0.000" else "#,##0.00"
            val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            return "$sym${df.format(price)}"
        }

        /**
         * 货币换算：优先使用长桥账户实盘汇率 (1 otherCurrency = averageRate * baseCurrency)，
         * 支持正反向查找与通过 USD / HKD 交叉换算，仅当汇率表缺失时回退至静态参考汇率。
         */
        fun convertAmount(
            amount: Double,
            fromCurrency: String?,
            toCurrency: String?,
            rates: List<LongbridgeRatePair>
        ): Double {
            if (!amount.isFinite()) return 0.0
            val from = normalizeCurrency(fromCurrency)
            val to = normalizeCurrency(toCurrency)
            if (from == to) return amount

            findDirectFactor(from, to, rates)?.let { return amount * it }

            for (pivot in listOf("USD", "HKD", "CNY")) {
                if (pivot == from || pivot == to) continue
                val f1 = findDirectFactor(from, pivot, rates)
                val f2 = findDirectFactor(pivot, to, rates)
                if (f1 != null && f2 != null) {
                    return amount * f1 * f2
                }
            }

            // 静态参考兜底（仅当远端未返回对应货币对时使用）
            val usdPerUnit = mapOf(
                "USD" to 1.0,
                "HKD" to (1.0 / 7.80),
                "CNY" to (1.0 / 7.20),
                "SGD" to (1.0 / 1.34)
            )
            val fromUsd = usdPerUnit[from]
            val toUsd = usdPerUnit[to]
            return if (fromUsd != null && toUsd != null) {
                amount * (fromUsd / toUsd)
            } else {
                amount
            }
        }

        private fun findDirectFactor(
            from: String,
            to: String,
            rates: List<LongbridgeRatePair>
        ): Double? {
            if (from == to) return 1.0
            for (pair in rates) {
                val base = normalizeCurrency(pair.baseCurrency)
                val other = normalizeCurrency(pair.otherCurrency)
                if (pair.averageRate <= 0.0 || !pair.averageRate.isFinite()) continue
                // Longbridge 汇率语义: 1 otherCurrency = averageRate * baseCurrency
                if (base == to && other == from) {
                    return pair.averageRate
                }
                if (base == from && other == to) {
                    return 1.0 / pair.averageRate
                }
            }
            return null
        }
    }
}
