package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Voegt ritten uit een CSV toe aan de bestaande lijst. Er wordt nooit iets gewist of
 * overschreven: een rit met dezelfde starttijd wordt overgeslagen.
 */
class ImportDrivesUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val projectRepository: ProjectRepository,
) {
    data class Result(val imported: Int, val skipped: Int)

    suspend operator fun invoke(csv: String): Result {
        val rows = DriveCsv.read(csv)
        require(rows.isNotEmpty()) { "Geen ritten gevonden in het bestand" }
        var imported = 0
        var skipped = 0
        for (row in rows) {
            if (driveRepository.findByStartTime(row.drive.startTime) != null) {
                skipped++
                continue
            }
            val projectId = row.projectName?.let { projectRepository.findOrCreate(it).id }
            driveRepository.insertDrive(row.drive.copy(projectId = projectId))
            imported++
        }
        return Result(imported, skipped)
    }
}
