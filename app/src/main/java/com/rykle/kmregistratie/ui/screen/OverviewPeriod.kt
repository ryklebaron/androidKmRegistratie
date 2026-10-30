package com.rykle.kmregistratie.ui.screen

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class OverviewGranularity(val label: String) {
    DAY("Dag"), WEEK("Week"), MONTH("Maand"), YEAR("Jaar"), ALL("Alles")
}

/** Een staaf in de grafiek: [short] onder de staaf, [long] bij de geselecteerde staaf. */
data class Bucket(val short: String, val long: String, val from: Long, val to: Long)

/**
 * Periode in het overzicht: een [granularity] rond een [anchor]-datum.
 * Tijden zijn epoch-millis, [from] inclusief en [to] exclusief.
 */
data class OverviewPeriod(
    val granularity: OverviewGranularity,
    val anchor: LocalDate,
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val nl = Locale("nl", "NL")

    val start: LocalDate = when (granularity) {
        OverviewGranularity.DAY -> anchor
        OverviewGranularity.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        OverviewGranularity.MONTH -> anchor.withDayOfMonth(1)
        OverviewGranularity.YEAR -> anchor.withDayOfYear(1)
        OverviewGranularity.ALL -> LocalDate.of(1970, 1, 1)
    }

    /** Eerste dag ná de periode. */
    val end: LocalDate = when (granularity) {
        OverviewGranularity.DAY -> start.plusDays(1)
        OverviewGranularity.WEEK -> start.plusWeeks(1)
        OverviewGranularity.MONTH -> start.plusMonths(1)
        OverviewGranularity.YEAR -> start.plusYears(1)
        OverviewGranularity.ALL -> LocalDate.of(9999, 1, 1)
    }

    val from: Long get() = millis(start)
    val to: Long get() = millis(end)

    fun contains(time: Long) = time in from until to

    fun previous() = shift(-1)
    fun next() = shift(1)
    // Niet verder dan vandaag bladeren.
    val hasNext: Boolean get() = granularity != OverviewGranularity.ALL && !end.isAfter(LocalDate.now(zone))
    val hasPrevious: Boolean get() = granularity != OverviewGranularity.ALL

    private fun shift(n: Long) = copy(
        anchor = when (granularity) {
            OverviewGranularity.DAY -> anchor.plusDays(n)
            OverviewGranularity.WEEK -> anchor.plusWeeks(n)
            OverviewGranularity.MONTH -> anchor.plusMonths(n)
            OverviewGranularity.YEAR -> anchor.plusYears(n)
            OverviewGranularity.ALL -> anchor
        }
    )

    val title: String
        get() = when (granularity) {
            OverviewGranularity.DAY -> anchor.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", nl))
            OverviewGranularity.WEEK -> {
                val last = end.minusDays(1)
                val fmt = DateTimeFormatter.ofPattern("d MMM", nl)
                "Week ${start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · ${start.format(fmt)} – ${last.format(fmt)}"
            }
            OverviewGranularity.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", nl))
            OverviewGranularity.YEAR -> start.year.toString()
            OverviewGranularity.ALL -> "Alle ritten"
        }

    /** De staven voor de grafiek; null bij Dag (dan tonen we de ritten zelf). */
    fun buckets(firstYear: Int, lastYear: Int): List<Bucket>? = when (granularity) {
        OverviewGranularity.DAY -> null
        OverviewGranularity.WEEK -> (0L until 7L).map { i ->
            val d = start.plusDays(i)
            Bucket(
                short = d.format(DateTimeFormatter.ofPattern("EEEEE", nl)).uppercase(nl),
                long = d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", nl)),
                from = millis(d), to = millis(d.plusDays(1)),
            )
        }
        OverviewGranularity.MONTH -> (0 until start.lengthOfMonth()).map { i ->
            val d = start.plusDays(i.toLong())
            Bucket(
                // Niet elke dag een label, anders loopt het in elkaar.
                short = if (d.dayOfMonth == 1 || d.dayOfMonth % 5 == 0) d.dayOfMonth.toString() else "",
                long = d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", nl)),
                from = millis(d), to = millis(d.plusDays(1)),
            )
        }
        OverviewGranularity.YEAR -> (0L until 12L).map { i ->
            val m = start.plusMonths(i)
            Bucket(
                short = m.format(DateTimeFormatter.ofPattern("MMMMM", nl)).uppercase(nl),
                long = m.format(DateTimeFormatter.ofPattern("MMMM", nl)).replaceFirstChar { it.uppercase(nl) },
                from = millis(m), to = millis(m.plusMonths(1)),
            )
        }
        OverviewGranularity.ALL -> (firstYear..lastYear).map { y ->
            val s = LocalDate.of(y, 1, 1)
            Bucket(
                short = if (lastYear - firstYear < 6) y.toString() else "'" + (y % 100).toString().padStart(2, '0'),
                long = y.toString(),
                from = millis(s), to = millis(s.plusYears(1)),
            )
        }
    }

    private fun millis(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
}
