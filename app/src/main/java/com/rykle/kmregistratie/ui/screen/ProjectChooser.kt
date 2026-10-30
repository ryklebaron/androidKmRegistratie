package com.rykle.kmregistratie.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.Project

/** Actieve projecten, het laatst gebruikte eerst. */
fun recentProjectsFirst(projects: List<Project>, drives: List<Drive>): List<Project> {
    val lastUsed = drives.asSequence()
        .filter { it.projectId != null }
        .groupBy { it.projectId!! }
        .mapValues { (_, list) -> list.maxOf { it.startTime } }
    return projects.filter { !it.archived }.sortedByDescending { lastUsed[it.id] ?: 0L }
}

/**
 * Chips om een project te kiezen (of "Geen"), plus een veld om er ter plekke een aan te maken.
 * [onCreate] maakt het project aan en geeft het via de callback terug, zodat het meteen gekozen is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectChooser(
    projects: List<Project>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: (name: String, onCreated: (Project) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun submit() {
        val name = newName.trim()
        if (name.isEmpty()) return
        onCreate(name) { onSelect(it.id) }
        newName = ""
        adding = false
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedId == null, onClick = { onSelect(null) }, label = { Text("Geen") })
            projects.forEach { p ->
                FilterChip(selected = selectedId == p.id, onClick = { onSelect(p.id) }, label = { Text(p.name) })
            }
            FilterChip(
                selected = adding,
                onClick = { adding = !adding },
                label = { Text("Nieuw") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }
        AnimatedVisibility(adding) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Naam project") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = ::submit, enabled = newName.isNotBlank()) {
                    Icon(Icons.Default.Check, contentDescription = "Project toevoegen")
                }
            }
        }
    }
}
