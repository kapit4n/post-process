package com.inventory.industry.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatTest {

    @Test
    fun `formatMoney uses Bolivian separators`() {
        assertEquals("650,00", formatMoney(650.0))
        assertEquals("1.250,50", formatMoney(1250.5))
        assertEquals("12.500,00", formatMoney(12500.0))
        assertEquals("125.450,75", formatMoney(125450.75))
    }

    @Test
    fun `formatMoneyBs prefixes Bs and keeps Bolivian separators`() {
        assertEquals("Bs 650,00", formatMoneyBs(650.0))
        assertEquals("Bs 1.250,50", formatMoneyBs(1250.5))
        assertEquals("Bs 15.850,00", formatMoneyBs(15850.0))
    }

    @Test
    fun `parseMoneyAmount accepts Bolivian and international formats`() {
        assertEquals(1234.56, parseMoneyAmount("1.234,56"))
        assertEquals(1234.56, parseMoneyAmount("1234.56"))
        assertEquals(1234.56, parseMoneyAmount("1,234.56"))
        assertEquals(1234.56, parseMoneyAmount("1234,56"))
    }
}
