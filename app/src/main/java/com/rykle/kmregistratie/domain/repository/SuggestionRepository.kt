package com.rykle.kmregistratie.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Projectvoorstellen waarop de gebruiker "nee" zei, per rit + project. */
interface SuggestionRepository {
    val rejected: StateFlow<Set<Rejection>>
    fun reject(rejections: Collection<Rejection>)

    data class Rejection(val driveId: Long, val projectId: Long)
}
