package com.rykle.kmregistratie.domain.repository

interface VolvoRepository {
    suspend fun getCurrentOdometer(): Int?
}
