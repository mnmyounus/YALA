package com.mnmyounus.yala.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YALA") },
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Intruder gallery")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.query.isEmpty() && state.showSystemApps,
                    onClick = { viewModel.toggleSystemApps() },
                    label = { Text(if (state.showSystemApps) "System apps: on" else "System apps: off") }
                )
                Text(
                    "${state.lockedCount} locked",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(state.apps, key = { it.info.packageName }) { row ->
                    ListItem(
                        headlineContent = { Text(row.info.label) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(row.info.packageName)
                                    if (row.info.isSystemApp) append("  •  system")
                                    if (row.locked) append("  •  ${row.lockType.name}")
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = row.locked,
                                onCheckedChange = { viewModel.toggle(row.info.packageName, it) }
                            )
                        }
                    )
                }
            }
        }
    }
}
