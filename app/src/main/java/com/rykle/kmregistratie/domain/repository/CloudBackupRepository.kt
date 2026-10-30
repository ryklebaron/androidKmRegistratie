package com.rykle.kmregistratie.domain.repository

/** Kopie van de rittenlijst (CSV) in de cloud; nu Nextcloud via WebDAV. */
interface CloudBackupRepository {
    val isConfigured: Boolean
    suspend fun upload(csv: String, fileName: String = MAIN_FILE)
    /** Inhoud van [MAIN_FILE], of null als die nog niet bestaat. */
    suspend fun download(): String?

    companion object {
        const val MAIN_FILE = "km_ritten.csv"
    }
}
