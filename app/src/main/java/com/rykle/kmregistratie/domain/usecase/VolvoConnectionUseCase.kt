package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.VolvoStatus
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class VolvoConnectionUseCase @Inject constructor(
    private val volvoRepository: VolvoRepository,
) {
    val status: StateFlow<VolvoStatus> get() = volvoRepository.status

    /** Test de verbinding; de uitkomst verschijnt in [status]. */
    suspend fun check() {
        volvoRepository.fetchOdometer()
    }
}
