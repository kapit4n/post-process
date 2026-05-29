package com.inventory.industry.ui.utils

import java.time.LocalDate
import java.time.ZoneId

data class MonthEpochRange(
    val startThisMonth: Long,
    val startNextMonth: Long,
    val startPrevMonth: Long,
) {
    val thisMonthRange: LongRange get() = startThisMonth until startNextMonth
    val prevMonthRange: LongRange get() = startPrevMonth until startThisMonth
}

fun currentMonthEpochRange(zone: ZoneId = ZoneId.systemDefault()): MonthEpochRange {
    val firstOfMonth = LocalDate.now().withDayOfMonth(1)
    val startThisMonth = firstOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
    val startNextMonth = firstOfMonth.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val startPrevMonth = firstOfMonth.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return MonthEpochRange(startThisMonth, startNextMonth, startPrevMonth)
}
