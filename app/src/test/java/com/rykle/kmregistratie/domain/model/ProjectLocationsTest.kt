package com.rykle.kmregistratie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectLocationsTest {

    private val project = Project(id = 1, name = "Jansen", lat = 53.2000, lng = 6.5000, address = "Dorpsstraat 1, Loppersum, Groningen")

    private fun drive(
        id: Long,
        startLat: Double = 53.35, startLng: Double = 6.65, startAddress: String? = "Korenlaan 18, Middelstum",
        endLat: Double? = 53.35, endLng: Double? = 6.65, endAddress: String? = "Korenlaan 18, Middelstum",
        projectId: Long? = null,
    ) = Drive(
        id = id, startTime = id * 1000, endTime = id * 1000 + 500,
        startLat = startLat, startLng = startLng, startAddress = startAddress,
        endLat = endLat, endLng = endLng, endAddress = endAddress,
        driveType = DriveType.BUSINESS, startKm = 100, endKm = 120, projectId = projectId,
    )

    @Test
    fun findsArrivalsAndDeparturesNearby() {
        val heen = drive(1, endLat = 53.2005, endLng = 6.5005)                     // ± 65 m
        val terug = drive(2, startLat = 53.2003, startLng = 6.4998)
        val ver = drive(3, endLat = 53.2100, endLng = 6.5000)                      // ± 1,1 km
        val opAdres = drive(4, endLat = 0.0, endLng = 0.0, endAddress = "dorpsstraat 1,  Loppersum")

        val matches = ProjectLocations.nearbyDrives(project, listOf(heen, terug, ver, opAdres))

        assertEquals(listOf(4L, 2L, 1L), matches.map { it.drive.id })
        assertTrue(matches.first { it.drive.id == 1L }.arrives)
        assertTrue(matches.first { it.drive.id == 2L }.departs)
    }

    @Test
    fun suggestsOnlyForUnlinkedDrives() {
        val heen = drive(1, endLat = 53.2005, endLng = 6.5005)
        assertEquals(project, ProjectLocations.suggestFor(heen, listOf(project)))
        assertNull(ProjectLocations.suggestFor(heen.copy(projectId = 9), listOf(project)))
        assertNull(ProjectLocations.suggestFor(heen, listOf(project.copy(archived = true))))
        assertNull(ProjectLocations.suggestFor(drive(5), listOf(project)))
    }

    @Test
    fun rejectedProjectIsNotSuggestedAgainButAnotherOneIs() {
        val buurman = project.copy(id = 2, name = "Buurman", lat = 53.2004, lng = 6.5002)
        val heen = drive(1, endLat = 53.2005, endLng = 6.5005)
        assertEquals(project, ProjectLocations.suggestFor(heen, listOf(project, buurman)))
        assertEquals(buurman, ProjectLocations.suggestFor(heen, listOf(project, buurman), rejected = setOf(1L)))
        assertNull(ProjectLocations.suggestFor(heen, listOf(project, buurman), rejected = setOf(1L, 2L)))
    }

    @Test
    fun homeEndpointsNeverMatchEvenWithAClientNearby() {
        val home = SavedLocation("Korenlaan 18, Middelstum", 53.345093, 6.637889)
        // Klant op ± 120 m van huis: zonder thuisregel zou elke rit van/naar huis matchen.
        val buren = Project(id = 3, name = "Buren", lat = 53.346170, lng = 6.637889)
        val heen = drive(1, startLat = 53.34512, startLng = 6.63795, startAddress = "Korenlaan 20, Middelstum",   // GPS-ruis thuis
            endLat = 53.34617, endLng = 6.63789, endAddress = "Asingalaan 4, Middelstum")
        val naarHuis = drive(2, startLat = 53.2, startLng = 6.5, startAddress = "Elders 1, Bedum",
            endLat = 53.34510, endLng = 6.63790, endAddress = "Korenlaan 18, Middelstum")

        val matches = ProjectLocations.nearbyDrives(buren, listOf(heen, naarHuis), home)
        assertEquals(listOf(1L), matches.map { it.drive.id })
        assertTrue(matches.single().arrives)
        assertTrue(!matches.single().departs)
        assertNull(ProjectLocations.suggestFor(naarHuis, listOf(buren), home = home))
        assertEquals(buren, ProjectLocations.suggestFor(heen, listOf(buren), home = home))
    }

    @Test
    fun guessesHomeFromMostFrequentAddress() {
        val drives = (1L..5L).map { drive(it, startLat = 53.3451, startLng = 6.6379, startAddress = "Korenlaan 18, Middelstum, Groningen",
            endLat = 53.2, endLng = 6.5, endAddress = "Klant $it, Bedum") }
        val (home, count) = ProjectLocations.guessHome(drives)!!
        assertEquals("Korenlaan 18, Middelstum, Groningen", home.address)
        assertEquals(5, count)
        assertEquals(53.3451, home.latitude, 1e-9)
    }

    @Test
    fun linkingNeverChangesKm() {
        // Koppelen is alleen een label: kmDriven komt uit de eigen standen van de rit.
        val d = drive(1, endLat = 53.2005, endLng = 6.5005)
        assertEquals(20, d.copy(projectId = project.id).kmDriven)
    }
}
