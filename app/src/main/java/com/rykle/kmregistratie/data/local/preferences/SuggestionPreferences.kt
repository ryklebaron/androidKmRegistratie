package com.rykle.kmregistratie.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.rykle.kmregistratie.domain.repository.SuggestionRepository
import com.rykle.kmregistratie.domain.repository.SuggestionRepository.Rejection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Bewaart afgewezen voorstellen als "ritId:projectId"; buiten de ritten-database. */
@Singleton
class SuggestionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : SuggestionRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("project_suggestions", Context.MODE_PRIVATE)
    }

    private val _rejected = MutableStateFlow(load())
    override val rejected: StateFlow<Set<Rejection>> = _rejected.asStateFlow()

    override fun reject(rejections: Collection<Rejection>) {
        if (rejections.isEmpty()) return
        val updated = _rejected.value + rejections
        prefs.edit().putStringSet(KEY_REJECTED, updated.map { "${it.driveId}:${it.projectId}" }.toSet()).apply()
        _rejected.value = updated
    }

    private fun load(): Set<Rejection> =
        prefs.getStringSet(KEY_REJECTED, emptySet()).orEmpty().mapNotNull { s ->
            val (drive, project) = s.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            Rejection(drive.toLongOrNull() ?: return@mapNotNull null, project.toLongOrNull() ?: return@mapNotNull null)
        }.toSet()

    companion object {
        private const val KEY_REJECTED = "rejected"
    }
}
