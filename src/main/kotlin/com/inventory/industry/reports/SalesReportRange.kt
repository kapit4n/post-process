package com.inventory.industry.reports

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class SalesReportRange(val label: String) {
    ALL("Todo"),
    TODAY("Hoy"),
    WEEK("Semana"),
    MONTH("Mes"),
    YEAR("Año"),
    CUSTOM("Personalizado"),
}

data class SalesEpochRange(
    val fromEpochMs: Long?,
    val toEpochMsExclusive: Long?,
    val description: String,
)

object SalesReportRangeResolver {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val displayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun resolve(
        range: SalesReportRange,
        customFrom: LocalDate? = null,
        customTo: LocalDate? = null,
        today: LocalDate = LocalDate.now(zone),
    ): SalesEpochRange {
        when (range) {
            SalesReportRange.ALL ->
                return SalesEpochRange(null, null, "Todo el historial")
            SalesReportRange.TODAY -> {
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                return SalesEpochRange(start, end, "Hoy (${today.format(displayFmt)})")
            }
            SalesReportRange.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY)
                val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
                return SalesEpochRange(
                    start,
                    end,
                    "Semana ${monday.format(displayFmt)} — ${monday.plusDays(6).format(displayFmt)}",
                )
            }
            SalesReportRange.MONTH -> {
                val first = today.withDayOfMonth(1)
                val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
                return SalesEpochRange(
                    start,
                    end,
                    "Mes ${first.format(DateTimeFormatter.ofPattern("yyyy-MM"))}",
                )
            }
            SalesReportRange.YEAR -> {
                val first = today.withDayOfYear(1)
                val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = first.plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()
                return SalesEpochRange(start, end, "Año ${today.year}")
            }
            SalesReportRange.CUSTOM -> {
                val from = customFrom ?: today
                val to = customTo ?: today
                val start = minOf(from, to).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = maxOf(from, to).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                return SalesEpochRange(
                    start,
                    end,
                    "${minOf(from, to).format(displayFmt)} — ${maxOf(from, to).format(displayFmt)}",
                )
            }
        }
    }
}
