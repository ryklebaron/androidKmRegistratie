package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DriveCsvTest {

    private val zone = ZoneId.of("Europe/Amsterdam")

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi, s).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun roundTripKeepsEverything() {
        val drive = Drive(
            startTime = millis(2026, 10, 27, 18, 52, 13),
            endTime = millis(2026, 10, 28, 7, 23, 45),   // over middernacht
            startLat = 53.3401, startLng = 6.6512, startAddress = "Asingalaan 6T, Middelstum",
            endLat = 53.2, endLng = 6.5, endAddress = "Korenlaan 18, Middelstum",
            driveType = DriveType.BUSINESS,
            startKm = 158243, endKm = 158253,
            hasKmMismatch = true, kmPending = false,
            projectId = 7, note = "Storing \"Jansen\", kast 2\nmet regeleinde",
        )
        val csv = DriveCsv.write(listOf(drive), mapOf(7L to "Zonnepanelen, De Vries"), zone)
        val rows = DriveCsv.read(csv, zone)

        assertEquals(1, rows.size)
        val back = rows.single().drive
        assertEquals("Zonnepanelen, De Vries", rows.single().projectName)
        assertEquals(drive.startTime, back.startTime)
        assertEquals(drive.endTime, back.endTime)
        assertEquals(drive.startAddress, back.startAddress)
        assertEquals(drive.endAddress, back.endAddress)
        assertEquals(drive.startLat, back.startLat, 0.0)
        assertEquals(drive.endLng!!, back.endLng!!, 0.0)
        assertEquals(DriveType.BUSINESS, back.driveType)
        assertEquals(158243, back.startKm)
        assertEquals(158253, back.endKm)
        assertTrue(back.hasKmMismatch)
        assertEquals("Storing \"Jansen\", kast 2 met regeleinde", back.note)
    }

    @Test
    fun readsOldMainFormatWithLatLng() {
        val csv = """
            Datum,Starttijd,Eindtijd,Type,Start Lat,Start Lng,Eind Lat,Eind Lng,Start KM,Eind KM,Km Gereden,Opmerking
            28-10-2026,07:23:00,09:55:00,Zakelijk,53.1,6.2,53.3,6.4,158253,158295,42,
            27-10-2026,18:52:00,07:23:00,Privé,0.0,0.0,0.0,0.0,158243,,,EINDSTAND ONBEKEND
        """.trimIndent()
        val rows = DriveCsv.read(csv, zone)

        assertEquals(2, rows.size)
        assertEquals(53.1, rows[0].drive.startLat, 0.0)
        assertEquals(42, rows[0].drive.kmDriven)
        val overnight = rows[1].drive
        assertEquals(DriveType.PRIVATE, overnight.driveType)
        assertEquals(millis(2026, 10, 28, 7, 23), overnight.endTime)   // dag erbij
        assertTrue(overnight.kmPending)
        assertNull(overnight.endKm)
    }

    @Test
    fun readsOldNextcloudBranchFormat() {
        val csv = "Datum,Starttijd,Eindtijd,Type,Startlocatie,Eindlocatie,Start KM,Eind KM,Zakelijk km,Privé km,Opmerking\r\n" +
            "11-05-2026,08:00:00,08:30:00,Zakelijk,\"Dorpsstraat 1, Loppersum\",53.33450 6.74520,1000,1020,20,,KM MISMATCH\r\n"
        val d = DriveCsv.read(csv, zone).single().drive

        assertEquals("Dorpsstraat 1, Loppersum", d.startAddress)
        assertNull(d.endAddress)
        assertEquals(53.3345, d.endLat!!, 1e-9)
        assertEquals(6.7452, d.endLng!!, 1e-9)
        assertTrue(d.hasKmMismatch)
        assertEquals(20, d.kmDriven)
    }

    @Test
    fun skipsRowsWithoutValidDate() {
        val csv = "Datum,Starttijd,Type\nkapot,08:00:00,Zakelijk\n\n01-01-2026,08:00,Privé\n"
        val rows = DriveCsv.read(csv, zone)
        assertEquals(1, rows.size)
        assertEquals(millis(2026, 1, 1, 8, 0), rows.single().drive.startTime)
    }
}
