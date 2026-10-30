package com.rykle.kmregistratie.domain.repository

import com.rykle.kmregistratie.domain.model.OdometerResult
import com.rykle.kmregistratie.domain.model.VolvoStatus
import kotlinx.coroutines.flow.StateFlow

interface VolvoRepository {
    /** Wordt bij elke [fetchOdometer] bijgewerkt. */
    val status: StateFlow<VolvoStatus>

    suspend fun fetchOdometer(): OdometerResult

    suspend fun getCurrentOdometer(): Int? = (fetchOdometer() as? OdometerResult.Success)?.km
}
