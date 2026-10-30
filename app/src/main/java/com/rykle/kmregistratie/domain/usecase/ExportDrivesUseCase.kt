package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportDrivesUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val projectRepository: ProjectRepository,
) {
    /** Afgeronde ritten als CSV (oudste eerst), optioneel binnen [from]..[to]. */
    suspend operator fun invoke(from: Long? = null, to: Long? = null): String {
        val drives = driveRepository.getAllDrives().first()
            .filter { !it.isActive }
            .let { list ->
                if (from != null && to != null) list.filter { it.startTime in from..to } else list
            }
            .sortedBy { it.startTime }
        val projectNames = projectRepository.getProjects().first().associate { it.id to it.name }
        return DriveCsv.write(drives, projectNames)
    }
}
