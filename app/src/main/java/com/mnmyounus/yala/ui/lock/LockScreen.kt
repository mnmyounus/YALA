package com.mnmyounus.yala.ui.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.mnmyounus.yala.core.theme.LocalIsTv
import com.mnmyounus.yala.domain.model.LockType

@Composable
fun LockScreen(state: LockUiState, viewModel: LockViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("YALA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Locked: ${state.packageName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (state.hint.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Hint: ${state.hint}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))

            when {
                state.recoveryVisible -> RecoveryPanel(viewModel)
                state.lockType == LockType.PIN -> PinPad(viewModel)
                state.lockType == LockType.PASSWORD -> PasswordEntry(viewModel)
                state.lockType == LockType.PATTERN -> PatternLock { viewModel.submitPattern(it) }
                state.lockType == LockType.IMAGE_SEQUENCE -> ImageSequenceLock(state, viewModel)
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (state.failedAttempts > 0) {
                Text(
                    "Failed attempts: ${state.failedAttempts}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { viewModel.toggleRecovery() }) {
                Text(if (state.recoveryVisible) "Back to lock" else "Forgot? Use recovery key")
            }
        }
    }
}

@Composable
private fun RecoveryPanel(viewModel: LockViewModel) {
    var key by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter your 12-character recovery key")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.uppercase().take(12) },
            singleLine = true,
            label = { Text("Recovery key") }
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { viewModel.submitRecoveryKey(key) }) { Text("Unlock") }
    }
}

/** 4-digit numeric pad; every key is focusable so a TV D-pad can drive it. */
@Composable
private fun PinPad(viewModel: LockViewModel) {
    var pin by remember { mutableStateOf("") }
    val isTv = LocalIsTv.current
    val keySize = if (isTv) 88.dp else 72.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(keySize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                when (label) {
                                    "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    "OK" -> if (pin.length == 4) { viewModel.submitPin(pin); pin = "" }
                                    else -> if (pin.length < 4) {
                                        pin += label
                                        if (pin.length == 4) { viewModel.submitPin(pin); pin = "" }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** 8-character alphanumeric password. */
@Composable
private fun PasswordEntry(viewModel: LockViewModel) {
    var text by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(8) },
            label = { Text("8-character password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.submitPassword(text); text = "" },
            enabled = text.length == 8
        ) { Text("Unlock") }
    }
}

/**
 * 3x3 pattern grid. Dots are indexed 0..8; the drawn path is submitted on release.
 */
@Composable
fun PatternLock(onPatternComplete: (List<Int>) -> Unit) {
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var gridSize by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(280.dp)
            .pointerInput(Unit) {
                gridSize = size.width.toFloat()
                detectDragGestures(
                    onDragEnd = {
                        if (selected.size >= 4) onPatternComplete(selected)
                        selected = emptyList()
                    }
                ) { change, _ ->
                    val cell = gridSize / 3f
                    val col = (change.position.x / cell).toInt().coerceIn(0, 2)
                    val row = (change.position.y / cell).toInt().coerceIn(0, 2)
                    val index = row * 3 + col
                    val center = Offset(col * cell + cell / 2, row * cell + cell / 2)
                    val within = (change.position - center).getDistance() < cell * 0.35f
                    if (within && index !in selected) selected = selected + index
                }
            }
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        val active = index in selected
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                                // D-pad / TV fallback: dots are individually selectable.
                                .clickable {
                                    if (index !in selected) selected = selected + index
                                    if (selected.size >= 9) {
                                        onPatternComplete(selected); selected = emptyList()
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = {
        if (selected.size >= 4) onPatternComplete(selected)
        selected = emptyList()
    }) { Text("Submit pattern") }
}

/**
 * Image-based sequence lock: the user picks 3-4 images from their own pool of 5
 * in the exact secret order.
 */
@Composable
fun ImageSequenceLock(state: LockUiState, viewModel: LockViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Tap your images in the secret order")
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.selectedImages.size} selected",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.imagePool.take(3).forEach { id -> ImageTile(id, state, viewModel) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.imagePool.drop(3).take(2).forEach { id -> ImageTile(id, state, viewModel) }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.submitImageSequence() },
            enabled = state.selectedImages.size in 3..4
        ) { Text("Unlock") }
    }
}

@Composable
private fun ImageTile(id: String, state: LockUiState, viewModel: LockViewModel) {
    val order = state.selectedImages.indexOf(id)
    Box(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (order >= 0) 3.dp else 1.dp,
                color = if (order >= 0) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { viewModel.onImageTapped(id) }
    ) {
        AsyncImage(
            model = id,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (order >= 0) {
            Text(
                "${order + 1}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
