package com.rykle.kmregistratie.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OverviewPeriodTest {

    private val zone = ZoneId.of("Europe/Amsterdam")

    @Test
    fun weekRunsMondayToSunday() {
        val p = OverviewPeriod(OverviewGranularity.WEEK, LocalDate.of(2026, 10, 30), zone)   // vrijdag
        assertEquals(LocalDate.of(2026, 10, 26), p.start)
        assertEquals(LocalDate.of(2026, 11, 2), p.end)
        // Maandafkorting verschilt per JDK ("okt" of "okt."), dus alleen het begin vergelijken.
        assertTrue(p.title, p.title.startsWith("Week 44 · 26 okt"))
        assertEquals(7, p.buckets(2026, 2026)!!.size)
    }

    @Test
    fun monthHasOneBucketPerDay() {
        val p = OverviewPeriod(OverviewGranularity.MONTH, LocalDate.of(2026, 10, 30), zone)
        val buckets = p.buckets(2026, 2026)!!
        assertEquals(31, buckets.size)
        assertEquals(p.from, buckets.first().from)
        assertEquals(p.to, buckets.last().to)
        assertEquals("", buckets[1].short)   // 2 oktober: geen label
    }

    @Test
    fun yearAndAllAndDay() {
        assertEquals(12, OverviewPeriod(OverviewGranularity.YEAR, LocalDate.of(2026, 5, 1), zone).buckets(2026, 2026)!!.size)
        assertEquals(3, OverviewPeriod(OverviewGranularity.ALL, LocalDate.of(2026, 5, 1), zone).buckets(2024, 2026)!!.size)
        assertNull(OverviewPeriod(OverviewGranularity.DAY, LocalDate.of(2026, 5, 1), zone).buckets(2026, 2026))
    }

    @Test
    fun cannotBrowsePastToday() {
        assertFalse(OverviewPeriod(OverviewGranularity.MONTH, LocalDate.now(zone), zone).hasNext)
        assertTrue(OverviewPeriod(OverviewGranularity.MONTH, LocalDate.now(zone).minusMonths(2), zone).hasNext)
        assertFalse(OverviewPeriod(OverviewGranularity.ALL, LocalDate.now(zone), zone).hasPrevious)
    }
}
