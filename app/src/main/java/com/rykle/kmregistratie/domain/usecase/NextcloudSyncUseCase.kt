package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.CloudBackupRepository
import com.rykle.kmregistratie.domain.repository.DriveRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Rittenlijst naar Nextcloud zetten, of ritten van Nextcloud samenvoegen met de lokale lijst. */
class NextcloudSyncUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val driveRepository: DriveRepository,
    private val exportDrives: ExportDrivesUseCase,
    private val importDrives: ImportDrivesUseCase,
) {
    val isConfigured: Boolean get() = cloud.isConfigured

    /**
     * Overschrijft km_ritten.csv. Staan daar ritten in die lokaal niet (meer) bestaan, dan blijft
     * de oude versie eerst bewaard als km_ritten_<datum-tijd>.csv, zodat er nooit iets verloren gaat.
     */
    suspend fun upload(): Result<Unit> = runCatching {
        val remote = cloud.download()
        if (remote != null) {
            val local = driveRepository.getAllDrives().first().map { it.startTime / 1000 }.toSet()
            val onlyRemote = DriveCsv.read(remote).count { it.drive.startTime / 1000 !in local }
            if (onlyRemote > 0) {
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
                cloud.upload(remote, "km_ritten_$stamp.csv")
            }
        }
        cloud.upload(exportDrives())
    }

    /** Voegt ritten van Nextcloud toe; bestaande ritten blijven zoals ze zijn. */
    suspend fun restore(): Result<ImportDrivesUseCase.Result> = runCatching {
        val remote = cloud.download() ?: error("Geen ${CloudBackupRepository.MAIN_FILE} gevonden op Nextcloud")
        importDrives(remote)
    }
}
